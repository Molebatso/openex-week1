import React, { useEffect, useState } from 'react';
import { walletApi } from '../api/wallet';
import type { WalletResponse } from '../types';

export function WalletPanel() {
  const [wallets, setWallets] = useState<WalletResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    walletApi
      .list()
      .then(setWallets)
      .catch(() => setError('Failed to load wallets'))
      .finally(() => setLoading(false));
  }, []);

  const fmt = (val: string, decimals = 8) => {
    const n = parseFloat(val);
    if (isNaN(n)) return '—';
    return n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: decimals });
  };

  return (
    <div className="panel h-full">
      <div className="panel-title">Portfolio</div>

      {loading && <div className="text-xs text-gray-500 text-center py-4">Loading…</div>}
      {error && <div className="text-xs text-accent-red text-center py-2">{error}</div>}

      {!loading && !error && (
        <div className="space-y-2">
          {wallets.map((w) => (
            <div
              key={w.id}
              className="flex items-center justify-between bg-surface-2 rounded px-3 py-2"
            >
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold text-accent-blue w-8">{w.currency}</span>
              </div>
              <div className="text-right">
                <div className="text-sm font-semibold text-gray-100">
                  {fmt(w.balance, w.currency === 'USD' ? 2 : 8)}
                </div>
                <div className="text-xs text-gray-500">{w.currency}</div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
