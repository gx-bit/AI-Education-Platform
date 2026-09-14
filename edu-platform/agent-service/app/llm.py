import json
import re

import httpx

from .config import settings


def _completion(messages: list[dict], temperature: float) -> str | None:
    if not settings.ai_base_url or not settings.ai_api_key:
        return None
    try:
        response = httpx.post(
            f"{settings.ai_base_url}/chat/completions",
            headers={"Authorization": f"Bearer {settings.ai_api_key}"},
            json={"model": settings.ai_model, "temperature": temperature, "messages": messages},
            timeout=12,
        )
        response.raise_for_status()
        return response.json()["choices"][0]["message"]["content"].strip()
    except (httpx.HTTPError, KeyError, IndexError, TypeError):
        return None


def summarize(goal: str, interest: str, level: str, weeks: int, hours: int, courses: list[dict]) -> str:
    fallback = "Agent 已调用课程推荐工具，并根据目标、兴趣、水平和可用时间拆解任务。确认计划后才会开始记录进度。"
    names = "、".join(str(c.get("title")) for c in courses[:6] if c.get("title")) or "暂无匹配课程"
    prompt = f"请用80字以内中文说明规划依据，不夸大能力，不承诺结果。目标：{goal}；兴趣：{interest}；水平：{level}；周期：{weeks}周；每周：{hours}小时；课程：{names}"
    return _completion([{"role": "system", "content": "你是教育平台学习规划 Agent，只解释业务系统已生成的计划。"}, {"role": "user", "content": prompt}], 0.2) or fallback


def decide(message: str, history: list[dict]) -> tuple[str, str, bool]:
    fallback = "PROGRESS" if re.search(r"进度|完成多少|计划情况", message) else "COURSE_SEARCH" if re.search(r"推荐|课程|学习什么", message) else "GENERAL"
    prompt = '选择工具，只输出JSON：{"intent":"PROGRESS|COURSE_SEARCH|GENERAL","reply":"简短回复"}。不得声称执行未执行的动作。历史：' + json.dumps(history, ensure_ascii=False, default=str) + "；用户：" + message
    raw = _completion([{"role": "system", "content": "你是受控学习 Agent，只能选择给定工具。"}, {"role": "user", "content": prompt}], 0.1)
    if not raw:
        return fallback, "", False
    try:
        parsed = json.loads(raw.replace("```json", "").replace("```", "").strip())
        intent = parsed.get("intent", "GENERAL")
        return (intent if intent in {"PROGRESS", "COURSE_SEARCH", "GENERAL"} else "GENERAL", str(parsed.get("reply", "")), True)
    except (json.JSONDecodeError, AttributeError):
        return fallback, "", False
