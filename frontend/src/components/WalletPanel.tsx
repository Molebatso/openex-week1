import React, { useEffect, useState } from 'react';
import { walletApi } from '../api/wallet';
import type { WalletResponse } from '../types';

export function WalletPanel() {
  const [wallets, setWallets] = useState<WalletResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showDeposit, setShowDeposit] = useState(false);
  const [currency, setCurrency] = useState('USD');
  const [amount, setAmount] = useState('1000');
  const [depositing, setDepositing] = useState(false);
  const [depositMessage, setDepositMessage] = useState<string | null>(null);

  const loadWallets = () => {
    setLoading(true);
    setError(null);
    walletApi.list()
      .then(setWallets)
      .catch(() => setError('Failed to load wallets'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadWallets();
  }, []);

  const handleDeposit = async (event: React.FormEvent) => {
    event.preventDefault();
    const numericAmount = Number(amount);
    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
      setDepositMessage('Enter an amount greater than zero.');
      return;
    }
    setDepositing(true);
    setDepositMessage(null);
    try {
      await walletApi.deposit({ currency, amount: numericAmount });
      setDepositMessage(`Deposited ${currency} successfully.`);
      loadWallets();
    } catch (err: any) {
      setDepositMessage(err?.response?.data?.message || 'Deposit failed. Please try again.');
    } finally {
      setDepositing(false);
    }
  };

  const fmt = (val: string, decimals = 8) => {
    const n = parseFloat(val);
    if (isNaN(n)) return '—';
    return n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: decimals });
  };

  return (
    <div className="panel h-full">
      <div className="flex items-center justify-between panel-title">
        <span>Portfolio</span>
        <button
          type="button"
          onClick={() => { setShowDeposit((open) => !open); setDepositMessage(null); }}
          className="rounded bg-accent-blue px-2 py-1 text-[10px] font-bold text-white hover:bg-blue-500"
        >
          {showDeposit ? 'Close' : '+ Deposit'}
        </button>
      </div>

      {showDeposit && (
        <form onSubmit={handleDeposit} className="mb-3 space-y-2 rounded bg-surface-2 p-2">
          <div className="text-[10px] uppercase tracking-wider text-gray-500">Simulated deposit</div>
          <div className="grid grid-cols-2 gap-2">
            <select value={currency} onChange={(e) => setCurrency(e.target.value)} className="input-field text-xs">
              <option value="USD">USD</option>
              <option value="BTC">BTC</option>
              <option value="ETH">ETH</option>
            </select>
            <input
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              type="number"
              min="0.00000001"
              step="any"
              className="input-field text-xs"
              aria-label="Deposit amount"
            />
          </div>
          <button disabled={depositing} className="w-full rounded bg-accent-green py-1.5 text-xs font-bold text-black disabled:opacity-50">
            {depositing ? 'Depositing…' : 'Confirm deposit'}
          </button>
          {depositMessage && <div className="text-[10px] text-gray-400">{depositMessage}</div>}
        </form>
      )}

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
