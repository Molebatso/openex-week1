import axios from 'axios';
import type { AiChatRequest, AiChatResponse } from '../types';

// The AI service runs on port 8001 in development.
// In Docker it's reachable at the same host via nginx proxy at /ai.
const AI_BASE = import.meta.env.VITE_AI_URL || 'http://localhost:8001';

const aiClient = axios.create({
  baseURL: AI_BASE,
  headers: { 'Content-Type': 'application/json' },
  timeout: 60000, // LLM inference can be slow
});

aiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const aiApi = {
  chat: (data: AiChatRequest) =>
    aiClient.post<AiChatResponse>('/api/ai/chat', data).then((r) => r.data),
};
