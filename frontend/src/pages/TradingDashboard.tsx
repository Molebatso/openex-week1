import React, { useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../hooks/useWebSocket';
import { MarketStats } from '../components/MarketStats';
import { OrderBook } from '../components/OrderBook';
import { OrderEntry } from '../components/OrderEntry';
import { TradeHistory } from '../components/TradeHistory';
import { OpenOrders } from '../components/OpenOrders';
import { WalletPanel } from '../components/WalletPanel';
import { AiAssistant } from '../components/AiAssistant';
import type { WsTradeEvent, WsOrderBookEvent, WsMarketUpdateEvent } from '../types';

const SYMBOLS = ['BTC/USD', 'ETH/USD'];

type Tab = 'terminal' | 'ai';

export function TradingDashboard() {
  const { user, logout } = useAuth();
  const [symbol, setSymbol] = useState(SYMBOLS[0]);
  const [activeTab, setActiveTab] = useState<Tab>('terminal');
  const [orderRefresh, setOrderRefresh] = useState(0);

  // Live WebSocket events
  const [latestTrade, setLatestTrade] = useState<WsTradeEvent | null>(null);
  const [latestOrderBook, setLatestOrderBook] = useState<WsOrderBookEvent | null>(null);
  const [latestMarket, setLatestMarket] = useState<WsMarketUpdateEvent | null>(null);

  const handleTrade = useCallback((e: WsTradeEvent) => setLatestTrade(e), []);
  const handleOrderBook = useCallback((e: WsOrderBookEvent) => setLatestOrderBook(e), []);
  const handleMarketUpdate = useCallback((e: WsMarketUpdateEvent) => setLatestMarket(e), []);

  useWebSocket({
    symbol,
    onTrade: handleTrade,
    onOrderBook: handleOrderBook,
    onMarketUpdate: handleMarketUpdate,
  });

  const handleOrderPlaced = useCallback(() => {
    setOrderRefresh((n) => n + 1);
  }, []);

  return (
    <div className="min-h-screen bg-surface flex flex-col">
      {/* ── Top bar ──────────────────────────────────────────────────────── */}
      <header className="bg-surface-1 border-b border-surface-3 px-4 py-2 flex items-center gap-4">
        <div className="font-bold text-white text-lg mr-2">
          Open<span className="text-accent-blue">Ex</span>
        </div>

        {/* Symbol selector */}
        <div className="flex gap-1">
          {SYMBOLS.map((s) => (
            <button
              key={s}
              onClick={() => setSymbol(s)}
              className={`px-3 py-1 rounded text-xs font-semibold transition-colors
                ${symbol === s
                  ? 'bg-accent-blue/20 text-accent-blue border border-accent-blue/30'
                  : 'text-gray-400 hover:text-gray-200'
                }`}
            >
              {s}
            </button>
          ))}
        </div>

        {/* Live indicator */}
        <div className="flex items-center gap-1.5 text-xs text-gray-500">
          <span className="w-1.5 h-1.5 rounded-full bg-accent-green animate-pulse" />
          LIVE
        </div>

        <div className="ml-auto flex items-center gap-4">
          {/* Nav tabs */}
          <nav className="flex gap-1">
            {(['terminal', 'ai'] as Tab[]).map((t) => (
              <button
                key={t}
                onClick={() => setActiveTab(t)}
                className={`px-3 py-1 rounded text-xs font-semibold capitalize transition-colors
                  ${activeTab === t
                    ? 'bg-surface-3 text-white'
                    : 'text-gray-500 hover:text-gray-300'
                  }`}
              >
                {t === 'ai' ? 'AI Assistant' : 'Terminal'}
              </button>
            ))}
          </nav>

          <div className="text-xs text-gray-500">
            <span className="text-gray-400">{user?.username}</span>
          </div>
          <button
            onClick={logout}
            className="text-xs text-gray-500 hover:text-accent-red transition-colors"
          >
            Sign out
          </button>
        </div>
      </header>

      {/* ── Market stats bar ─────────────────────────────────────────────── */}
      <div className="px-4 pt-3">
        <MarketStats symbol={symbol} wsUpdate={latestMarket} />
      </div>

      {/* ── Main content ─────────────────────────────────────────────────── */}
      {activeTab === 'terminal' ? (
        <div className="flex-1 px-4 pb-4 pt-3 grid grid-cols-12 gap-3 min-h-0">

          {/* Left column: wallet + order entry */}
          <div className="col-span-2 flex flex-col gap-3">
            <WalletPanel />
          </div>

          {/* Centre: order book + trade history */}
          <div className="col-span-2 flex flex-col gap-3" style={{ height: 'calc(100vh - 140px)' }}>
            <div className="flex-1">
              <OrderBook symbol={symbol} wsEvent={latestOrderBook} />
            </div>
            <div className="flex-1">
              <TradeHistory symbol={symbol} wsEvent={latestTrade} />
            </div>
          </div>

          {/* Right: order entry + open orders */}
          <div className="col-span-8 flex flex-col gap-3">
            {/* Order entry + open orders on same row */}
            <div className="grid grid-cols-5 gap-3">
              <div className="col-span-2">
                <OrderEntry symbol={symbol} onOrderPlaced={handleOrderPlaced} />
              </div>
              <div className="col-span-3">
                <OpenOrders refreshTrigger={orderRefresh} />
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="flex-1 px-4 pb-4 pt-3" style={{ height: 'calc(100vh - 140px)' }}>
          <AiAssistant />
        </div>
      )}
    </div>
  );
}
