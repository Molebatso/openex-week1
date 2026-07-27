# OpenEx 3.0 — Simulated Crypto Exchange

A production-quality cryptocurrency trading platform built as a 3-week capstone.

**Week 1 (this branch):** Kotlin/Spring Boot backend — matching engine, double-entry ledger, JWT auth.

---

## Quick Start

```bash
# Boot the full stack (Postgres + backend)
docker compose up --build
```

The API is available at `http://localhost:8080`.

---

## API Reference

### Authentication

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@ex.com","password":"password123"}'

# Login — returns JWT
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'
```

All other endpoints require `Authorization: Bearer <token>`.

### Wallet

```bash
# Get all wallets
curl http://localhost:8080/api/wallet \
  -H "Authorization: Bearer $TOKEN"
```

### Orders

```bash
# Place a LIMIT BUY order
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BTC/USD",
    "side": "BUY",
    "type": "LIMIT",
    "price": 50000.00,
    "quantity": 0.5,
    "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000"
  }'

# Place a MARKET SELL order
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol":"BTC/USD","side":"SELL","type":"MARKET","quantity":0.1}'

# List orders
curl http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN"

# Cancel order
curl -X DELETE http://localhost:8080/api/orders/<order-id> \
  -H "Authorization: Bearer $TOKEN"
```

### Trades

```bash
# Trade history
curl http://localhost:8080/api/trades \
  -H "Authorization: Bearer $TOKEN"
```

---

## Architecture

```
backend/src/main/kotlin/com/openex/backend/
├── config/         # Security configuration
├── controller/     # REST endpoints (auth, wallet, orders, trades)
├── dto/            # Request/response shapes
├── entity/         # JPA entities + enums
├── exception/      # Custom exceptions + global handler
├── ledger/         # Double-entry LedgerService
├── matching/       # In-memory OrderBook + MatchingEngineService
├── repository/     # Spring Data JPA repositories
├── security/       # JWT provider, filter, UserDetails
└── service/        # Business logic (auth, wallet, order, trade)
```

### Key Design Decisions

**Double-Entry Ledger:** Every trade creates exactly 4 immutable `LedgerEntry` rows — a DEBIT and CREDIT for each currency in each direction. Wallet balances are always updated via `LedgerService.settleTrade()`, never directly. The ledger asserts `sum(DEBIT) == sum(CREDIT)` per currency before committing.

**Matching Engine:** Each symbol has its own `OrderBook`. Bids are sorted by price DESC then arrival time ASC; asks by price ASC then arrival time ASC (price-time priority). Matching is guarded by a `ReentrantLock` per book. The engine returns `MatchResult` objects; `MatchingEngineService` handles persistence in a single `@Transactional` boundary.

**API Idempotency:** Order creation accepts an `Idempotency-Key` UUID header. If a key has been seen before, the cached `OrderResponse` is returned without creating a duplicate.

**JWT Security:** Stateless sessions. All endpoints are protected except `POST /api/auth/register` and `POST /api/auth/login`. Passwords use BCrypt (strength 12).

---

## Database Schema

Managed by Flyway migrations in `src/main/resources/db/migration/`:

| Migration | Table | Purpose |
|---|---|---|
| V1 | `users` | Accounts with BCrypt password hash |
| V2 | `wallets` | Per-user per-currency balance (non-negative constraint) |
| V3 | `orders` | Order book entries with idempotency key |
| V4 | `trades` | Immutable execution records |
| V5 | `ledger_entries` | Double-entry accounting rows |

---

## Running Tests

```bash
cd backend
gradle test
```

Test coverage:
- `AuthServiceTest` — registration, login, duplicate detection
- `WalletServiceTest` — credit/debit, insufficient funds, zero-amount guard
- `OrderBookTest` — price-time priority, partial fills, market orders, cancellation
- `MatchingEngineTest` — 10 concurrent orders, ledger integration, idempotency
- `LedgerServiceTest` — double-entry invariant, 4-entry rule, immutability

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/openex` | Postgres JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `openex` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `openex_secret` | DB password |
| `JWT_SECRET` | *(unsafe default)* | **Change in production** — min 512-bit key |
| `JWT_EXPIRATION_MS` | `86400000` | Token TTL in ms (default: 24 h) |

---

## Week 2 (coming)

Spring WebSocket endpoint broadcasting live order book updates + React trading terminal.

## Week 3 (coming)

Python/Flask market simulator + LangChain agent powered by local Ollama LLM.
