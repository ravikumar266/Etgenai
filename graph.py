"""
graph.py — LangGraph multi-agent orchestration

Agents:
  chat_node          — Primary agent: Gemini with all tools
  email_approval     — Human-in-the-loop gate for send_email
  tool_node          — Executes all tool calls
  critic_node        — Option 3: Quality verification agent (second LLM pass)

Flow:
  chat_node → [tool calls?]
    → NO  → critic_node → END
    → YES → [is send_email?]
                → YES → email_approval_node → tools → chat_node → critic_node
                → NO  → tool_node → chat_node → critic_node
"""

from typing import Annotated, Any, TypedDict

from langchain_core.messages import (
    AnyMessage, AIMessage, HumanMessage, SystemMessage, ToolMessage
)
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, StateGraph
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode
from langgraph.types import interrupt

from agent.config import llm, logger
from agent.prompts import SYSTEM_PROMPT

# ── Tool imports ──────────────────────────────────────────────────────────────
from agent.tools_search import search_web, fetch_webpage, duckduckgo_search
from agent.tools_google import get_weather, google_doc, update_google_doc
from agent.tools_email import send_email, check_updates
from agent.tools_github import review_pr, get_pr_files, list_prs, get_file, search_code
from agent.tools_rag import (
    ingest_webpage, ingest_pdf, ingest_youtube,
    query_rag, list_rag_collections, delete_rag_collection,
)
from agent.tools_workflow import (
    start_workflow, update_workflow_step,
    get_workflow_status, list_workflows, escalate_workflow,
)
from agent.tools_meeting import (
    process_meeting, check_action_items, escalate_stalled_items,
)
from agent.scheduler import start_scheduler, stop_scheduler, get_scheduler  # noqa: F401

# ── Tool registry ─────────────────────────────────────────────────────────────

TOOLS = [
    # Search & web
    search_web, fetch_webpage, duckduckgo_search,
    # Utilities
    get_weather,
    # Google Docs
    google_doc, update_google_doc,
    # Email
    send_email, check_updates,
    # GitHub code review
    review_pr, get_pr_files, list_prs, get_file, search_code,
    # RAG knowledge base
    ingest_webpage, ingest_pdf, ingest_youtube,
    query_rag, list_rag_collections, delete_rag_collection,
    # Option 1: Process orchestration
    start_workflow, update_workflow_step,
    get_workflow_status, list_workflows, escalate_workflow,
    # Option 2: Meeting intelligence
    process_meeting, check_action_items, escalate_stalled_items,
]

_llm_with_tools = llm.bind_tools(TOOLS)

# Separate lightweight LLM for critic — no tools needed, just text review
_critic_llm = llm

# ── State ─────────────────────────────────────────────────────────────────────

class State(TypedDict):
    messages: Annotated[list[AnyMessage], add_messages]
    critic_score: int          # last critic score (0 = not reviewed yet)
    critic_retries: int        # how many times critic has sent back for revision


# ── Content extraction helper ─────────────────────────────────────────────────

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


# ── Nodes ─────────────────────────────────────────────────────────────────────

def chat_node(state: State) -> State:
    """Primary agent — Gemini with all tools."""
    messages = list(state["messages"])
    if not messages or not isinstance(messages[0], SystemMessage):
        messages = [SYSTEM_PROMPT] + messages

    response = _llm_with_tools.invoke(messages)

    if isinstance(response, AIMessage):
        if response.tool_calls:
            logger.info(f"[Chat] Tool(s): {[tc['name'] for tc in response.tool_calls]}")
        else:
            text = _extract_text(response.content)
            logger.info(f"[Chat] Reply ({type(response.content).__name__}): {text[:100]}")

    return {
        "messages": [response],
        "critic_score": state.get("critic_score", 0),
        "critic_retries": state.get("critic_retries", 0),
    }


def critic_node(state: State) -> State:
    """
    Option 3: Quality verification agent (second independent LLM pass).

    Scores the main agent's last response 1-10 on:
      - Completeness  (did it fully answer the request?)
      - Accuracy      (no hallucinated facts, IDs, or URLs?)
      - Actionability (does the user have what they need?)

    If score < 7 AND retries < 2: injects feedback and routes back to chat.
    Otherwise: passes through to END.

    This is what makes the system a genuine multi-agent architecture —
    two independent agents with different roles collaborating on quality.
    """
    messages = state["messages"]
    retries = state.get("critic_retries", 0)

    # Skip if already retried twice — avoid infinite loops
    if retries >= 2:
        logger.info("[Critic] Max retries reached — passing through")
        return {"messages": [], "critic_score": state.get("critic_score", 0), "critic_retries": retries}

    # Find the last non-empty, non-tool-calling AI message
    last_ai = None
    for msg in reversed(messages):
        if msg.type == "ai" and not getattr(msg, "tool_calls", None):
            text = _extract_text(msg.content)
            if text and len(text) > 30:
                last_ai = msg
                break

    if not last_ai:
        return {"messages": [], "critic_score": 0, "critic_retries": retries}

    # Find the original user request for context
    last_human = next(
        (m for m in reversed(messages) if m.type == "human"), None
    )
    user_request = _extract_text(last_human.content) if last_human else ""
    response_text = _extract_text(last_ai.content)

    critic_prompt = f"""You are a strict quality control agent reviewing an AI assistant's response.

USER REQUEST:
{user_request[:500]}

AI RESPONSE TO REVIEW:
{response_text[:2000]}

Score this response strictly on a scale of 1-10:
- 10: Perfect. Complete, accurate, actionable, no issues.
- 7-9: Good. Minor gaps but acceptable.
- 4-6: Poor. Missing key information or has errors.
- 1-3: Unacceptable. Wrong, incomplete, or harmful.

Check for:
1. Completeness — did it fully address the user's request?
2. Accuracy — are all facts, URLs, IDs, and names correct? (flag hallucinations)
3. Actionability — does the user have everything they need to act?

Reply in EXACTLY this format, nothing else:
SCORE: [1-10]
VERDICT: PASS or RETRY
ISSUES: [comma-separated list of specific issues, or "none" if PASS]"""

    try:
        review = _critic_llm.invoke(critic_prompt)
        review_text = _extract_text(review.content) if hasattr(review, "content") else ""

        score = 10
        verdict = "PASS"
        issues = "none"

        for line in review_text.split("\n"):
            line = line.strip()
            if line.startswith("SCORE:"):
                try:
                    score = int(line.split(":")[1].strip().split()[0])
                except Exception:
                    score = 10
            elif line.startswith("VERDICT:"):
                verdict = line.split(":")[1].strip().upper()
            elif line.startswith("ISSUES:"):
                issues = line.split(":", 1)[1].strip()

        logger.info(f"[Critic] Score: {score}/10 | Verdict: {verdict} | Issues: {issues[:100]}")

        if verdict == "RETRY" and score <3:
            feedback = HumanMessage(
                content=(
                    f"[QUALITY REVIEW — Score {score}/10]: Your response needs improvement.\n"
                    f"Issues: {issues}\n\n"
                    f"Please revise your answer to fix these specific issues "
                    f"and provide a more complete, accurate response."
                )
            )
            logger.info(f"[Critic] Sending back for revision (retry {retries + 1}/2)")
            return {
                "messages": [feedback],
                "critic_score": score,
                "critic_retries": retries + 1,
            }

        return {"messages": [], "critic_score": score, "critic_retries": retries}

    except Exception as e:
        logger.warning(f"[Critic] Review failed: {e} — passing through")
        return {"messages": [], "critic_score": 0, "critic_retries": retries}


def email_approval_node(state: State) -> State:
    """Human-in-the-loop gate for send_email."""
    last_message = state["messages"][-1]

    if not isinstance(last_message, AIMessage) or not last_message.tool_calls:
        return state

    email_calls = [tc for tc in last_message.tool_calls if tc["name"] == "send_email"]
    if not email_calls:
        return state

    tc = email_calls[0]
    args = tc["args"]

    human_response: str = interrupt({
        "type": "email_approval",
        "message": "An email is ready to send. Please review and approve or deny.",
        "to": args.get("to", ""),
        "subject": args.get("subject", ""),
        "body": args.get("body", ""),
        "total_emails": len(email_calls),
        "instructions": 'Reply with "approve" to send or "deny" to cancel.',
    })

    decision = (human_response or "").strip().lower()

    if decision == "approve":
        logger.info(f"Email approved → to: {args.get('to')}")
        return state

    logger.info(f"Email denied — injecting {len(email_calls)} cancellation(s)")
    cancellations = [
        ToolMessage(
            tool_call_id=call["id"],
            name="send_email",
            content=(
                f"❌ Email to '{call['args'].get('to')}' was cancelled. "
                "Do not resend unless explicitly asked."
            ),
        )
        for call in email_calls
    ]
    return {"messages": cancellations}


# ── Routing ───────────────────────────────────────────────────────────────────

def route_tools(state: State) -> str:
    """After chat_node: route to email_approval, tools, or critic."""
    last_message = state["messages"][-1]

    if not isinstance(last_message, AIMessage):
        return "critic"

    tool_calls = getattr(last_message, "tool_calls", None)
    if not tool_calls:
        return "critic"   # no tools → go to critic before END

    tool_names = [tc["name"] for tc in tool_calls]
    logger.info(f"Routing — tools: {tool_names}")
    return "email_approval" if "send_email" in tool_names else "tools"


def route_after_approval(state: State) -> str:
    last_message = state["messages"][-1]
    return "chat" if isinstance(last_message, ToolMessage) else "tools"


def route_after_critic(state: State) -> str:
    """After critic_node: if it injected feedback → back to chat. Else → END."""
    messages = state["messages"]
    # Critic injects a HumanMessage when it wants revision
    # Check if the very last message is that feedback
    if messages and isinstance(messages[-1], HumanMessage):
        content = _extract_text(messages[-1].content) if hasattr(messages[-1], "content") else ""
        if "[QUALITY REVIEW" in str(messages[-1].content):
            return "chat"
    return END


# ── Graph compilation ─────────────────────────────────────────────────────────

def build_graph():
    graph = StateGraph(State)

    graph.add_node("chat", chat_node)
    graph.add_node("email_approval", email_approval_node)
    graph.add_node("tools", ToolNode(TOOLS))
    graph.add_node("critic", critic_node)           # Option 3: Critic agent

    graph.set_entry_point("chat")

    graph.add_conditional_edges(
        "chat",
        route_tools,
        {
            "email_approval": "email_approval",
            "tools": "tools",
            "critic": "critic",
        },
    )
    graph.add_conditional_edges(
        "email_approval",
        route_after_approval,
        {"tools": "tools", "chat": "chat"},
    )
    graph.add_edge("tools", "chat")

    graph.add_conditional_edges(
        "critic",
        route_after_critic,
        {"chat": "chat", END: END},
    )

    return graph.compile(checkpointer=MemorySaver())


chatbot = build_graph()
logger.info(
    "LangGraph Multi-Agent System compiled ✓\n"
    "  Agents: chat_node (primary) + critic_node (verifier) + email_approval (HITL)\n"
    "  Options: 1=workflow orchestration, 2=meeting intelligence, 3=critic agent"
)