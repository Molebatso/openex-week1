# OpenEx Market Simulator (Python/Flask)

Generates a continuously moving, simulated price feed per symbol — a
random walk with drift — independent of any real trading activity. The
chart moves on a fixed interval whether or not anyone actually places
an order.

This is a separate service from your Kotlin backend. It does not read
or write anything in your Postgres database and has no knowledge of
real trades — it's purely a synthetic feed for a "the market is always
moving" look.

## Run locally

```powershell
cd market-sim
python -m venv venv
.\venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

Runs on `http://localhost:5001` by default. Health check:
```powershell
curl http://localhost:5001/health
```

## Run via Docker

```powershell
docker build -t openex-market-sim .
docker run -p 5001:5001 openex-market-sim
```

Or add to your existing `docker-compose.yml`:
```yaml
market-sim:
  build:
    context: ./market-sim
    dockerfile: Dockerfile
  ports:
    - "5001:5001"
  environment:
    TICK_INTERVAL_SECONDS: 2
    ALLOWED_ORIGINS: http://localhost:3000
```

## Endpoints

All namespaced under `/api/sim/...` — see "Why /api/sim" below for why.

| Method | Path | Description |
|---|---|---|
| GET | `/health` | Health check + list of active symbols |
| GET | `/api/sim/market/symbols` | List simulated symbols |
| GET | `/api/sim/market/ticks?symbol=BTC/USD&limit=60` | Historical + current ticks with price + moving averages |
| GET | `/api/sim/market/latest?symbol=BTC/USD` | Just the current price |

Example:
```powershell
curl "http://localhost:5001/api/sim/market/ticks?symbol=BTC%2FUSD&limit=10"
```
```json
{
  "symbol": "BTC/USD",
  "ticks": [
    { "timestamp": "2026-08-24T10:00:00+00:00", "price": 97183.42, "ma_short": null, "ma_long": null },
    ...
    { "timestamp": "2026-08-24T10:00:18+00:00", "price": 97210.11, "ma_short": 97195.30, "ma_long": 97180.02 }
  ]
}
```
`ma_short`/`ma_long` are `null` until enough history exists for that window (5 and 20 ticks respectively) — they are never faked/padded.

## Why `/api/sim/...` and not `/api/market/...`

Your Kotlin backend **already owns** `/api/market/stats`, `/api/market/recent-trades`, etc. Your Vite dev server proxies all of `/api` straight to Kotlin on `:8080`. If this Flask service also used `/api/market/...`, there'd be a routing collision — whichever proxy rule matches first wins, and the other service's endpoint becomes unreachable through the frontend.

Namespacing this service under `/api/sim/...` avoids that collision entirely, while still living under one shared `/api` mental model.

## Wiring it into your frontend (`vite.config.ts`)

Add a **second, more specific** proxy rule for `/api/sim`, placed so it's checked before the general `/api` rule:

```typescript
server: {
  port: 3000,
  proxy: {
    // More specific — must come before the general '/api' rule below
    '/api/sim': {
      target: 'http://localhost:5001',
      changeOrigin: true,
    },
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
    '/ws': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      ws: true,
    },
  },
},
```

Restart `npm run dev` after this change — proxy config is read once at server start, not hot-reloaded.

## Wiring it into `PriceChart.tsx`

If you want your `PriceChart.tsx` (the Chart.js component with moving averages) pointed at this simulator instead of/alongside real trade data, its data-fetching layer expects exactly this shape (`{ timestamp, price, ma_short, ma_long }`), matching what this service returns — that's not a coincidence, it's built to the same contract your earlier component already expected from `analyticsFetch('/api/market/ticks?...')`. Point that fetch at `/api/sim/market/ticks` instead and it should work with no other changes.
