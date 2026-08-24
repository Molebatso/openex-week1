import React, { useEffect, useMemo, useRef, useState } from 'react';
import type { WsTradeEvent } from '../types';
 
interface Props {
  symbol: string;
  /** Kept for drop-in compatibility with the parent component's props —
   *  not used here, since this chart now renders the simulated feed
   *  instead of real trade events. */
  wsEvent?: WsTradeEvent | null;
}
 
interface SimTick {
  timestamp: string;
  price: number;
  ma_short: number | null;
  ma_long: number | null;
}
 
interface Candle {
  time: number;
  open: number;
  high: number;
  low: number;
  close: number;
  /** Synthetic — derived from price movement, NOT real trade volume.
   *  The simulator has no concept of volume; this exists only so the
   *  volume/OBV panes still have something meaningful to plot. */
  volume: number;
  isLive: boolean;
}
 
const WIDTH = 960;
const HEIGHT = 460;
 
const TOOLBAR_H = 34;
const PRICE_TOP = TOOLBAR_H + 14;
const PRICE_BOTTOM = 300;
const VOL_TOP = 312;
const VOL_BOTTOM = 372;
const IND_TOP = 384;
const IND_BOTTOM = 434;
const AXIS_RIGHT = 68;
const AXIS_LEFT = 8;
const TIME_LABEL_Y = HEIGHT - 8;
 
// The Flask simulator ticks every 2s by default (TICK_INTERVAL_SECONDS),
// so polling on the same cadence keeps this chart moving continuously,
// independent of whether any real order ever matches.
const POLL_INTERVAL_MS = 2_000;
 
// The simulator's in-memory history is capped (history_size=500 by
// default) — request as much as it can give us so candle bucketing has
// enough raw points to work with, especially for longer timeframes.
const SIM_FETCH_LIMIT = 500;
 
interface TimeframeConfig {
  label: string;
  bucketMs: number;
  maxCandles: number;
}
 
// NOTE: the simulator only keeps a shallow rolling history (a few
// minutes' worth of ticks at the default 2s interval). Longer
// timeframes (3M/6M/YTD/1Y/5Y) will visually collapse into very few
// candles until the service has been running a long time — that's a
// property of the simulator's history depth, not a bug here.
const TIMEFRAMES: TimeframeConfig[] = [
  { label: '1D', bucketMs: 10_000, maxCandles: 90 },
  { label: '5D', bucketMs: 30_000, maxCandles: 90 },
  { label: '1M', bucketMs: 60_000, maxCandles: 90 },
  { label: '3M', bucketMs: 5 * 60_000, maxCandles: 90 },
  { label: '6M', bucketMs: 15 * 60_000, maxCandles: 90 },
  { label: 'YTD', bucketMs: 30 * 60_000, maxCandles: 90 },
  { label: '1Y', bucketMs: 60 * 60_000, maxCandles: 90 },
  { label: '5Y', bucketMs: 4 * 60 * 60_000, maxCandles: 90 },
];
 
async function fetchSimTicks(symbol: string, limit: number): Promise<SimTick[]> {
  const res = await fetch(
    `/api/sim/market/ticks?symbol=${encodeURIComponent(symbol)}&limit=${limit}`,
  );
  if (!res.ok) {
    throw new Error(`Simulator request failed: ${res.status}`);
  }
  const data = await res.json();
  return data.ticks as SimTick[];
}
 
function buildCandlesFromTicks(
  ticks: SimTick[],
  bucketMs: number,
  maxCandles: number,
): Candle[] {
  const grouped = new Map<number, Omit<Candle, 'isLive'> & { lastPrice: number }>();
 
  ticks.forEach((tick) => {
    const price = Number(tick.price);
    const executedAt = new Date(tick.timestamp).getTime();
    if (!Number.isFinite(price) || !Number.isFinite(executedAt)) return;
 
    const time = Math.floor(executedAt / bucketMs) * bucketMs;
    const existing = grouped.get(time);
 
    if (!existing) {
      grouped.set(time, {
        time,
        open: price,
        high: price,
        low: price,
        close: price,
        volume: 0,
        lastPrice: price,
      });
      return;
    }
 
    existing.high = Math.max(existing.high, price);
    existing.low = Math.min(existing.low, price);
    // Synthetic volume = cumulative absolute price movement within the bucket.
    existing.volume += Math.abs(price - existing.lastPrice);
    existing.lastPrice = price;
    existing.close = price;
  });
 
  const nowBucket = Math.floor(Date.now() / bucketMs) * bucketMs;
 
  return [...grouped.values()]
    .sort((a, b) => a.time - b.time)
    .slice(-maxCandles)
    .map(({ lastPrice, ...c }) => ({ ...c, isLive: c.time === nowBucket }));
}
 
/** On-Balance Volume computed from the synthetic volume series above. */
function computeOBV(candles: Candle[]): number[] {
  const obv: number[] = [];
  let running = 0;
  candles.forEach((c, i) => {
    if (i === 0) {
      obv.push(0);
      return;
    }
    const prevClose = candles[i - 1].close;
    if (c.close > prevClose) running += c.volume;
    else if (c.close < prevClose) running -= c.volume;
    obv.push(running);
  });
  return obv;
}
 
function ToolbarButton({ children }: { children: React.ReactNode }) {
  return (
    <button
      type="button"
      className="rounded px-2 py-1 text-[10px] font-medium text-gray-400 transition-colors hover:bg-white/5 hover:text-gray-200"
    >
      {children}
    </button>
  );
}
 
export function TradingChart({ symbol }: Props) {
  const [ticks, setTicks] = useState<SimTick[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [timeframeIndex, setTimeframeIndex] = useState(0);
  const [hover, setHover] = useState<number | null>(null);
  const svgRef = useRef<SVGSVGElement | null>(null);
 
  const timeframe = TIMEFRAMES[timeframeIndex];
 
  const loadSimData = () => {
    fetchSimTicks(symbol, SIM_FETCH_LIMIT)
      .then((data) => {
        setTicks(data);
        setError(null);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'Failed to load simulated market data');
      })
      .finally(() => setLoading(false));
  };
 
  useEffect(() => {
    setLoading(true);
    setTicks([]);
    setHover(null);
    setError(null);
 
    loadSimData();
 
    const refreshTimer = window.setInterval(loadSimData, POLL_INTERVAL_MS);
    return () => window.clearInterval(refreshTimer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [symbol]);
 
  const candles = useMemo(
    () => buildCandlesFromTicks(ticks, timeframe.bucketMs, timeframe.maxCandles),
    [ticks, timeframe],
  );
  const obv = useMemo(() => computeOBV(candles), [candles]);
 
  const chart = useMemo(() => {
    if (!candles.length) return null;
 
    const highs = candles.map((c) => c.high);
    const lows = candles.map((c) => c.low);
    const rawMin = Math.min(...lows);
    const rawMax = Math.max(...highs);
    const pad = (rawMax - rawMin || Math.max(rawMax * 0.004, 1)) * 0.1;
    const min = rawMin - pad;
    const max = rawMax + pad;
    const range = max - min || 1;
 
    const maxVolume = Math.max(...candles.map((c) => c.volume), 0.0001);
    const obvMin = Math.min(...obv, 0);
    const obvMax = Math.max(...obv, 1);
    const obvRange = obvMax - obvMin || 1;
 
    const innerWidth = WIDTH - AXIS_LEFT - AXIS_RIGHT;
    const step = innerWidth / Math.max(candles.length, 1);
    const bodyWidth = Math.max(2, Math.min(12, step * 0.6));
 
    const xAt = (i: number) => AXIS_LEFT + i * step + step / 2;
    const yPrice = (p: number) =>
      PRICE_TOP + ((max - p) / range) * (PRICE_BOTTOM - PRICE_TOP);
    const yObv = (v: number) =>
      IND_TOP + ((obvMax - v) / obvRange) * (IND_BOTTOM - IND_TOP);
 
    const obvPath = obv
      .map((v, i) => `${i === 0 ? 'M' : 'L'} ${xAt(i).toFixed(2)} ${yObv(v).toFixed(2)}`)
      .join(' ');
 
    return {
      candles,
      obv,
      min,
      max,
      maxVolume,
      step,
      bodyWidth,
      xAt,
      yPrice,
      yObv,
      obvPath,
      latest: candles[candles.length - 1].close,
    };
  }, [candles, obv]);
 
  const formatPrice = (v: number) =>
    v.toLocaleString('en-US', { maximumFractionDigits: 2 });
 
  const formatTime = (t: number) => {
    const d = new Date(t);
    if (timeframe.bucketMs >= 24 * 60 * 60_000) {
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    }
    return d.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      second: timeframe.bucketMs < 60_000 ? '2-digit' : undefined,
      hour12: false,
    });
  };
 
  const handleMouseMove: React.MouseEventHandler<SVGSVGElement> = (e) => {
    if (!chart || !svgRef.current) return;
    const rect = svgRef.current.getBoundingClientRect();
    const svgX = (e.clientX - rect.left) * (WIDTH / rect.width);
    let nearest = 0;
    let best = Infinity;
    chart.candles.forEach((_, i) => {
      const d = Math.abs(chart.xAt(i) - svgX);
      if (d < best) {
        best = d;
        nearest = i;
      }
    });
    setHover(nearest);
  };
 
  const hovered = hover != null && chart ? chart.candles[hover] : null;
  const last = chart?.candles[chart.candles.length - 1];
  const prevClose =
    chart && chart.candles.length > 1
      ? chart.candles[chart.candles.length - 2].close
      : last?.open;
  const change = last && prevClose != null ? last.close - prevClose : 0;
  const changePct = prevClose ? (change / prevClose) * 100 : 0;
  const isUp = change >= 0;
 
  return (
    <div className="panel min-h-[480px] overflow-hidden p-0">
      {/* Toolbar */}
      <div className="flex items-center justify-between border-b border-[#1c2331] px-3 py-1.5">
        <div className="flex items-center gap-3">
          <span className="text-xs font-bold text-white">{symbol}</span>
          <span className="flex items-center gap-1 rounded bg-blue-500/15 px-1.5 py-0.5 text-[9px] font-semibold uppercase tracking-wide text-blue-400">
            <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-blue-400" />
            Simulated
          </span>
          <span
            className={`text-[10px] font-semibold ${
              isUp ? 'text-accent-green' : 'text-accent-red'
            }`}
          >
            {formatPrice(last?.close ?? 0)} ({isUp ? '+' : ''}
            {changePct.toFixed(2)}%)
          </span>
        </div>
        <div className="flex items-center gap-1">
          <ToolbarButton>Compare</ToolbarButton>
          <ToolbarButton>Indicators</ToolbarButton>
          <ToolbarButton>Templates</ToolbarButton>
          <ToolbarButton>Fullscreen</ToolbarButton>
          <ToolbarButton>AutoSave</ToolbarButton>
        </div>
      </div>
 
      {loading ? (
        <div className="flex h-[420px] items-center justify-center text-xs text-gray-500">
          Loading simulated market data…
        </div>
      ) : error ? (
        <div className="flex h-[420px] items-center justify-center text-center text-xs text-accent-red">
          <div>
            <div>{error}</div>
            <div className="mt-1 text-[10px] text-gray-500">
              Check that the market simulator is running on :5001 and the
              /api/sim proxy rule is set in vite.config.ts.
            </div>
          </div>
        </div>
      ) : chart ? (
        <div className="relative">
          <svg
            ref={svgRef}
            viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
            className="h-[420px] w-full cursor-crosshair"
            onMouseMove={handleMouseMove}
            onMouseLeave={() => setHover(null)}
          >
            {/* Price grid */}
            {[0, 0.25, 0.5, 0.75, 1].map((f) => {
              const y = PRICE_TOP + f * (PRICE_BOTTOM - PRICE_TOP);
              const price = chart.max - f * (chart.max - chart.min);
              return (
                <g key={f}>
                  <line x1={AXIS_LEFT} x2={WIDTH - AXIS_RIGHT} y1={y} y2={y} stroke="#1a2030" />
                  <text x={WIDTH - AXIS_RIGHT + 6} y={y + 3} fill="#5b6474" fontSize="9">
                    {formatPrice(price)}
                  </text>
                </g>
              );
            })}
 
            {/* Candles */}
            {chart.candles.map((c, i) => {
              const x = chart.xAt(i);
              const up = c.close >= c.open;
              const color = up ? '#00d68f' : '#ff4d5e';
              const openY = chart.yPrice(c.open);
              const closeY = chart.yPrice(c.close);
              const highY = chart.yPrice(c.high);
              const lowY = chart.yPrice(c.low);
              const bodyY = Math.min(openY, closeY);
              const bodyH = Math.max(1.5, Math.abs(openY - closeY));
              return (
                <g key={c.time}>
                  <line x1={x} x2={x} y1={highY} y2={lowY} stroke={color} strokeWidth="1" />
                  <rect
                    x={x - chart.bodyWidth / 2}
                    y={bodyY}
                    width={chart.bodyWidth}
                    height={bodyH}
                    fill={color}
                    fillOpacity={c.isLive ? 0.55 : 0.95}
                    stroke={c.isLive ? color : 'none'}
                  />
                </g>
              );
            })}
 
            {/* Volume label + bars (synthetic — derived from price movement) */}
            <text x={AXIS_LEFT} y={VOL_TOP - 4} fill="#4b5563" fontSize="9">
              Volume (simulated)
            </text>
            {chart.candles.map((c, i) => {
              const x = chart.xAt(i);
              const up = c.close >= c.open;
              const h = (c.volume / chart.maxVolume) * (VOL_BOTTOM - VOL_TOP);
              return (
                <rect
                  key={`v-${c.time}`}
                  x={x - chart.bodyWidth / 2}
                  y={VOL_BOTTOM - h}
                  width={chart.bodyWidth}
                  height={Math.max(h, 1)}
                  fill={up ? '#00d68f' : '#ff4d5e'}
                  fillOpacity={c.isLive ? 0.3 : 0.45}
                />
              );
            })}
 
            {/* OBV indicator pane (computed on synthetic volume) */}
            <text x={AXIS_LEFT} y={IND_TOP - 4} fill="#4b5563" fontSize="9">
              OBV (simulated) {chart.obv[chart.obv.length - 1]?.toFixed(4)}
            </text>
            <line
              x1={AXIS_LEFT}
              x2={WIDTH - AXIS_RIGHT}
              y1={(IND_TOP + IND_BOTTOM) / 2}
              y2={(IND_TOP + IND_BOTTOM) / 2}
              stroke="#1a2030"
            />
            <path d={chart.obvPath} fill="none" stroke="#3b82f6" strokeWidth="1.5" />
 
            {/* Crosshair */}
            {hover != null && chart.candles[hover] && (
              <g>
                <line
                  x1={chart.xAt(hover)}
                  x2={chart.xAt(hover)}
                  y1={PRICE_TOP}
                  y2={IND_BOTTOM}
                  stroke="#4b5568"
                  strokeDasharray="3 3"
                />
                <text
                  x={chart.xAt(hover)}
                  y={TIME_LABEL_Y}
                  textAnchor="middle"
                  fill="#9ca3af"
                  fontSize="9"
                >
                  {formatTime(chart.candles[hover].time)}
                </text>
              </g>
            )}
 
            {/* Time axis (sparse, only when not hovering) */}
            {hover == null &&
              chart.candles.map((c, i) => {
                if (i !== 0 && i !== chart.candles.length - 1 && i % 10 !== 0) return null;
                return (
                  <text
                    key={`t-${c.time}`}
                    x={chart.xAt(i)}
                    y={TIME_LABEL_Y}
                    textAnchor="middle"
                    fill="#5b6474"
                    fontSize="9"
                  >
                    {formatTime(c.time)}
                  </text>
                );
              })}
          </svg>
 
          {/* OHLC tooltip */}
          {hovered && (
            <div className="pointer-events-none absolute left-2 top-2 rounded border border-[#252d3d] bg-[#0f1420]/95 px-2.5 py-1.5 text-[10px] leading-tight text-gray-300 shadow-lg">
              <div className="mb-1 font-semibold text-gray-200">
                {formatTime(hovered.time)}
                {hovered.isLive && <span className="ml-1 text-blue-400">● live sim</span>}
              </div>
              <div className="grid grid-cols-2 gap-x-3 gap-y-0.5">
                <span className="text-gray-500">O</span>
                <span>{formatPrice(hovered.open)}</span>
                <span className="text-gray-500">H</span>
                <span>{formatPrice(hovered.high)}</span>
                <span className="text-gray-500">L</span>
                <span>{formatPrice(hovered.low)}</span>
                <span className="text-gray-500">C</span>
                <span>{formatPrice(hovered.close)}</span>
                <span className="text-gray-500">Vol (sim)</span>
                <span>{hovered.volume.toFixed(4)}</span>
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="flex h-[420px] items-center justify-center text-center text-xs text-gray-500">
          <div>Waiting for simulated ticks…</div>
        </div>
      )}
 
      {/* Timeframe tabs */}
      <div className="flex items-center gap-1 border-t border-[#1c2331] px-3 py-1.5">
        {TIMEFRAMES.map((tf, i) => (
          <button
            key={tf.label}
            onClick={() => setTimeframeIndex(i)}
            className={`rounded px-2 py-1 text-[10px] font-semibold transition-colors ${
              i === timeframeIndex
                ? 'bg-white/10 text-white'
                : 'text-gray-500 hover:text-gray-300'
            }`}
          >
            {tf.label}
          </button>
        ))}
      </div>
    </div>
  );
}
 
export default TradingChart;