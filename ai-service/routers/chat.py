"""
FastAPI router for AI chat endpoint.
POST /api/ai/chat  — accepts a user message, returns AI response.
"""

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel

from agent.trading_agent import build_agent
from tools import market_tools
import config

router = APIRouter(prefix="/api/ai", tags=["AI Assistant"])


class ChatRequest(BaseModel):
    message: str


class ChatResponse(BaseModel):
    reply: str


@router.post("/chat", response_model=ChatResponse)
async def chat(request: Request, body: ChatRequest) -> ChatResponse:
    """
    Process a natural-language question about the user's portfolio.

    Requires a valid JWT in the Authorization header — it is forwarded
    to the backend to fetch portfolio data on behalf of the user.
    The AI never executes trades or modifies any data.
    """
    # Extract JWT from incoming request
    auth_header = request.headers.get("Authorization", "")
    if not auth_header.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")

    token = auth_header.split(" ", 1)[1]

    # Configure tools with the user's token for this request
    market_tools.configure(backend_url=config.BACKEND_URL, token=token)

    # Build a fresh agent (tool state is per-request via configure())
    agent = build_agent(market_tools.ALL_TOOLS)

    try:
        result = await _run_agent(agent, body.message)
        return ChatResponse(reply=result)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Agent error: {exc}") from exc


async def _run_agent(agent, message: str) -> str:
    """Run the agent synchronously (Ollama calls are blocking)."""
    import asyncio

    loop = asyncio.get_event_loop()
    result = await loop.run_in_executor(None, lambda: agent.invoke({"input": message}))
    return result.get("output", "I could not generate a response.")
