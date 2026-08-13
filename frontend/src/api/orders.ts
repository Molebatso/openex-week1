import apiClient from './client';
import type { OrderRequest, OrderResponse } from '../types';

export const ordersApi = {
  place: (data: OrderRequest) =>
    apiClient.post<OrderResponse>('/orders', data).then((r) => r.data),

  list: () =>
    apiClient.get<OrderResponse[]>('/orders').then((r) => r.data),

  get: (id: string) =>
    apiClient.get<OrderResponse>(`/orders/${id}`).then((r) => r.data),

  cancel: (id: string) =>
    apiClient.delete<OrderResponse>(`/orders/${id}`).then((r) => r.data),
};
