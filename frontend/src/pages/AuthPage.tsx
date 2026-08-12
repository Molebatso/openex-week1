import React, { useState } from 'react';
import { authApi } from '../api/auth';
import { useAuth } from '../context/AuthContext';

type Mode = 'login' | 'register';

export function AuthPage() {
  const { login } = useAuth();
  const [mode, setMode] = useState<Mode>('login');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const response =
        mode === 'login'
          ? await authApi.login({ username, password })
          : await authApi.register({ username, email, password });
      login(response);
    } catch (err: any) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Authentication failed. Please try again.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-surface px-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="text-3xl font-bold text-white mb-1">
            Open<span className="text-accent-blue">Ex</span>
          </div>
          <div className="text-xs text-gray-500 tracking-widest uppercase">
            Crypto Trading Terminal
          </div>
        </div>

        <div className="panel">
          {/* Tabs */}
          <div className="flex mb-6 border-b border-surface-3">
            {(['login', 'register'] as Mode[]).map((m) => (
              <button
                key={m}
                onClick={() => { setMode(m); setError(null); }}
                className={`flex-1 pb-2 text-sm font-semibold capitalize transition-colors
                  ${mode === m
                    ? 'text-accent-blue border-b-2 border-accent-blue'
                    : 'text-gray-500 hover:text-gray-300'
                  }`}
              >
                {m}
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="text-xs text-gray-500 block mb-1">Username</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="satoshi"
                autoComplete="username"
                required
                className="input-field"
              />
            </div>

            {mode === 'register' && (
              <div>
                <label className="text-xs text-gray-500 block mb-1">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="satoshi@nakamoto.com"
                  autoComplete="email"
                  required
                  className="input-field"
                />
              </div>
            )}

            <div>
              <label className="text-xs text-gray-500 block mb-1">Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                required
                minLength={8}
                className="input-field"
              />
            </div>

            {error && (
              <div className="text-xs text-accent-red bg-red-900/20 rounded px-3 py-2">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="btn-primary bg-accent-blue hover:bg-blue-500 text-white disabled:opacity-50"
            >
              {loading ? 'Please wait…' : mode === 'login' ? 'Sign In' : 'Create Account'}
            </button>
          </form>
        </div>

        <p className="text-center text-xs text-gray-600 mt-4">
          OpenEx 3.0 — Simulated crypto exchange for educational purposes
        </p>
      </div>
    </div>
  );
}
