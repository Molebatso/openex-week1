import React, { useEffect, useState, useCallback } from 'react';
import { ordersApi } from '../api/orders';
import type { OrderResponse, OrderStatus } from '../types';

interface Props {
  refreshTrigger?: number;
}

export function OpenOrders({ refreshTrigger }: Props) {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState<string | null>(null);

  const loadOrders = useCallback(() => {
    ordersApi
      .list()
      .then(setOrders)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadOrders();
  }, [loadOrders, refreshTrigger]);

  const handleCancel = async (id: string) => {
    setCancelling(id);
    try {
      await ordersApi.cancel(id);
      setOrders((prev) =>
        prev.map((o) => (o.id === id ? { ...o, status: 'CANCELLED' as OrderStatus } : o)),
      );
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel order');
    } finally {
      setCancelling(null);
    }
  };

  const statusBadge = (status: OrderStatus) => {
    const map: Record<OrderStatus, string> = {
      OPEN: 'badge-open',
      PARTIAL: 'badge-partial',
      FILLED: 'badge-filled',
      CANCELLED: 'badge-cancelled',
    };
    return <span className={map[status]}>{status}</span>;
  };

  const fmt = (val: string | null, dec = 2) => {
    if (!val) return '—';
    const n = parseFloat(val);
    return isNaN(n) ? val : n.toLocaleString('en-US', { minimumFractionDigits: dec, maximumFractionDigits: dec });
  };

  const activeOrders = orders.filter((o) => o.status === 'OPEN' || o.status === 'PARTIAL');

  return (
    <div className="panel">
      <div className="panel-title">Open Orders ({activeOrders.length})</div>

      {loading ? (
        <div className="text-xs text-gray-500 text-center py-4">Loading…</div>
      ) : activeOrders.length === 0 ? (
        <div className="text-xs text-gray-500 text-center py-4">No open orders</div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr className="text-gray-500 border-b border-surface-3">
                <th className="text-left py-1 pr-3">Symbol</th>
                <th className="text-left py-1 pr-3">Side</th>
                <th className="text-left py-1 pr-3">Type</th>
                <th className="text-right py-1 pr-3">Price</th>
                <th className="text-right py-1 pr-3">Qty</th>
                <th className="text-right py-1 pr-3">Filled</th>
                <th className="text-left py-1 pr-3">Status</th>
                <th className="text-right py-1">Action</th>
              </tr>
            </thead>
            <tbody>
              {activeOrders.map((order) => (
                <tr key={order.id} className="border-b border-surface-2 hover:bg-surface-2">
                  <td className="py-1.5 pr-3 text-accent-blue">{order.symbol}</td>
                  <td className={`py-1.5 pr-3 font-semibold ${order.side === 'BUY' ? 'text-accent-green' : 'text-accent-red'}`}>
                    {order.side}
                  </td>
                  <td className="py-1.5 pr-3 text-gray-400">{order.type}</td>
                  <td className="py-1.5 pr-3 text-right">{order.price ? `$${fmt(order.price)}` : 'MKT'}</td>
                  <td className="py-1.5 pr-3 text-right">{fmt(order.quantity, 6)}</td>
                  <td className="py-1.5 pr-3 text-right text-gray-400">{fmt(order.filledQuantity, 6)}</td>
                  <td className="py-1.5 pr-3">{statusBadge(order.status)}</td>
                  <td className="py-1.5 text-right">
                    <button
                      onClick={() => handleCancel(order.id)}
                      disabled={cancelling === order.id}
                      className="text-xs px-2 py-0.5 rounded bg-red-900/40 text-accent-red hover:bg-red-800/60 transition-colors disabled:opacity-50"
                    >
                      {cancelling === order.id ? '…' : 'Cancel'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
