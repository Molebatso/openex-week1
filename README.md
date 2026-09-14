OpenEx 3.0 — Simulated Crypto Exchange & AI Trading Terminal

A lightweight, simulated cryptocurrency exchange built as a capstone project. Real double-entry accounting, a real in-memory matching engine, live order book and trade streaming over WebSockets, a simulated market feed for continuous chart movement, and a local-LLM-powered AI trading assistant.

This is a learning/demo project. All funds, trades, and balances are simulated — no real money or real exchanges are involved anywhere in this system.

Architecture
openex-week1/
├── backend/          Kotlin + Spring Boot — core exchange engine
├── frontend/          React + Vite + TypeScript — trading terminal UI
├── market-sim/         Python + Flask — simulated price feed generator
└── ai-assistant/        Python + Flask + LangChain + Ollama — AI chat assistant
┌───────────────────┐        REST + WebSocket        ┌────────────────────────┐
│                   │ ──────────────────────────────▶ │                       │
│     Frontend        │                                 │       Backend           │
│  React / Vite         │ ◀──────────────────────────────  │  Kotlin / Spring Boot    │
│     :3000            │        /api/*, /ws               │          :8080           │
│                   │                                 │    ↕ PostgreSQL :5432   │
└──────────┬────────┘                                 └───────────▲────────────┘
           │                                                       │
           │  /api/sim/*                                           │ GET /api/wallet
           ▼  (simulated price feed only)                          │ (tool call, uses
┌────────────────────────┐                                         │  the user's own JWT)
│    Market Simulator       │                             ┌────────┴─────────────┐
│     Python / Flask           │                            │    AI Assistant         │
│           :5001              │                            │  Python / Flask /         │
└────────────────────────┘                            │  LangChain / Ollama       │
                                                        │           :8001            │
                                                        └────────────────────────┘

Four independently-run services:

Service	Port	Owns
Backend (Kotlin/Spring Boot)	:8080	Accounts, wallets, orders, matching engine, ledger, real trades, WebSocket broadcasts
Frontend (React/Vite)	:3000	The trading terminal UI
Market simulator (Python/Flask)	:5001	A synthetic, continuously-moving price feed — unrelated to real trading activity
AI assistant (Python/Flask/LangChain/Ollama)	:8001	A local-LLM chat assistant that can look up the user's real wallet balances via a tool call

The market simulator and AI assistant are independent Python services — neither depends on the other, and both are optional extensions on top of the core exchange (backend + frontend), which works fully on its own.

Tech stack
Layer	Technology
Backend	Kotlin, Spring Boot 3.x, Spring Security (JWT), Spring Data JPA, Flyway, PostgreSQL
Real-time	Spring WebSocket (STOMP over SockJS)
Frontend	React, TypeScript, Vite, Tailwind CSS, Axios, @stomp/stompjs, SockJS
Market simulator	Python 3.12, Flask, NumPy, Pandas
AI assistant	Python 3.12, Flask, LangChain, Ollama (local LLM — no external AI APIs)
Testing	JUnit 5, MockK
Build tools	Gradle (Kotlin DSL) — backend; npm/Vite — frontend; pip — both Python services
Prerequisites
JDK 21 (Temurin recommended). Kotlin's compiler does not reliably support newer host JVMs (25+) — see Troubleshooting.
Node.js 18+ and npm
Python 3.12 (or close)
PostgreSQL 16 — local install or Docker
Gradle 8.x — installed globally, or via the project's own wrapper (gradlew)
Ollama (for the AI assistant only) — https://ollama.com, with a tool-calling-capable model pulled (e.g. ollama pull llama3.1)
Project setup — step by step
1. Database (PostgreSQL)
powershell
docker run --name openex-postgres -e POSTGRES_DB=openex -e POSTGRES_USER=openex -e POSTGRES_PASSWORD=openex -p 5432:5432 -d postgres:16

Or point backend/src/main/resources/application.yml at an existing local Postgres — match the database name/user/password.

2. Backend (Kotlin / Spring Boot)
powershell
cd backend

If you don't already have JDK 21, install it (https://adoptium.net/temurin/releases/?version=21) and pin it in backend/gradle.properties:

properties
org.gradle.java.home=C:/Program Files/Java/jdk-21.x.x
powershell
.\gradlew clean build
.\gradlew bootRun

Runs on http://localhost:8080. Flyway migrations run automatically. Confirm:

powershell
curl http://localhost:8080/actuator/health
3. Frontend (React / Vite)
powershell
cd frontend
npm install
npm run dev

Runs on http://localhost:3000. Proxies to both Python services and the backend — confirm vite.config.ts has all three proxy rules, most-specific first:

typescript
server: {
  port: 3000,
  proxy: {
    '/api/sim': { target: 'http://localhost:5001', changeOrigin: true },
    '/api':     { target: 'http://localhost:8080', changeOrigin: true },
    '/ws':      { target: 'http://localhost:8080', changeOrigin: true, ws: true },
  },
},

(The AI assistant is called directly at http://localhost:8001 from frontend/src/api/ai.ts rather than through the proxy — no rule needed for it unless you choose to add one.)

4. Market simulator (Python / Flask) — optional

Makes charts move continuously even with zero real trades.

powershell
cd market-sim
python -m venv venv
.\venv\Scripts\activate
pip install -r requirements.txt
python app.py

Runs on http://localhost:5001.

5. AI assistant (Python / Flask / LangChain / Ollama) — optional
powershell
ollama pull llama3.1        # if you haven't already
powershell
cd ai-assistant
python -m venv venv
.\venv\Scripts\activate
pip install -r requirements.txt
python app.py

Runs on http://localhost:8001. Requires Ollama running locally (ollama serve, usually automatic after install) and your backend running on :8080 (the wallet-lookup tool calls it directly).

Running everything together

Up to five things running at once:

PostgreSQL (background service/container)
backend: .\gradlew bootRun
frontend: npm run dev
market-sim (optional): python app.py
ai-assistant (optional): python app.py, with ollama serve running separately

Open http://localhost:3000 once backend + frontend are up. The other two enhance the experience but aren't required for core trading to work.

API reference
Backend (Kotlin, :8080)
Method	Endpoint	Auth	Description
POST	/api/auth/register	No	Create account, returns JWT
POST	/api/auth/login	No	Authenticate, returns JWT
GET	/api/wallet	Yes	List wallets for the authenticated user
POST	/api/wallet/deposit	Yes	Add simulated demo funds
POST	/api/orders	Yes	Place an order (LIMIT or MARKET)
GET	/api/orders	Yes	List the authenticated user's orders
GET	/api/orders/{id}	Yes	Get a single order
DELETE	/api/orders/{id}	Yes	Cancel an open order
GET	/api/trades	Yes	List trades the authenticated user participated in
GET	/api/orderbook?symbol=BTC/USD&depth=20	Yes	Live order book snapshot
GET	/api/market/recent-trades?symbol=BTC/USD	Yes	Recent public trades for a symbol
GET	/api/market/stats?symbol=BTC/USD	Yes	24h high/low/volume/change
GET	/api/market/latest-price?symbol=BTC/USD	Yes	Most recent execution price

All authenticated endpoints require Authorization: Bearer <jwt>.

WebSocket topics (STOMP over SockJS, endpoint /ws):

Topic	Payload
/topic/trades/{symbol}	Each executed trade, as it happens
/topic/orderbook/{symbol}	Order book snapshot, after every change
/topic/market/{symbol}	Market price + stats update
Market simulator (:5001, proxied under /api/sim)
Method	Endpoint	Description
GET	/health	Health check + active symbols
GET	/api/sim/market/symbols	List simulated symbols
GET	/api/sim/market/ticks?symbol=BTC/USD&limit=60	Historical + current simulated ticks, with moving averages
GET	/api/sim/market/latest?symbol=BTC/USD	Current simulated price
AI assistant (:8001)
Method	Endpoint	Auth	Description
GET	/health	No	Health check
POST	/api/ai/chat	Yes (JWT)	Send a chat message; the agent can call the wallet tool to fetch real balances

Request:

json
{ "message": "what's my BTC balance?" }

Response:

json
{ "reply": "You currently hold 0.00 BTC and 10,000.00 USD in your simulated wallet." }
Core architecture decisions
Double-entry ledger — every trade produces exactly 4 immutable ledger entries (buyer debit/credit, seller debit/credit). Wallet balances are never mutated directly outside this flow.
In-memory matching engine — price-time priority, LIMIT and MARKET orders, partial fills, cancellation. Resting orders reload from the database on startup.
JWT auth, stateless — no server-side sessions.
UUID primary keys, DB-generated — @GeneratedValue(strategy = GenerationType.UUID) with a nullable Kotlin id field, not a client-assigned default (see Troubleshooting).
Two independent Python microservices, deliberately not merged — the market simulator has zero knowledge of real trades; the AI assistant has zero knowledge of the simulated feed. Each does one job.
AI assistant is stateless per request and uses a local Ollama model only — no external LLM API calls, per the "air-gapped GenAI" requirement.
The wallet tool never exposes the JWT to the LLM — it's bound via closure, so the model decides whether to call the tool, never how to authenticate it.
Testing
powershell
cd backend
.\gradlew test

Report: backend/build/reports/tests/test/index.html

Single test class:

powershell
.\gradlew test --tests "com.openex.backend.matching.MatchingEngineTest"

AI assistant validation (no live Ollama needed for these):

powershell
curl -X POST http://localhost:8001/api/ai/chat -H "Content-Type: application/json" -d "{}"
# -> 400 message is required
curl -X POST http://localhost:8001/api/ai/chat -H "Content-Type: application/json" -d "{\"message\":\"hi\"}"
# -> 401 Authorization header required
Known limitations
Market simulator has no real trade volume — volume/OBV figures shown alongside its feed are synthetic (derived from price movement), and labeled as such in the UI.
Longer chart timeframes (3M/6M/YTD/1Y/5Y) are limited by available history — neither the backend nor the simulator has deep historical backfill. Expected, not a bug.
AI assistant has no multi-turn memory yet — each chat request builds a fresh agent; fine for single Q&A, would need session memory added for a real back-and-forth conversation.
Tool-calling reliability depends on the Ollama model — smaller/older models may not reliably decide to call the wallet tool, or may hallucinate a balance instead of calling it. Use a model known to support tool calling (llama3.1, llama3.2, qwen2.5, mistral-nemo).
Deposit is a fixed demo faucet, not a configurable amount-picker.
Troubleshooting

gradlew build fails with IllegalArgumentException: <version> from the Kotlin compiler Host JDK too new for Kotlin's compiler (commonly JDK 25+). Install JDK 21, pin it in backend/gradle.properties:

properties
org.gradle.java.home=C:/Program Files/Java/jdk-21.x.x

Then .\gradlew --stop before rebuilding.

.\gradlew not recognized cd backend first, or the wrapper was never generated — gradle wrapper --gradle-version 8.9 via a standalone Gradle install.

Postgres password authentication failed for user "openex" Container/instance credentials don't match application.yml. If using a Docker volume, docker-compose down -v && docker-compose up -d postgres to reset it.

org.hibernate.StaleObjectStateException on save An entity's id defaults to a client-generated UUID instead of null. Must be val id: UUID? = null alongside @GeneratedValue(strategy = GenerationType.UUID).

WebSocket /ws/info returns 403 Missing .requestMatchers("/ws/**").permitAll() in SecurityConfig, and/or no WebSocketConfig registering the /ws STOMP endpoint.

Browser: Uncaught ReferenceError: global is not defined sockjs-client needs a global polyfill Vite doesn't provide by default:

typescript
define: { global: 'globalThis' },

ENOSPC: no space left on device Genuine low disk space. Free space and retry — not a code issue.

Frontend shows 500s for /api/orderbook, /api/market/stats, /api/market/recent-trades Confirm those controllers actually exist in the backend.

Simulator chart shows the red error panel Either market-sim/app.py isn't running, or the /api/sim proxy rule is missing/out of order in vite.config.ts (must come before the general /api rule).

AI assistant returns 502 "Failed to connect to Ollama" ollama serve isn't running, or the model in OLLAMA_MODEL hasn't been pulled — ollama pull llama3.1 (or whichever model you're targeting).

AI assistant gives a wrong/made-up balance instead of calling the tool The Ollama model you're using likely doesn't support reliable tool calling. Switch to llama3.1, llama3.2, qwen2.5, or mistral-nemo via the OLLAMA_MODEL environment variable.

License

Educational capstone project — not intended for production or real financial use.
