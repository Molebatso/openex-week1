"""
OpenEx market simulator — Flask microservice.

Continuously generates a simulated price feed per symbol (random walk
with drift) on a background thread, independent of any real trading
activity in the Kotlin backend. Exposes REST endpoints for the frontend
to poll.

Routes are namespaced under /api/sim/... specifically so they don't
collide with the Kotlin backend's /api/... routes when both are proxied
through the same Vite dev server — see vite.config.ts notes in the
accompanying README.
"""

import os

from flask import Flask, jsonify, request
from flask_cors import CORS

from simulator import MarketSimulatorRegistry

TICK_INTERVAL_SECONDS = float(os.environ.get("TICK_INTERVAL_SECONDS", "2"))
PORT = int(os.environ.get("PORT", "5001"))
ALLOWED_ORIGINS = os.environ.get("ALLOWED_ORIGINS", "http://localhost:3000").split(",")

app = Flask(__name__)
CORS(app, resources={r"/api/*": {"origins": ALLOWED_ORIGINS}})

registry = MarketSimulatorRegistry(tick_interval_seconds=TICK_INTERVAL_SECONDS)
registry.start()


@app.get("/health")
def health():
    return jsonify({"status": "ok", "symbols": registry.symbols()})


@app.get("/api/sim/market/symbols")
def symbols():
    return jsonify({"symbols": registry.symbols()})


@app.get("/api/sim/market/ticks")
def ticks():
    """
    GET /api/sim/market/ticks?symbol=BTC/USD&limit=60

    Returns clean JSON arrays of historical + current simulated ticks,
    each with a price and short/long moving averages.
    """
    symbol = request.args.get("symbol", "BTC/USD")
    limit = request.args.get("limit", default=60, type=int)
    limit = max(1, min(limit, 500))

    sim = registry.get(symbol)
    if sim is None:
        return (
            jsonify({"error": f"Unknown symbol: {symbol}", "available": registry.symbols()}),
            404,
        )

    return jsonify(
        {
            "symbol": symbol,
            "ticks": [t.__dict__ for t in sim.snapshot(limit=limit)],
        }
    )


@app.get("/api/sim/market/latest")
def latest():
    symbol = request.args.get("symbol", "BTC/USD")
    sim = registry.get(symbol)
    if sim is None:
        return jsonify({"error": f"Unknown symbol: {symbol}"}), 404
    return jsonify({"symbol": symbol, "price": sim.latest_price()})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=PORT, debug=False)
