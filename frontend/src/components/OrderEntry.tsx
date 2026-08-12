import React, { useState } from 'react';
import { ordersApi } from '../api/orders';
import type { OrderSide, OrderType } from '../types';

interface Props {
  symbol: string;
  onOrderPlaced?: () => void;
}

export function OrderEntry({ symbol, onOrderPlaced }: Props) {
  const [side, setSide] = useState<OrderSide>('BUY');
  const [type, setType] = useState<OrderType>('LIMIT');
  const [price, setPrice] = useState('');
  const [quantity, setQuantity] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ text: string; kind: 'ok' | 'err' } | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);

    const qty = parseFloat(quantity);
    if (isNaN(qty) || qty <= 0) {
      setMessage({ text: 'Quantity must be a positive number', kind: 'err' });
      return;
    }

    if (type === 'LIMIT') {
      const p = parseFloat(price);
      if (isNaN(p) || p <= 0) {
        setMessage({ text: 'Limit price must be a positive number', kind: 'err' });
        return;
      }
    }

    setLoading(true);
    try {
      await ordersApi.place({
        symbol,
        side,
        type,
        price: type === 'LIMIT' ? parseFloat(price) : undefined,
        quantity: qty,
      });
      setMessage({ text: `${side} order placed!`, kind: 'ok' });
      setQuantity('');
      if (type === 'LIMIT') setPrice('');
      onOrderPlaced?.();
    } catch (err: any) {
      const msg =
        err.response?.data?.message || err.response?.data?.error || 'Failed to place order';
      setMessage({ text: msg, kind: 'err' });
    } finally {
      setLoading(false);
    }
  };

  const total =
    type === 'LIMIT' && price && quantity
      ? (parseFloat(price) * parseFloat(quantity)).toLocaleString('en-US', {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        })
      : null;

  return (
    <div className="panel h-full">
      <div className="panel-title">Place Order — {symbol}</div>

      {/* Side Toggle */}
      <div className="flex rounded overflow-hidden mb-3 border border-surface-3">
        {(['BUY', 'SELL'] as OrderSide[]).map((s) => (
          <button
            key={s}
            type="button"
            onClick={() => setSide(s)}
            className={`flex-1 py-2 text-sm font-semibold transition-colors
              ${side === s
                ? s === 'BUY'
                  ? 'bg-accent-green text-black'
                  : 'bg-accent-red text-white'
                : 'bg-surface-2 text-gray-400 hover:text-gray-200'
              }`}
          >
            {s}
          </button>
        ))}
      </div>

      {/* Order Type */}
      <div className="flex rounded overflow-hidden mb-4 border border-surface-3">
        {(['LIMIT', 'MARKET'] as OrderType[]).map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setType(t)}
            className={`flex-1 py-1.5 text-xs font-semibold transition-colors
              ${type === t
                ? 'bg-accent-blue text-white'
                : 'bg-surface-2 text-gray-400 hover:text-gray-200'
              }`}
          >
            {t}
          </button>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="space-y-3">
        {type === 'LIMIT' && (
          <div>
            <label className="text-xs text-gray-500 block mb-1">Limit Price (USD)</label>
            <input
              type="number"
              step="0.01"
              min="0"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              placeholder="0.00"
              className="input-field"
            />
          </div>
        )}

        <div>
          <label className="text-xs text-gray-500 block mb-1">Quantity (BTC)</label>
          <input
            type="number"
            step="0.00000001"
            min="0"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            placeholder="0.00000000"
            className="input-field"
          />
        </div>

        {total && (
          <div className="flex justify-between text-xs text-gray-500 bg-surface-2 rounded px-3 py-2">
            <span>Total</span>
            <span className="text-gray-200">${total}</span>
          </div>
        )}

        {message && (
          <div
            className={`text-xs rounded px-3 py-2 ${
              message.kind === 'ok'
                ? 'bg-green-900/40 text-accent-green'
                : 'bg-red-900/40 text-accent-red'
            }`}
          >
            {message.text}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className={side === 'BUY' ? 'btn-buy' : 'btn-sell'}
        >
          {loading ? 'Placing…' : `${side} ${type}`}
        </button>
      </form>
    </div>
  );
}
