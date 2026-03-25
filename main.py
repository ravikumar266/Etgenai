"""
main.py — FastAPI server for the LangGraph chatbot

Endpoints:
  POST /chat                    → send a message, get a reply
  POST /chat/approve            → approve or deny a pending email
  POST /chat/reset              → get a fresh thread_id
  GET  /chat/history/{thread_id}→ clean conversation history
  GET  /chat/debug/{thread_id}  → raw message trace for debugging
  GET  /scheduler/status        → background scheduler status
  POST /scheduler/run-now       → trigger email check immediately
  GET  /health                  → health check

Run with:
  uvicorn main:app --reload
"""

import uuid
from contextlib import asynccontextmanager
from datetime import datetime
from typing import Any, Literal, Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from langchain_core.messages import HumanMessage
from langgraph.types import Command
from pydantic import BaseModel

from graph import chatbot
from agent.scheduler import start_scheduler, stop_scheduler, get_scheduler, _scheduled_check_updates


# ── Lifespan ──────────────────────────────────────────────────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    start_scheduler()
    yield
    stop_scheduler()


app = FastAPI(title="LangGraph Chatbot", version="3.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── Structured error helper ───────────────────────────────────────────────────

def _error(status: int, message: str, detail: str = "") -> HTTPException:
    return HTTPException(
        status_code=status,
        detail={"error": message, "detail": detail} if detail else {"error": message},
    )


# ── Schemas ───────────────────────────────────────────────────────────────────

class ChatRequest(BaseModel):
    message: str
    thread_id: Optional[str] = None


class PendingEmail(BaseModel):
    to: str
    subject: str
    body: str
    instructions: str


class PendingWorkflow(BaseModel):
    type: str
    message: str
    pending_actions: list[str]
    plan_summary: str
    instructions: str


class ChatResponse(BaseModel):
    reply: str
    thread_id: str
    tools_used: list[str]
    pending_email: Optional[PendingEmail] = None
    pending_workflow: Optional[PendingWorkflow] = None


class ApprovalRequest(BaseModel):
    thread_id: str
    decision: Literal["approve", "deny"]


class ApprovalResponse(BaseModel):
    reply: str
    thread_id: str
    tools_used: list[str]
    email_sent: bool


class WorkflowApprovalRequest(BaseModel):
    thread_id: str
    decision: Literal["approve", "deny"]
    modification: Optional[str] = None   # filled when decision="modify"


class WorkflowApprovalResponse(BaseModel):
    reply: str
    thread_id: str
    tools_used: list[str]
    executed: bool   # True if approved and tools ran, False if denied/modified


class HistoryMessage(BaseModel):
    role: str
    content: str
    tool_name: Optional[str] = None


class HistoryResponse(BaseModel):
    thread_id: str
    message_count: int
    messages: list[HistoryMessage]


class DebugMessage(BaseModel):
    index: int
    type: str
    content_type: str
    content_preview: str
    tool_calls: list[Any]
    tool_name: Optional[str]


class DebugResponse(BaseModel):
    thread_id: str
    message_count: int
    messages: list[DebugMessage]


class SchedulerStatus(BaseModel):
    running: bool
    next_run_email: Optional[str]
    next_run_briefing: Optional[str]
    brief_time: str
    brief_city: str
    brief_topics: str


# ── Content helpers ───────────────────────────────────────────────────────────

def _extract_text(content: Any) -> str:
    if isinstance(content, str):
        return content.strip()
    if isinstance(content, list):
        parts = [
            block.get("text", "")
            for block in content
            if isinstance(block, dict) and block.get("type") == "text"
        ]
        return " ".join(parts).strip()
    return ""


def _extract_reply_and_tools(messages: list) -> tuple[str, list[str]]:
    reply = ""
    tools_used = []

    for msg in messages:
        if msg.type == "tool":
            tools_used.append(getattr(msg, "name", "unknown"))
        if msg.type == "ai":
            text = _extract_text(msg.content)
            if text:
                reply = text

    if not reply and not tools_used:
        for msg in messages:
            if msg.type == "ai":
                tool_calls = getattr(msg, "tool_calls", [])
                if tool_calls:
                    names = [tc.get("name", "?") for tc in tool_calls]
                    reply = (
                        f"The agent tried to call {names} but execution did not complete. "
                        "Try again with a fresh thread_id."
                    )
                    break

    return reply, tools_used


def _get_pending_workflow(config: dict) -> Optional[dict]:
    """Return pending workflow approval payload if graph is paused at workflow_approval_node."""
    try:
        snapshot = chatbot.get_state(config)
    except Exception:
        return None
    if not snapshot.next:
        return None
    for task in snapshot.tasks:
        for interrupt_obj in getattr(task, "interrupts", []):
            payload = interrupt_obj.value
            if isinstance(payload, dict) and payload.get("type") == "workflow_approval":
                return payload
    return None


def _get_pending_email(config: dict) -> Optional[dict]:
    try:
        snapshot = chatbot.get_state(config)
    except Exception:
        return None
    if not snapshot.next:
        return None
    for task in snapshot.tasks:
        for interrupt_obj in getattr(task, "interrupts", []):
            payload = interrupt_obj.value
            if isinstance(payload, dict) and payload.get("type") == "email_approval":
                return payload
    return None


# ── Chat routes ───────────────────────────────────────────────────────────────

@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    thread_id = req.thread_id or str(uuid.uuid4())
    config = {"configurable": {"thread_id": thread_id}, "recursion_limit": 25}

    try:
        result = chatbot.invoke(
            {"messages": [HumanMessage(content=req.message)]},
            config=config,
        )
    except Exception as e:
        raise _error(500, "Graph execution failed", str(e))

    reply, tools_used = _extract_reply_and_tools(result["messages"])

    # Check for pending workflow approval first (higher priority)
    pending_wf = _get_pending_workflow(config)
    if pending_wf:
        return ChatResponse(
            reply=(
                "An autonomous process is ready to execute. "
                "Please review the plan below and call POST /workflow/approve "
                "with decision=approve, deny, or modify."
            ),
            thread_id=thread_id,
            tools_used=tools_used,
            pending_workflow=pending_wf,
        )

    pending = _get_pending_email(config)
    if pending:
        return ChatResponse(
            reply="An email is ready to send. Please review the details and approve or deny.",
            thread_id=thread_id,
            tools_used=tools_used,
            pending_email=PendingEmail(
                to=pending.get("to", ""),
                subject=pending.get("subject", ""),
                body=pending.get("body", ""),
                instructions=pending.get("instructions", ""),
            ),
        )

    if not reply:
        reply = (
            "I completed the requested actions but could not produce a text summary. "
            f"Check tools_used or call GET /chat/debug/{thread_id} for the full trace."
        )

    return ChatResponse(reply=reply, thread_id=thread_id, tools_used=tools_used)


@app.post("/chat/approve", response_model=ApprovalResponse)
async def approve_email(req: ApprovalRequest):
    config = {"configurable": {"thread_id": req.thread_id}, "recursion_limit": 25}

    pending = _get_pending_email(config)
    if not pending:
        raise _error(
            400,
            "No pending email approval",
            f"thread_id='{req.thread_id}' is not paused at an approval step.",
        )

    try:
        result = chatbot.invoke(Command(resume=req.decision), config=config)
    except Exception as e:
        raise _error(500, "Graph resume failed", str(e))

    reply, tools_used = _extract_reply_and_tools(result["messages"])
    email_sent = req.decision == "approve"

    if not reply:
        reply = "Email sent successfully." if email_sent else "Email cancelled as requested."

    return ApprovalResponse(
        reply=reply,
        thread_id=req.thread_id,
        tools_used=tools_used,
        email_sent=email_sent,
    )


@app.post("/workflow/approve", response_model=WorkflowApprovalResponse)
async def approve_workflow(req: WorkflowApprovalRequest):
    """
    Approve, deny, or modify a pending autonomous workflow process.

    decision=approve  → executes all pending workflow tools
    decision=deny     → cancels all pending actions cleanly
    decision=modify   → pass modification string, plan is revised before execution
    """
    config = {"configurable": {"thread_id": req.thread_id}, "recursion_limit": 40}

    pending = _get_pending_workflow(config)
    if not pending:
        raise _error(
            400,
            "No pending workflow approval",
            f"thread_id='{req.thread_id}' is not paused at a workflow approval step. "
            "Use POST /chat first to start a workflow.",
        )

    # Build the resume value
    if req.decision == "approve":
        resume_value = "approve"
    elif req.decision == "deny":
        resume_value = "deny"
    else:
        raise _error(400, "Invalid decision", "Use 'approve' or 'deny'.")

    try:
        result = chatbot.invoke(Command(resume=resume_value), config=config)
    except Exception as e:
        raise _error(500, "Workflow resume failed", str(e))

    reply, tools_used = _extract_reply_and_tools(result["messages"])
    executed = req.decision == "approve"

    if not reply:
        reply = (
            "Workflow executed successfully — all steps completed."
            if executed
            else "Workflow cancelled. No actions were taken."
        )

    return WorkflowApprovalResponse(
        reply=reply,
        thread_id=req.thread_id,
        tools_used=tools_used,
        executed=executed,
    )


@app.post("/chat/reset")
async def reset(thread_id: Optional[str] = None):
    return {
        "message": "Use new_thread_id to start a fresh conversation.",
        "new_thread_id": str(uuid.uuid4()),
        "previous_thread_id": thread_id,
    }


@app.get("/chat/history/{thread_id}", response_model=HistoryResponse)
async def get_history(thread_id: str):
    config = {"configurable": {"thread_id": thread_id}}
    try:
        snapshot = chatbot.get_state(config)
    except Exception as e:
        raise _error(500, "Failed to load thread state", str(e))

    raw_messages = snapshot.values.get("messages", [])
    if not raw_messages:
        raise _error(404, "Thread not found", f"No messages for thread_id='{thread_id}'")

    history = []
    role_map = {"human": "user", "ai": "assistant", "tool": "tool"}
    for msg in raw_messages:
        role = role_map.get(msg.type, msg.type)
        content = _extract_text(msg.content)
        tool_name = getattr(msg, "name", None) if msg.type == "tool" else None
        if msg.type == "ai" and not content:
            continue   # skip empty intermediate planning steps
        history.append(HistoryMessage(role=role, content=content or "(no text)", tool_name=tool_name))

    return HistoryResponse(thread_id=thread_id, message_count=len(history), messages=history)


@app.get("/chat/debug/{thread_id}", response_model=DebugResponse)
async def debug_thread(thread_id: str):
    config = {"configurable": {"thread_id": thread_id}}
    try:
        snapshot = chatbot.get_state(config)
    except Exception as e:
        raise _error(500, "Failed to load thread state", str(e))

    messages = snapshot.values.get("messages", [])
    debug_msgs = [
        DebugMessage(
            index=i,
            type=msg.type,
            content_type=type(msg.content).__name__,
            content_preview=_extract_text(msg.content)[:300] or "(empty)",
            tool_calls=getattr(msg, "tool_calls", []),
            tool_name=getattr(msg, "name", None),
        )
        for i, msg in enumerate(messages)
    ]

    return DebugResponse(thread_id=thread_id, message_count=len(messages), messages=debug_msgs)


# ── Scheduler routes ──────────────────────────────────────────────────────────

@app.get("/scheduler/status", response_model=SchedulerStatus)
async def scheduler_status():
    scheduler = get_scheduler()

    import os
    brief_time   = os.getenv("BRIEF_TIME", "08:00")
    brief_city   = os.getenv("BRIEF_CITY", "Mumbai")
    brief_topics = os.getenv("BRIEF_TOPICS", "AI,technology,startups")

    if scheduler is None or not scheduler.running:
        return SchedulerStatus(
            running=False,
            next_run_email=None,
            next_run_briefing=None,
            brief_time=brief_time,
            brief_city=brief_city,
            brief_topics=brief_topics,
        )

    email_job    = scheduler.get_job("email_check")
    briefing_job = scheduler.get_job("morning_briefing")

    next_email    = email_job.next_run_time.isoformat()    if email_job    and email_job.next_run_time    else None
    next_briefing = briefing_job.next_run_time.isoformat() if briefing_job and briefing_job.next_run_time else None

    return SchedulerStatus(
        running=True,
        next_run_email=next_email,
        next_run_briefing=next_briefing,
        brief_time=brief_time,
        brief_city=brief_city,
        brief_topics=brief_topics,
    )


@app.post("/scheduler/run-now")
async def scheduler_run_now(job: str = "email"):
    """
    Manually trigger a scheduler job immediately.
    job=email    → run email monitor now
    job=briefing → run morning briefing now (useful for testing)
    """
    scheduler = get_scheduler()
    if scheduler is None or not scheduler.running:
        raise _error(503, "Scheduler not running", "Check /health.")

    from agent.scheduler import _scheduled_check_updates, _morning_briefing

    if job == "briefing":
        try:
            scheduler.add_job(
                _morning_briefing,
                "date",
                run_date=datetime.now(),
                id="morning_briefing_manual",
                replace_existing=True,
            )
            return {
                "message": "Morning briefing triggered — check your email in ~30 seconds.",
                "triggered_at": datetime.now().isoformat(),
                "job": "morning_briefing",
            }
        except Exception as e:
            raise _error(500, "Failed to trigger briefing", str(e))

    # Default: email check
    try:
        scheduler.add_job(
            _scheduled_check_updates,
            "date",
            run_date=datetime.now(),
            id="email_check_manual",
            replace_existing=True,
        )
    except Exception as e:
        raise _error(500, "Failed to trigger email check", str(e))

    return {
        "message": "Email check triggered. Results in server logs.",
        "triggered_at": datetime.now().isoformat(),
        "job": "email_check",
    }


# ── Health ────────────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    scheduler = get_scheduler()
    return {
        "status": "ok",
        "version": app.version,
        "graph_nodes": list(chatbot.get_graph().nodes),
        "scheduler_running": scheduler is not None and scheduler.running,
    }