import React, { useEffect, useState } from 'react';
import { marketApi } from '../api/market';
import type { MarketStatsResponse, WsMarketUpdateEvent } from '../types';

interface Props {
  symbol: string;
  wsUpdate?: WsMarketUpdateEvent | null;
}

export function MarketStats({ symbol, wsUpdate }: Props) {
  const [stats, setStats] = useState<MarketStatsResponse | null>(null);

  useEffect(() => {
    marketApi.stats(symbol).then(setStats).catch(console.error);
  }, [symbol]);

  // Merge live WS updates
  useEffect(() => {
    if (!wsUpdate) return;
    setStats((prev) =>
      prev
        ? {
            ...prev,
            latestPrice: wsUpdate.latestPrice,
            high24h: wsUpdate.high24h,
            low24h: wsUpdate.low24h,
            volume24h: wsUpdate.volume24h,
          }
        : null,
    );
  }, [wsUpdate]);

  const fmt = (val: string | null | undefined, dec = 2) => {
    if (!val) return '—';
    const n = parseFloat(val);
    if (isNaN(n)) return '—';
    return n.toLocaleString('en-US', { minimumFractionDigits: dec, maximumFractionDigits: dec });
  };

  const pctColor =
    stats?.priceChangePct24h == null
      ? 'text-gray-400'
      : parseFloat(stats.priceChangePct24h) >= 0
      ? 'text-accent-green'
      : 'text-accent-red';

  return (
    <div className="panel">
      <div className="flex items-baseline gap-3 flex-wrap">
        <span className="text-xs font-bold text-gray-400">{symbol}</span>
        <span className="text-2xl font-bold text-gray-100">
          ${fmt(stats?.latestPrice)}
        </span>
        {stats?.priceChangePct24h != null && (
          <span className={`text-sm font-semibold ${pctColor}`}>
            {parseFloat(stats.priceChangePct24h) >= 0 ? '+' : ''}
            {fmt(stats.priceChangePct24h)}%
          </span>
        )}
        <div className="flex gap-6 ml-auto text-xs text-gray-400">
          <div>
            <div className="text-gray-500">24h High</div>
            <div className="text-accent-green">${fmt(stats?.high24h)}</div>
          </div>
          <div>
            <div className="text-gray-500">24h Low</div>
            <div className="text-accent-red">${fmt(stats?.low24h)}</div>
          </div>
          <div>
            <div className="text-gray-500">24h Volume</div>
            <div className="text-gray-300">{fmt(stats?.volume24h, 4)}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
