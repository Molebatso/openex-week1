// ── Auth ──────────────────────────────────────────────────────────────────────

export interface AuthResponse {
  token: string;
  tokenType: string;
  username: string;
  email: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

// ── Wallet ───────────────────────────────────────────────────────────────────

export interface WalletResponse {
  id: string;
  currency: string;
  balance: string;
}

export interface DepositRequest {
  currency: string;
  amount: number;
}

// ── Orders ───────────────────────────────────────────────────────────────────

export type OrderSide = 'BUY' | 'SELL';
export type OrderType = 'LIMIT' | 'MARKET';
export type OrderStatus = 'OPEN' | 'PARTIAL' | 'FILLED' | 'CANCELLED';

export interface OrderRequest {
  symbol: string;
  side: OrderSide;
  type: OrderType;
  price?: number;
  quantity: number;
  idempotencyKey?: string;
}

export interface OrderResponse {
  id: string;
  symbol: string;
  side: OrderSide;
  type: OrderType;
  price: string | null;
  quantity: string;
  filledQuantity: string;
  remainingQuantity: string;
  status: OrderStatus;
  createdAt: string;
}

// ── Trades ───────────────────────────────────────────────────────────────────

export interface TradeResponse {
  id: string;
  symbol: string;
  buyOrderId: string;
  sellOrderId: string;
  price: string;
  quantity: string;
  total: string;
  executedAt: string;
}

// ── Market ───────────────────────────────────────────────────────────────────

export interface PriceLevel {
  price: string;
  quantity: string;
}

export interface OrderBookResponse {
  symbol: string;
  bids: PriceLevel[];
  asks: PriceLevel[];
  timestamp: string;
}

export interface MarketStatsResponse {
  symbol: string;
  latestPrice: string | null;
  high24h: string | null;
  low24h: string | null;
  volume24h: string;
  priceChange24h: string | null;
  priceChangePct24h: string | null;
  timestamp: string;
}

// ── WebSocket events ─────────────────────────────────────────────────────────

export interface WsTradeEvent {
  type: 'TRADE';
  id: string;
  symbol: string;
  price: string;
  quantity: string;
  total: string;
  executedAt: string;
}

export interface WsOrderBookEvent {
  type: 'ORDER_BOOK_UPDATE';
  symbol: string;
  bids: PriceLevel[];
  asks: PriceLevel[];
  timestamp: string;
}

export interface WsMarketUpdateEvent {
  type: 'MARKET_UPDATE';
  symbol: string;
  latestPrice: string | null;
  high24h: string | null;
  low24h: string | null;
  volume24h: string;
  timestamp: string;
}

// ── AI ────────────────────────────────────────────────────────────────────────

export interface AiChatRequest {
  message: string;
}

export interface AiChatResponse {
  reply: string;
  error?: string;
}
