"""
Configuration for the OpenEx AI Astromech assistant service.
All settings can be overridden via environment variables.
"""

import os

# Backend REST API (used by tools to fetch portfolio data)
BACKEND_URL: str = os.getenv("BACKEND_URL", "http://localhost:8080")

# Ollama endpoint
OLLAMA_BASE_URL: str = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")

# Model to use — must be pulled in Ollama first (e.g. `ollama pull llama3.2`)
OLLAMA_MODEL: str = os.getenv("OLLAMA_MODEL", "llama3.2")

# FastAPI service
AI_SERVICE_PORT: int = int(os.getenv("AI_SERVICE_PORT", "8001"))

# CORS — allow all origins by default (frontend may live on any port)
CORS_ORIGINS: list[str] = os.getenv("CORS_ORIGINS", "*").split(",")
