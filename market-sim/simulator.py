"""
Market simulator core.

Each symbol gets its own SymbolSimulator holding an in-memory price
history. A single background thread (owned by MarketSimulatorRegistry)
advances every symbol's price on a fixed interval — this is what makes
the chart move continuously, whether or not any real trade happens
anywhere else in the system.

Price model: a simple random walk with drift —
    next_price = price * (1 + drift + noise)
where noise ~ Normal(0, volatility). This is the same "random walk with
drift" model called for in the original capstone brief (Day 11).
"""

import threading
import time
from collections import deque
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Deque, Dict, List, Optional

import numpy as np
import pandas as pd


@dataclass
class Tick:
    timestamp: str
    price: float
    ma_short: Optional[float]
    ma_long: Optional[float]


class SymbolSimulator:
    """Simulates a continuously moving price for a single symbol."""

    def __init__(
        self,
        symbol: str,
        start_price: float,
        drift: float = 0.00002,
        volatility: float = 0.0015,
        history_size: int = 500,
        ma_short: int = 5,
        ma_long: int = 20,
    ):
        self.symbol = symbol
        self.drift = drift
        self.volatility = volatility
        self.ma_short = ma_short
        self.ma_long = ma_long
        self.history_size = history_size

        self._lock = threading.Lock()
        self._prices: Deque[float] = deque(maxlen=history_size)
        self._timestamps: Deque[str] = deque(maxlen=history_size)

        # Backfill synthetic history so the chart isn't empty on first load.
        self._backfill(start_price, count=100)

    def _step(self, price: float) -> float:
        shock = np.random.normal(loc=self.drift, scale=self.volatility)
        return max(price * (1 + shock), 0.01)

    def _backfill(self, start_price: float, count: int):
        price = start_price
        prices = []
        for _ in range(count):
            price = self._step(price)
            prices.append(price)

        now_ts = datetime.now(timezone.utc).timestamp()
        for i, p in enumerate(prices):
            ts = now_ts - (count - i)  # spaced 1s apart, ending at "now"
            self._prices.append(p)
            self._timestamps.append(
                datetime.fromtimestamp(ts, tz=timezone.utc).isoformat()
            )

    def tick(self):
        """Advance the simulation by one step. Called by the background loop."""
        with self._lock:
            last_price = self._prices[-1] if self._prices else 100.0
            next_price = self._step(last_price)
            self._prices.append(next_price)
            self._timestamps.append(datetime.now(timezone.utc).isoformat())

    def snapshot(self, limit: int = 60) -> List[Tick]:
        with self._lock:
            prices = list(self._prices)[-limit:]
            timestamps = list(self._timestamps)[-limit:]

        if not prices:
            return []

        series = pd.Series(prices)
        ma_short_series = series.rolling(window=self.ma_short, min_periods=self.ma_short).mean()
        ma_long_series = series.rolling(window=self.ma_long, min_periods=self.ma_long).mean()

        ticks = []
        for i, (ts, price) in enumerate(zip(timestamps, prices)):
            ticks.append(
                Tick(
                    timestamp=ts,
                    price=round(float(price), 2),
                    ma_short=(
                        round(float(ma_short_series[i]), 2)
                        if not pd.isna(ma_short_series[i])
                        else None
                    ),
                    ma_long=(
                        round(float(ma_long_series[i]), 2)
                        if not pd.isna(ma_long_series[i])
                        else None
                    ),
                )
            )
        return ticks

    def latest_price(self) -> Optional[float]:
        with self._lock:
            return round(float(self._prices[-1]), 2) if self._prices else None


class MarketSimulatorRegistry:
    """Owns one SymbolSimulator per symbol plus the background thread
    that ticks all of them on a fixed interval."""

    DEFAULT_SYMBOLS: Dict[str, float] = {
        "BTC/USD": 97_200.00,
        "ETH/USD": 3_450.00,
        "SOL/USD": 152.00,
    }

    def __init__(self, tick_interval_seconds: float = 2.0):
        self.tick_interval_seconds = tick_interval_seconds
        self.simulators: Dict[str, SymbolSimulator] = {
            symbol: SymbolSimulator(symbol, start_price)
            for symbol, start_price in self.DEFAULT_SYMBOLS.items()
        }
        self._stop_event = threading.Event()
        self._thread: Optional[threading.Thread] = None

    def get(self, symbol: str) -> Optional[SymbolSimulator]:
        return self.simulators.get(symbol)

    def symbols(self) -> List[str]:
        return list(self.simulators.keys())

    def start(self):
        if self._thread and self._thread.is_alive():
            return
        self._stop_event.clear()
        self._thread = threading.Thread(target=self._run_loop, daemon=True)
        self._thread.start()

    def stop(self):
        self._stop_event.set()

    def _run_loop(self):
        while not self._stop_event.is_set():
            for sim in self.simulators.values():
                sim.tick()
            time.sleep(self.tick_interval_seconds)
