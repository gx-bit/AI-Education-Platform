from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.responses import JSONResponse

from .database import initialize_schema
from .schemas import ChatRequest, PlanRequest
from .service import AgentError, change_task, chat, confirm_plan, create_plan, current_plan, history, metrics


@asynccontextmanager
async def lifespan(_: FastAPI):
    initialize_schema()
    yield


app = FastAPI(title="AI Education Python Agent", version="2.0.0", lifespan=lifespan)


def ok(data):
    return {"code": 200, "message": "操作成功", "data": data}


def user_id(x_user_id: str | None = Header(default=None)) -> int:
    if not x_user_id or not x_user_id.isdigit():
        raise HTTPException(status_code=401, detail="未登录或登录已过期")
    return int(x_user_id)


@app.exception_handler(AgentError)
async def agent_error(_, error: AgentError):
    return JSONResponse(status_code=error.status, content={"code": error.status, "message": error.message, "data": None})


@app.get("/actuator/health")
def health():
    return {"status": "UP", "service": "python-agent-service"}


@app.get("/api/agent/plans/current")
def get_current(uid: int = Depends(user_id)):
    return ok(current_plan(uid))


@app.post("/api/agent/plans/preview")
def preview(request: PlanRequest, uid: int = Depends(user_id)):
    return ok(create_plan(uid, request))


@app.post("/api/agent/plans/{plan_id}/confirm")
def confirm(plan_id: str, uid: int = Depends(user_id)):
    return ok(confirm_plan(uid, plan_id))


@app.post("/api/agent/tasks/{task_id}/complete")
def complete(task_id: str, uid: int = Depends(user_id)):
    return ok(change_task(uid, task_id, True))


@app.post("/api/agent/tasks/{task_id}/reopen")
def reopen(task_id: str, uid: int = Depends(user_id)):
    return ok(change_task(uid, task_id, False))


@app.post("/api/agent/chat")
def agent_chat(request: ChatRequest, uid: int = Depends(user_id)):
    return ok(chat(uid, request.conversation_id, request.message))


@app.get("/api/agent/conversations/{conversation_id}/messages")
def messages(conversation_id: str, uid: int = Depends(user_id)):
    return ok(history(uid, conversation_id))


@app.get("/api/agent/admin/metrics")
def admin_metrics(x_user_role: str = Header(default=""), _: int = Depends(user_id)):
    if x_user_role.lower() != "admin":
        raise HTTPException(status_code=403, detail="权限不足")
    return ok(metrics())
