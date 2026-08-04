# OpenEx 3.0 — Simulated Crypto Exchange

A production-quality cryptocurrency trading platform (3-week capstone project).

## Stack

| Layer | Technology |
|---|---|
| Backend | Kotlin 2.0 / Spring Boot 3.4, Gradle, JPA/Hibernate |
| Database | PostgreSQL 16, Flyway migrations |
| Auth | JWT (JJWT 0.12), BCrypt |
| WebSocket | STOMP over SockJS (Spring WebSocket) |
| API docs | SpringDoc OpenAPI / Swagger UI |
| Frontend | React 18, Vite, TypeScript, Tailwind CSS |
| AI Service | Python 3.12, FastAPI, LangChain, Ollama |
| Runtime | Docker Compose (5 services) |

## Project structure

```
backend/          Kotlin/Spring Boot matching engine (Week 1 + 2)
frontend/         React trading terminal (Week 2)
ai-service/       Python AI Astromech microservice (Week 3)
docker-compose.yml
.github/workflows/ci.yml
```

## Quick start (local, Docker required)

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| React terminal | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| AI service | http://localhost:8001 |
| Ollama | http://localhost:11434 |

### Pull the AI model (first boot only)

```bash
docker compose exec ollama ollama pull llama3.2
```

## Week 1 (complete)

- Kotlin + Spring Boot backend
- PostgreSQL + Flyway migrations
- JWT authentication (register / login)
- Wallet management (USD, BTC, ETH)
- In-memory matching engine (price-time priority, partial fills, market/limit)
- Double-entry ledger
- Full unit test suite

## Week 2 (complete)

Backend additions:
- Spring WebSocket (STOMP) — live order book, trades, market stats
- `GET /api/orders/{id}` — single order lookup
- `GET /api/orderbook?symbol=…` — live order book snapshot
- `GET /api/market/latest-price?symbol=…`
- `GET /api/market/stats?symbol=…` — 24 h rolling stats
- `GET /api/market/recent-trades?symbol=…`
- SpringDoc OpenAPI / Swagger UI
- CORS support

React frontend (`frontend/`):
- Dark TradingView-style dashboard
- Wallet panel, Order entry (limit/market), live Order book
- Trade history (WebSocket live), Open orders with cancel
- Market statistics bar

## Week 3 (complete)

- Python FastAPI AI Astromech microservice (`ai-service/`)
- LangChain + Ollama (local model — no cloud)
- Read-only tools: wallet, trades, orders, market stats
- AI Assistant panel in the React UI

## Environment variables (backend)

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/openex` | Postgres JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `openex` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `openex_secret` | DB password |
| `JWT_SECRET` | *(unsafe default)* | Min 512-bit key — change in production |
| `JWT_EXPIRATION_MS` | `86400000` | Token TTL in ms (24 h) |

## Environment variables (AI service)

| Variable | Default | Description |
|---|---|---|
| `BACKEND_URL` | `http://localhost:8080` | Kotlin backend URL |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama runtime URL |
| `OLLAMA_MODEL` | `llama3.2` | Model to use (must be pulled) |

## Running tests

```bash
cd backend && gradle test --no-daemon
```

## User preferences
