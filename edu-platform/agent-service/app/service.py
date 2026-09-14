import json
import time
import uuid
from datetime import datetime

import httpx

from .config import settings
from .database import connect, transaction
from .llm import decide, summarize
from .schemas import PlanRequest


class AgentError(Exception):
    def __init__(self, message: str, status: int = 400):
        self.message, self.status = message, status


def _one(cursor, sql: str, args=()):
    cursor.execute(sql, args)
    return cursor.fetchone()


def recommend(user_id: int, interest: str, goal: str, level: str, limit: int) -> list[dict]:
    try:
        response = httpx.post(
            f"{settings.recommendation_url}/api/recommendation/courses",
            headers={"X-User-Id": str(user_id)},
            json={"interest": interest, "goal": goal, "level": level, "limit": limit, "sessionId": f"agent-{user_id}"},
            timeout=8,
        )
        response.raise_for_status()
        return response.json().get("data", {}).get("courses", [])
    except (httpx.HTTPError, AttributeError, TypeError):
        return []


def _audit(cursor, user_id: int, plan_id: str, action: str, status: str, confirmation: bool, input_data, output_data):
    cursor.execute(
        "INSERT INTO t_agent_action_log VALUES(%s,%s,%s,%s,%s,%s,%s,%s,NOW())",
        (str(uuid.uuid4()), user_id, plan_id, action, status, confirmation, json.dumps(input_data, ensure_ascii=False), json.dumps(output_data, ensure_ascii=False)),
    )


def _load(cursor, user_id: int, plan_id: str) -> dict:
    plan = _one(cursor, "SELECT id,title,goal,interest,current_level currentLevel,weeks,hours_per_week hoursPerWeek,status,agent_summary agentSummary,created_at createdAt,updated_at updatedAt FROM t_learning_plan WHERE id=%s AND user_id=%s", (plan_id, user_id))
    if not plan:
        raise AgentError("计划不存在或无权操作", 404)
    cursor.execute("SELECT id,week_no weekNo,CAST(course_id AS CHAR) courseId,course_title courseTitle,title,description,estimated_minutes estimatedMinutes,status,completed_at completedAt FROM t_learning_task WHERE plan_id=%s AND user_id=%s ORDER BY week_no,created_at", (plan_id, user_id))
    tasks = cursor.fetchall()
    completed = sum(task["status"] == "COMPLETED" for task in tasks)
    return {**plan, "tasks": tasks, "completedTasks": completed, "totalTasks": len(tasks), "progress": round(completed * 100 / len(tasks)) if tasks else 0}


def create_plan(user_id: int, request: PlanRequest) -> dict:
    plan_id, now = str(uuid.uuid4()), datetime.now()
    courses = recommend(user_id, request.interest, request.goal, request.current_level, min(6, request.weeks * 2))
    title = request.goal[:100] + " · AI 学习计划"
    summary = summarize(request.goal, request.interest, request.current_level, request.weeks, request.hours_per_week, courses)
    minutes = max(30, request.hours_per_week * 60 // 3)
    with transaction() as connection, connection.cursor() as cursor:
        cursor.execute("INSERT INTO t_learning_plan VALUES(%s,%s,%s,%s,%s,%s,%s,%s,'DRAFT',%s,%s,%s)", (plan_id, user_id, title, request.goal, request.interest, request.current_level, request.weeks, request.hours_per_week, summary, now, now))
        templates = [("学习核心内容", "完成推荐课程对应章节，记录关键概念和两个仍不清楚的问题。", minutes), ("项目实践与验证", "把知识映射到 AI 智慧教育平台，完成一次可运行修改或故障排查。", minutes), ("复盘与面试输出", "口述调用链、技术取舍与异常场景，整理为项目面试问答。", max(30, minutes // 2))]
        for week in range(1, request.weeks + 1):
            course = courses[(week - 1) % len(courses)] if courses else {}
            for task_title, description, estimated in templates:
                cursor.execute("INSERT INTO t_learning_task(id,plan_id,user_id,week_no,course_id,course_title,title,description,estimated_minutes,status,created_at,updated_at) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,'TODO',%s,%s)", (str(uuid.uuid4()), plan_id, user_id, week, course.get("id"), course.get("title"), task_title, description, estimated, now, now))
        _audit(cursor, user_id, plan_id, "CREATE_PLAN_DRAFT", "WAITING_CONFIRMATION", True, request.model_dump(by_alias=True), {"courses": len(courses), "tasks": request.weeks * 3})
        return _load(cursor, user_id, plan_id)


def current_plan(user_id: int) -> dict:
    connection = connect()
    try:
        with connection.cursor() as cursor:
            row = _one(cursor, "SELECT id FROM t_learning_plan WHERE user_id=%s ORDER BY FIELD(status,'ACTIVE','DRAFT','ARCHIVED'),updated_at DESC LIMIT 1", (user_id,))
            return {"empty": True} if not row else _load(cursor, user_id, row["id"])
    finally:
        connection.close()


def confirm_plan(user_id: int, plan_id: str) -> dict:
    with transaction() as connection, connection.cursor() as cursor:
        cursor.execute("UPDATE t_learning_plan SET status='ACTIVE',updated_at=NOW() WHERE id=%s AND user_id=%s AND status='DRAFT'", (plan_id, user_id))
        if cursor.rowcount == 0:
            raise AgentError("计划不存在、已经确认或无权操作")
        cursor.execute("UPDATE t_learning_plan SET status='ARCHIVED',updated_at=NOW() WHERE user_id=%s AND id<>%s AND status='ACTIVE'", (user_id, plan_id))
        _audit(cursor, user_id, plan_id, "CONFIRM_PLAN", "SUCCEEDED", False, {}, {})
        return _load(cursor, user_id, plan_id)


def change_task(user_id: int, task_id: str, done: bool) -> dict:
    with transaction() as connection, connection.cursor() as cursor:
        cursor.execute("UPDATE t_learning_task t JOIN t_learning_plan p ON p.id=t.plan_id SET t.status=%s,t.completed_at=%s,t.updated_at=NOW() WHERE t.id=%s AND t.user_id=%s AND p.status='ACTIVE'", ("COMPLETED" if done else "TODO", datetime.now() if done else None, task_id, user_id))
        if cursor.rowcount == 0:
            raise AgentError("任务不存在、计划未确认或无权操作")
        plan_id = _one(cursor, "SELECT plan_id FROM t_learning_task WHERE id=%s", (task_id,))["plan_id"]
        _audit(cursor, user_id, plan_id, "COMPLETE_TASK" if done else "REOPEN_TASK", "SUCCEEDED", False, {"taskId": task_id}, {})
        return _load(cursor, user_id, plan_id)


def chat(user_id: int, conversation_id: str | None, message: str) -> dict:
    started = time.monotonic()
    with transaction() as connection, connection.cursor() as cursor:
        valid = _one(cursor, "SELECT id FROM t_agent_conversation WHERE id=%s AND user_id=%s", (conversation_id, user_id)) if conversation_id else None
        cid = valid["id"] if valid else str(uuid.uuid4())
        if not valid:
            cursor.execute("INSERT INTO t_agent_conversation VALUES(%s,%s,%s,NOW(),NOW())", (cid, user_id, message[:50]))
        cursor.execute("INSERT INTO t_agent_message VALUES(%s,%s,%s,'user',%s,NULL,NULL,NULL,NULL,NOW())", (str(uuid.uuid4()), cid, user_id, message))
        cursor.execute("SELECT role,content FROM t_agent_message WHERE conversation_id=%s AND user_id=%s ORDER BY created_at DESC LIMIT 12", (cid, user_id))
        history = list(reversed(cursor.fetchall()))
        intent, reply, model_used = decide(message, history)
        tool, result = None, None
        if intent == "PROGRESS":
            tool, result = "get_current_plan", current_plan(user_id)
            reply = "你还没有学习计划，可以先让我制定一份。" if result.get("empty") else f"当前计划进度 {result['progress']}%，已完成 {result['completedTasks']} / {result['totalTasks']} 个任务。"
        elif intent == "COURSE_SEARCH":
            tool, result = "search_courses", recommend(user_id, message, message, "beginner", 5)
            reply = f"我从平台课程库检索到 {len(result)} 门相关课程，结果已附在回复中。" if result else "暂时没有检索到匹配课程，可以换一个更具体的方向。"
        elif not reply:
            reply = "我可以搜索课程、查询学习进度，也可以通过上方表单生成需要你确认的学习计划。"
        latency = round((time.monotonic() - started) * 1000)
        model_name = settings.ai_model if model_used else "deterministic-fallback"
        cursor.execute("INSERT INTO t_agent_message VALUES(%s,%s,%s,'assistant',%s,%s,%s,%s,%s,NOW())", (str(uuid.uuid4()), cid, user_id, reply, tool, json.dumps(result, ensure_ascii=False, default=str) if result is not None else None, model_name, latency))
        cursor.execute("UPDATE t_agent_conversation SET updated_at=NOW() WHERE id=%s", (cid,))
        return {"conversationId": cid, "reply": reply, "toolName": tool, "toolResult": result, "modelUsed": model_name, "latencyMs": latency}


def history(user_id: int, conversation_id: str) -> list[dict]:
    connection = connect()
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT id,role,content,tool_name toolName,tool_result toolResult,model_used modelUsed,latency_ms latencyMs,created_at createdAt FROM t_agent_message WHERE conversation_id=%s AND user_id=%s ORDER BY created_at", (conversation_id, user_id))
            return cursor.fetchall()
    finally:
        connection.close()


def metrics() -> dict:
    connection = connect()
    try:
        with connection.cursor() as cursor:
            queries = {
                "conversations": "SELECT COUNT(*) value FROM t_agent_conversation",
                "messages": "SELECT COUNT(*) value FROM t_agent_message",
                "toolCalls": "SELECT COUNT(*) value FROM t_agent_message WHERE tool_name IS NOT NULL",
                "fallbackRate": "SELECT COALESCE(ROUND(100*SUM(model_used='deterministic-fallback')/NULLIF(COUNT(*),0),1),0) value FROM t_agent_message WHERE role='assistant'",
                "averageLatencyMs": "SELECT COALESCE(ROUND(AVG(latency_ms)),0) value FROM t_agent_message WHERE role='assistant'",
            }
            return {key: _one(cursor, sql)["value"] for key, sql in queries.items()}
    finally:
        connection.close()
