import React, { useEffect, useMemo, useState } from 'react';
import { marketApi } from '../api/market';
import { tradesApi } from '../api/trades';
import type { TradeResponse, WsTradeEvent } from '../types';

interface Props {
  symbol: string;
  wsEvent?: WsTradeEvent | null;
}

const WIDTH = 760;
const HEIGHT = 220;
const PADDING = { top: 18, right: 12, bottom: 24, left: 58 };

export function TradingChart({ symbol, wsEvent }: Props) {
  const [trades, setTrades] = useState<TradeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [latestPrice, setLatestPrice] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      tradesApi.recentTrades(symbol),
      marketApi.latestPrice(symbol),
    ]).then(([recent, latest]) => {
      setTrades([...recent].reverse());
      setLatestPrice(latest.price);
    }).catch(() => {
      setTrades([]);
      setLatestPrice(null);
    }).finally(() => setLoading(false));
  }, [symbol]);

  useEffect(() => {
    if (!wsEvent || wsEvent.symbol !== symbol) return;
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
    setTrades((previous) => [...previous, trade].slice(-50));
    setLatestPrice(wsEvent.price);
  }, [wsEvent, symbol]);

  const points = useMemo(() => {
    const values = trades.map((trade) => Number(trade.price)).filter(Number.isFinite);
    if (values.length === 0) return null;
    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = max - min || Math.max(max * 0.001, 1);
    const chartWidth = WIDTH - PADDING.left - PADDING.right;
    const chartHeight = HEIGHT - PADDING.top - PADDING.bottom;
    return {
      values,
      min,
      max,
      path: values.map((value, index) => {
        const x = PADDING.left + (index / Math.max(values.length - 1, 1)) * chartWidth;
        const y = PADDING.top + ((max - value) / range) * chartHeight;
        return `${index === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`;
      }).join(' '),
      area: `${PADDING.left},${HEIGHT - PADDING.bottom} ${values.map((value, index) => {
        const x = PADDING.left + (index / Math.max(values.length - 1, 1)) * chartWidth;
        const y = PADDING.top + ((max - value) / range) * chartHeight;
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      }).join(' ')} ${WIDTH - PADDING.right},${HEIGHT - PADDING.bottom}`,
    };
  }, [trades]);

  const formatPrice = (value: number | null) =>
    value == null || !Number.isFinite(value) ? '—' : `$${value.toLocaleString('en-US', { maximumFractionDigits: 2 })}`;

  return (
    <div className="panel min-h-[250px]">
      <div className="mb-2 flex items-center justify-between">
        <div className="panel-title mb-0 border-0 pb-0">Live Price Chart</div>
        <div className="text-xs font-semibold text-gray-300">
          {latestPrice == null ? '—' : formatPrice(Number(latestPrice))}
        </div>
      </div>
      {loading ? (
        <div className="flex h-[210px] items-center justify-center text-xs text-gray-500">Loading chart…</div>
      ) : points ? (
        <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} className="h-[210px] w-full" role="img" aria-label={`${symbol} live price chart`}>
          {[0, 0.5, 1].map((fraction) => {
            const y = PADDING.top + fraction * (HEIGHT - PADDING.top - PADDING.bottom);
            const value = points.max - fraction * (points.max - points.min);
            return (
              <g key={fraction}>
                <line x1={PADDING.left} x2={WIDTH - PADDING.right} y1={y} y2={y} stroke="#252d3d" />
                <text x={PADDING.left - 8} y={y + 4} textAnchor="end" fill="#6b7280" fontSize="10">{formatPrice(value)}</text>
              </g>
            );
          })}
          <polygon points={points.area} fill="url(#chartFill)" />
          <path d={points.path} fill="none" stroke="#00d68f" strokeWidth="2" vectorEffect="non-scaling-stroke" />
          <defs>
            <linearGradient id="chartFill" x1="0" x2="0" y1="0" y2="1">
              <stop offset="0%" stopColor="#00d68f" stopOpacity="0.22" />
              <stop offset="100%" stopColor="#00d68f" stopOpacity="0" />
            </linearGradient>
          </defs>
        </svg>
      ) : (
        <div className="flex h-[210px] items-center justify-center rounded bg-surface-2 text-xs text-gray-500">
          No trades yet — place matching orders to start the live chart.
        </div>
      )}
    </div>
  );
}