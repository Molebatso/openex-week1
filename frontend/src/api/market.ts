import apiClient from './client';
import type { MarketStatsResponse, OrderBookResponse } from '../types';

export const marketApi = {
  latestPrice: (symbol: string) =>
    apiClient.get<{ symbol: string; price: string | null }>(`/market/latest-price?symbol=${encodeURIComponent(symbol)}`).then((r) => r.data),

  stats: (symbol: string) =>
    apiClient.get<MarketStatsResponse>(`/market/stats?symbol=${encodeURIComponent(symbol)}`).then((r) => r.data),

  orderBook: (symbol: string, depth = 20) =>
    apiClient.get<OrderBookResponse>(`/orderbook?symbol=${encodeURIComponent(symbol)}&depth=${depth}`).then((r) => r.data),
};
