import React, { useEffect, useMemo, useState } from 'react';
import { tradesApi } from '../api/trades';
import { marketApi } from '../api/market';
import type { TradeResponse, WsTradeEvent } from '../types';
 
interface Props {
  symbol: string;
  wsEvent?: WsTradeEvent | null;
}
 
interface Candle {
  time: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  isLive: boolean;
}
 
const WIDTH = 900;
const HEIGHT = 360;
 
const PADDING = {
  top: 18,
  right: 70,
  bottom: 42,
  left: 64,
};
 
const CHART_HEIGHT = 255;
const VOLUME_TOP = 286;
const CANDLE_INTERVAL_MS = 60_000;
const POLL_INTERVAL_MS = 3_000;
const MAX_TRADES_RETAINED = 200;
 
function makeCandles(trades: TradeResponse[]): Candle[] {
  const grouped = new Map<number, Omit<Candle, 'isLive'>>();
 
  trades.forEach((trade) => {
    const price = Number(trade.price);
    const volume = Number(trade.quantity);
    const executedAt = new Date(trade.executedAt).getTime();
 
    if (
      !Number.isFinite(price) ||
      !Number.isFinite(volume) ||
      !Number.isFinite(executedAt)
    ) {
      return;
    }
 
    const time =
      Math.floor(executedAt / CANDLE_INTERVAL_MS) * CANDLE_INTERVAL_MS;
 
    const existing = grouped.get(time);
 
    if (!existing) {
      grouped.set(time, {
        time,
        open: price,
        high: price,
        low: price,
        close: price,
        volume,
      });
 
      return;
    }
 
    existing.high = Math.max(existing.high, price);
    existing.low = Math.min(existing.low, price);
    existing.close = price;
    existing.volume += volume;
  });
 
  const nowBucket =
    Math.floor(Date.now() / CANDLE_INTERVAL_MS) * CANDLE_INTERVAL_MS;
 
  return [...grouped.values()]
    .sort((a, b) => a.time - b.time)
    .slice(-40)
    .map((candle) => ({ ...candle, isLive: candle.time === nowBucket }));
}
 
export function TradingChart({ symbol, wsEvent }: Props) {
  const [trades, setTrades] = useState<TradeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [latestPrice, setLatestPrice] = useState<number | null>(null);
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
 
  // Merge REST refresh into existing trades instead of clobbering them —
  // the WebSocket is the source of truth for "just happened" trades;
  // REST is only here to backfill history and catch anything missed
  // while the socket was reconnecting. Overwriting `trades` wholesale
  // on every poll caused live candles to flicker/reset every few seconds.
  const loadMarketData = () => {
    Promise.all([
      tradesApi.recentTrades(symbol),
      marketApi.latestPrice(symbol),
    ])
      .then(([recent, latest]) => {
        setTrades((previous) => {
          const byId = new Map(previous.map((t) => [t.id, t]));
          recent.forEach((t) => byId.set(t.id, t));
          return [...byId.values()]
            .sort(
              (a, b) =>
                new Date(a.executedAt).getTime() -
                new Date(b.executedAt).getTime(),
            )
            .slice(-MAX_TRADES_RETAINED);
        });
 
        setLatestPrice(
          latest.price == null ? null : Number(latest.price),
        );
      })
      .catch(() => {
        // Keep the last live state visible if refresh temporarily fails.
      })
      .finally(() => {
        setLoading(false);
      });
  };
 
  useEffect(() => {
    setLoading(true);
    setTrades([]);
    setLatestPrice(null);
    setHoveredIndex(null);
 
    loadMarketData();
 
    // Refresh trades created by other users or browser sessions.
    const refreshTimer = window.setInterval(loadMarketData, POLL_INTERVAL_MS);
 
    return () => {
      window.clearInterval(refreshTimer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [symbol]);
 
  useEffect(() => {
    if (!wsEvent || wsEvent.symbol !== symbol) {
      return;
    }
 
    const trade: TradeResponse = {
      id: wsEvent.id,
      symbol: wsEvent.symbol,
      buyOrderId: '',
      sellOrderId: '',
      price: wsEvent.price,
      quantity: wsEvent.quantity,
      total: wsEvent.total,
      executedAt: wsEvent.executedAt,
    };
 
    setTrades((previous) => {
      // Prevent duplicate trades when both REST and WebSocket
      // contain the same execution.
      if (previous.some((item) => item.id === trade.id)) {
        return previous;
      }
 
      return [...previous, trade].slice(-MAX_TRADES_RETAINED);
    });
 
    setLatestPrice(Number(wsEvent.price));
  }, [wsEvent, symbol]);
 
  const candles = useMemo(() => makeCandles(trades), [trades]);
 
  const chart = useMemo(() => {
    if (!candles.length) {
      return null;
    }
 
    const prices = candles.flatMap((candle) => [candle.high, candle.low]);
 
    const rawMin = Math.min(...prices);
    const rawMax = Math.max(...prices);
 
    const padding =
      (rawMax - rawMin || Math.max(rawMax * 0.002, 1)) * 0.12;
 
    const min = rawMin - padding;
    const max = rawMax + padding;
    const range = max - min || 1;
 
    const maxVolume = Math.max(...candles.map((candle) => candle.volume), 1);
 
    const chartWidth = WIDTH - PADDING.left - PADDING.right;
 
    const step = chartWidth / Math.max(candles.length, 1);
 
    const candleWidth = Math.max(4, Math.min(15, step * 0.62));
 
    const yPrice = (price: number) => {
      return (
        PADDING.top +
        ((max - price) / range) * (CHART_HEIGHT - PADDING.top)
      );
    };
 
    return {
      candles,
      min,
      max,
      maxVolume,
      step,
      candleWidth,
      yPrice,
      latest: candles[candles.length - 1].close,
    };
  }, [candles]);
 
  const formatPrice = (value: number | null) => {
    if (value == null || !Number.isFinite(value)) {
      return '—';
    }
 
    return `$${value.toLocaleString('en-US', {
      maximumFractionDigits: 2,
    })}`;
  };
 
  const formatTime = (timestamp: number) => {
    return new Date(timestamp).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  };
 
  const hoveredCandle =
    hoveredIndex != null && chart ? chart.candles[hoveredIndex] : null;
 
  return (
    <div className="panel min-h-[390px]">
      <div className="mb-2 flex items-center justify-between">
        <div>
          <div className="panel-title mb-0 border-0 pb-0">Price Chart</div>
 
          <div className="mt-1 text-[10px] text-gray-600">
            {symbol} · 1 minute candles
          </div>
        </div>
 
        <div className="flex items-center gap-2 text-xs font-semibold">
          <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-accent-green" />
 
          <span className="text-gray-300">
            {formatPrice(latestPrice ?? chart?.latest ?? null)}
          </span>
        </div>
      </div>
 
      {loading ? (
        <div className="flex h-[320px] items-center justify-center text-xs text-gray-500">
          Loading market data…
        </div>
      ) : chart ? (
        <div className="relative">
          <svg
            viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
            className="h-[320px] w-full"
            role="img"
            aria-label={`${symbol} live candlestick price chart`}
          >
            {/* Price grid */}
            {[0, 0.25, 0.5, 0.75, 1].map((fraction) => {
              const y =
                PADDING.top + fraction * (CHART_HEIGHT - PADDING.top);
 
              const price = chart.max - fraction * (chart.max - chart.min);
 
              return (
                <g key={fraction}>
                  <line
                    x1={PADDING.left}
                    x2={WIDTH - PADDING.right}
                    y1={y}
                    y2={y}
                    stroke="#252d3d"
                  />
 
                  <text
                    x={WIDTH - PADDING.right + 8}
                    y={y + 4}
                    fill="#6b7280"
                    fontSize="10"
                  >
                    {formatPrice(price)}
                  </text>
                </g>
              );
            })}
 
            {/* Volume separator */}
            <line
              x1={PADDING.left}
              x2={WIDTH - PADDING.right}
              y1={VOLUME_TOP - 8}
              y2={VOLUME_TOP - 8}
              stroke="#252d3d"
            />
 
            <text
              x={PADDING.left}
              y={VOLUME_TOP + 8}
              fill="#4b5563"
              fontSize="10"
            >
              VOLUME
            </text>
 
            {/* Candles and volume bars */}
            {chart.candles.map((candle, index) => {
              const x = PADDING.left + index * chart.step + chart.step / 2;
 
              const openY = chart.yPrice(candle.open);
              const closeY = chart.yPrice(candle.close);
              const highY = chart.yPrice(candle.high);
              const lowY = chart.yPrice(candle.low);
 
              const isUp = candle.close >= candle.open;
              const color = isUp ? '#00d68f' : '#ff4d6a';
 
              const bodyY = Math.min(openY, closeY);
 
              const bodyHeight = Math.max(2, Math.abs(openY - closeY));
 
              const volumeHeight = (candle.volume / chart.maxVolume) * 48;
 
              const isHovered = hoveredIndex === index;
 
              return (
                <g
                  key={candle.time}
                  onMouseEnter={() => setHoveredIndex(index)}
                  onMouseLeave={() =>
                    setHoveredIndex((current) =>
                      current === index ? null : current,
                    )
                  }
                >
                  {/* Wider invisible hit-area so hover is easy to trigger */}
                  <rect
                    x={x - chart.step / 2}
                    y={PADDING.top}
                    width={chart.step}
                    height={VOLUME_TOP + 50 - PADDING.top}
                    fill="transparent"
                  />
 
                  {isHovered && (
                    <line
                      x1={x}
                      x2={x}
                      y1={PADDING.top}
                      y2={VOLUME_TOP + 50}
                      stroke="#3b4254"
                      strokeDasharray="2 2"
                    />
                  )}
 
                  {/* Candle wick */}
                  <line
                    x1={x}
                    x2={x}
                    y1={highY}
                    y2={lowY}
                    stroke={color}
                    strokeWidth="1.5"
                  />
 
                  {/* Candle body — live/forming candle rendered softer + outlined */}
                  <rect
                    x={x - chart.candleWidth / 2}
                    y={bodyY}
                    width={chart.candleWidth}
                    height={bodyHeight}
                    fill={color}
                    fillOpacity={candle.isLive ? '0.55' : '0.9'}
                    stroke={candle.isLive ? color : 'none'}
                    strokeWidth={candle.isLive ? '1' : '0'}
                  />
 
                  {/* Volume bar */}
                  <rect
                    x={x - chart.candleWidth / 2}
                    y={VOLUME_TOP + 50 - volumeHeight}
                    width={chart.candleWidth}
                    height={volumeHeight}
                    fill={color}
                    fillOpacity={candle.isLive ? '0.2' : '0.3'}
                  />
 
                  {/* Time labels */}
                  {(index === 0 ||
                    index === chart.candles.length - 1 ||
                    index % 5 === 0) && (
                    <text
                      x={x}
                      y={HEIGHT - 10}
                      textAnchor="middle"
                      fill="#6b7280"
                      fontSize="9"
                    >
                      {formatTime(candle.time)}
                    </text>
                  )}
                </g>
              );
            })}
 
            {/* Latest price line */}
            <line
              x1={PADDING.left}
              x2={WIDTH - PADDING.right}
              y1={chart.yPrice(latestPrice ?? chart.latest)}
              y2={chart.yPrice(latestPrice ?? chart.latest)}
              stroke="#3b82f6"
              strokeDasharray="4 3"
              opacity="0.85"
            />
 
            {/* Latest price label */}
            <text
              x={WIDTH - PADDING.right + 8}
              y={chart.yPrice(latestPrice ?? chart.latest) + 4}
              fill="#60a5fa"
              fontSize="10"
            >
              {formatPrice(latestPrice ?? chart.latest)}
            </text>
          </svg>
 
          {/* OHLC hover tooltip */}
          {hoveredCandle && (
            <div className="pointer-events-none absolute left-2 top-2 rounded border border-[#252d3d] bg-[#0f1420]/95 px-2.5 py-1.5 text-[10px] leading-tight text-gray-300 shadow-lg">
              <div className="mb-1 font-semibold text-gray-200">
                {formatTime(hoveredCandle.time)}
                {hoveredCandle.isLive && (
                  <span className="ml-1 text-accent-green">● live</span>
                )}
              </div>
              <div className="grid grid-cols-2 gap-x-3 gap-y-0.5">
                <span className="text-gray-500">O</span>
                <span>{formatPrice(hoveredCandle.open)}</span>
                <span className="text-gray-500">H</span>
                <span>{formatPrice(hoveredCandle.high)}</span>
                <span className="text-gray-500">L</span>
                <span>{formatPrice(hoveredCandle.low)}</span>
                <span className="text-gray-500">C</span>
                <span>{formatPrice(hoveredCandle.close)}</span>
                <span className="text-gray-500">Vol</span>
                <span>{hoveredCandle.volume.toFixed(6)}</span>
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="flex h-[320px] items-center justify-center rounded bg-surface-2 text-center text-xs text-gray-500">
          <div>
            <div>No executed trades yet</div>
 
            <div className="mt-1 text-[10px] text-gray-600">
              Match a buy and sell order to start the live chart.
            </div>
          </div>
        </div>
      )}
    </div>
  );
}