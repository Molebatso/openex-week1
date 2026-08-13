import React, { useEffect, useState } from 'react';
import { marketApi } from '../api/market';
import type { PriceLevel, WsOrderBookEvent } from '../types';

interface Props {
  symbol: string;
  wsEvent?: WsOrderBookEvent | null;
}

export function OrderBook({ symbol, wsEvent }: Props) {
  const [bids, setBids] = useState<PriceLevel[]>([]);
  const [asks, setAsks] = useState<PriceLevel[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    marketApi
      .orderBook(symbol, 15)
      .then((ob) => {
        setBids(ob.bids);
        setAsks(ob.asks);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [symbol]);

  // Apply live WebSocket updates
  useEffect(() => {
    if (!wsEvent) return;
    setBids(wsEvent.bids);
    setAsks(wsEvent.asks);
  }, [wsEvent]);

  const fmt = (val: string, dec = 2) => {
    const n = parseFloat(val);
    return isNaN(n) ? val : n.toLocaleString('en-US', { minimumFractionDigits: dec, maximumFractionDigits: dec });
  };

  const maxBidQty = Math.max(...bids.map((b) => parseFloat(b.quantity)), 1);
  const maxAskQty = Math.max(...asks.map((a) => parseFloat(a.quantity)), 1);

  const spreadPrice =
    asks[0] && bids[0]
      ? (parseFloat(asks[0].price) - parseFloat(bids[0].price)).toFixed(2)
      : null;

  return (
    <div className="panel h-full flex flex-col">
      <div className="panel-title">Order Book</div>

      {loading ? (
        <div className="text-xs text-gray-500 text-center py-4">Loading…</div>
      ) : (
        <>
          {/* Header */}
          <div className="grid grid-cols-2 text-xs text-gray-500 mb-1 px-1">
            <span>Price (USD)</span>
            <span className="text-right">Size (BTC)</span>
          </div>

          {/* Asks (sells — red, lowest at bottom) */}
          <div className="flex-1 overflow-y-auto">
            <div className="flex flex-col-reverse gap-0.5">
              {asks.slice(0, 15).map((ask, i) => {
                const pct = (parseFloat(ask.quantity) / maxAskQty) * 100;
                return (
                  <div key={i} className="relative grid grid-cols-2 text-xs px-1 py-0.5">
                    <div
                      className="absolute inset-0 bg-red-900/20"
                      style={{ width: `${pct}%` }}
                    />
                    <span className="relative text-accent-red">{fmt(ask.price)}</span>
                    <span className="relative text-right text-gray-300">{fmt(ask.quantity, 6)}</span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Spread */}
          {spreadPrice && (
            <div className="text-center text-xs text-gray-500 my-1 border-y border-surface-3 py-1">
              Spread: ${spreadPrice}
            </div>
          )}

          {/* Bids (buys — green) */}
          <div className="flex-1 overflow-y-auto">
            <div className="flex flex-col gap-0.5">
              {bids.slice(0, 15).map((bid, i) => {
                const pct = (parseFloat(bid.quantity) / maxBidQty) * 100;
                return (
                  <div key={i} className="relative grid grid-cols-2 text-xs px-1 py-0.5">
                    <div
                      className="absolute inset-0 bg-green-900/20"
                      style={{ width: `${pct}%` }}
                    />
                    <span className="relative text-accent-green">{fmt(bid.price)}</span>
                    <span className="relative text-right text-gray-300">{fmt(bid.quantity, 6)}</span>
                  </div>
                );
              })}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
