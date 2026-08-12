import React, { useEffect, useState, useCallback } from 'react';
import { tradesApi } from '../api/trades';
import type { TradeResponse, WsTradeEvent } from '../types';

interface Props {
  symbol: string;
  wsEvent?: WsTradeEvent | null;
}

export function TradeHistory({ symbol, wsEvent }: Props) {
  const [trades, setTrades] = useState<TradeResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const loadTrades = useCallback(() => {
    tradesApi
      .recentTrades(symbol)
      .then(setTrades)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [symbol]);

  useEffect(() => {
    loadTrades();
  }, [loadTrades]);

  // Prepend live trade from WebSocket
  useEffect(() => {
    if (!wsEvent) return;
    const newTrade: TradeResponse = {
      id: wsEvent.id,
      symbol: wsEvent.symbol,
      buyOrderId: '',
      sellOrderId: '',
      price: wsEvent.price,
      quantity: wsEvent.quantity,
      total: wsEvent.total,
      executedAt: wsEvent.executedAt,
    };
    setTrades((prev) => [newTrade, ...prev.slice(0, 49)]);
  }, [wsEvent]);

  const fmt = (val: string, dec = 2) => {
    const n = parseFloat(val);
    return isNaN(n) ? val : n.toLocaleString('en-US', { minimumFractionDigits: dec, maximumFractionDigits: dec });
  };

  const fmtTime = (iso: string) => {
    return new Date(iso).toLocaleTimeString('en-US', { hour12: false });
  };

  return (
    <div className="panel h-full flex flex-col">
      <div className="panel-title">Recent Trades</div>

      <div className="grid grid-cols-3 text-xs text-gray-500 mb-1 px-1">
        <span>Price</span>
        <span className="text-right">Size</span>
        <span className="text-right">Time</span>
      </div>

      {loading ? (
        <div className="text-xs text-gray-500 text-center py-4">Loading…</div>
      ) : (
        <div className="flex-1 overflow-y-auto space-y-0.5">
          {trades.length === 0 && (
            <div className="text-xs text-gray-500 text-center py-4">No trades yet</div>
          )}
          {trades.map((t, i) => {
            const prevPrice = trades[i + 1]?.price;
            const color =
              !prevPrice
                ? 'text-gray-300'
                : parseFloat(t.price) >= parseFloat(prevPrice)
                ? 'text-accent-green'
                : 'text-accent-red';
            return (
              <div key={t.id || i} className="grid grid-cols-3 text-xs px-1 py-0.5 hover:bg-surface-2">
                <span className={color}>{fmt(t.price)}</span>
                <span className="text-right text-gray-300">{fmt(t.quantity, 6)}</span>
                <span className="text-right text-gray-500">{fmtTime(t.executedAt)}</span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
