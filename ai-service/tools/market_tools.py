"""
Read-only LangChain tools that call the OpenEx backend REST API.

The AI assistant uses these tools to answer portfolio questions.
It can NEVER place orders, modify balances, or delete records.
"""

from typing import Optional
import requests
from langchain_core.tools import tool

# Injected at startup from config
_backend_url: str = "http://localhost:8080"
_token: str = ""


def configure(backend_url: str, token: str) -> None:
    """Called per-request to set the backend URL and user JWT."""
    global _backend_url, _token
    _backend_url = backend_url
    _token = token


def _headers() -> dict:
    return {"Authorization": f"Bearer {_token}"}


def _get(path: str, params: Optional[dict] = None) -> dict | list:
    url = f"{_backend_url}{path}"
    resp = requests.get(url, headers=_headers(), params=params, timeout=10)
    resp.raise_for_status()
    return resp.json()


@tool
def get_wallet_balances(_: str = "") -> str:
    """
    Retrieve the user's current wallet balances for all currencies.
    Use this when asked about balance, funds, portfolio value, or assets.
    Input: any string (ignored).
    """
    try:
        wallets = _get("/api/wallet")
        if not wallets:
            return "No wallets found."
        lines = [f"  {w['currency']}: {float(w['balance']):.8f}" for w in wallets]
        return "Wallet balances:\n" + "\n".join(lines)
    except requests.HTTPError as e:
        return f"Error fetching wallets: {e.response.status_code} {e.response.text}"
    except Exception as e:
        return f"Error fetching wallets: {e}"


@tool
def get_recent_trades(limit: str = "5") -> str:
    """
    Retrieve the user's most recent trade history.
    Use this when asked about trades, executions, or transaction history.
    Input: number of trades to return (default 5, max 50).
    """
    try:
        n = min(int(limit), 50) if limit.isdigit() else 5
        trades = _get("/api/trades")
        trades = trades[:n]
        if not trades:
            return "No trades found."
        lines = []
        for t in trades:
            price = float(t["price"])
            qty = float(t["quantity"])
            total = float(t["total"])
            ts = t["executedAt"][:19].replace("T", " ")
            lines.append(f"  {ts} — {t['symbol']} {qty:.6f} @ ${price:,.2f} (total ${total:,.2f})")
        return f"Last {len(lines)} trades:\n" + "\n".join(lines)
    except Exception as e:
        return f"Error fetching trades: {e}"


@tool
def get_open_orders(_: str = "") -> str:
    """
    Retrieve all active open orders for the user.
    Use this when asked about open orders, pending orders, or order status.
    Input: any string (ignored).
    """
    try:
        orders = _get("/api/orders")
        open_orders = [o for o in orders if o["status"] in ("OPEN", "PARTIAL")]
        if not open_orders:
            return "No open orders."
        lines = []
        for o in open_orders:
            price_str = f"@ ${float(o['price']):,.2f}" if o.get("price") else "(MARKET)"
            filled = float(o["filledQuantity"])
            qty = float(o["quantity"])
            lines.append(
                f"  {o['side']} {o['type']} {o['symbol']} {qty:.6f} {price_str} "
                f"[{o['status']}, filled {filled:.6f}]"
            )
        return f"Open orders ({len(open_orders)}):\n" + "\n".join(lines)
    except Exception as e:
        return f"Error fetching orders: {e}"


@tool
def get_market_stats(symbol: str = "BTC/USD") -> str:
    """
    Retrieve 24-hour market statistics for a trading symbol.
    Use this when asked about price, market data, highs, lows, or volume.
    Input: symbol like 'BTC/USD' or 'ETH/USD'.
    """
    try:
        sym = symbol.upper().strip() or "BTC/USD"
        stats = _get("/api/market/stats", params={"symbol": sym})
        latest = stats.get("latestPrice")
        high = stats.get("high24h")
        low = stats.get("low24h")
        vol = float(stats.get("volume24h", 0))
        pct = stats.get("priceChangePct24h")

        lines = [f"Market stats for {sym}:"]
        lines.append(f"  Latest price: ${float(latest):,.2f}" if latest else "  Latest price: N/A")
        lines.append(f"  24h High: ${float(high):,.2f}" if high else "  24h High: N/A")
        lines.append(f"  24h Low:  ${float(low):,.2f}" if low else "  24h Low:  N/A")
        lines.append(f"  24h Volume: {vol:.6f}")
        if pct:
            lines.append(f"  24h Change: {float(pct):+.2f}%")
        return "\n".join(lines)
    except Exception as e:
        return f"Error fetching market stats: {e}"


# All tools available to the agent
ALL_TOOLS = [get_wallet_balances, get_recent_trades, get_open_orders, get_market_stats]
