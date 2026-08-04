"""
OpenEx AI Astromech — FastAPI microservice (Week 3).

Provides a read-only AI assistant backed by a local Ollama LLM.
Connects to the Kotlin backend REST API to fetch portfolio data.

Start:
    uvicorn main:app --host 0.0.0.0 --port 8001 --reload

Docker:
    docker compose up ai-service
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

import config
from routers.chat import router as chat_router

app = FastAPI(
    title="OpenEx AI Astromech",
    description="Read-only AI assistant for OpenEx trading data powered by a local Ollama model.",
    version="1.0.0",
)

# CORS — allow the React frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=config.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat_router)


@app.get("/health")
async def health() -> dict:
    """Health check endpoint."""
    return {
        "status": "ok",
        "service": "openex-ai-astromech",
        "model": config.OLLAMA_MODEL,
        "ollama": config.OLLAMA_BASE_URL,
    }
