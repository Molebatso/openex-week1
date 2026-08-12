import React from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { AuthPage } from './pages/AuthPage';
import { TradingDashboard } from './pages/TradingDashboard';

function AppInner() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <TradingDashboard /> : <AuthPage />;
}

export default function App() {
  return (
    <AuthProvider>
      <AppInner />
    </AuthProvider>
  );
}
