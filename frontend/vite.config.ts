import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],

  define: {
    global: 'globalThis',
  },

  server: {
    port: 3000,

    proxy: {
      // REST API
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },

      // WebSocket (STOMP over SockJS)
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
});