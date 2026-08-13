import React, { useState, useRef, useEffect } from 'react';
import { aiApi } from '../api/ai';

interface Message {
  role: 'user' | 'assistant';
  text: string;
}

const SUGGESTIONS = [
  'What is my wallet balance?',
  'Show my last 5 trades.',
  'How many open orders do I have?',
  "Summarize today's trading activity.",
];

export function AiAssistant() {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      text: "Hello! I'm your AI trading assistant. I can read your wallet, orders, and trades — but I'll never execute anything. Ask me anything!",
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const send = async (text: string) => {
    if (!text.trim() || loading) return;

    const userMsg: Message = { role: 'user', text };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const res = await aiApi.chat({ message: text });
      setMessages((prev) => [...prev, { role: 'assistant', text: res.reply }]);
    } catch (err: any) {
      const errText =
        err.response?.status === 0
          ? 'AI service is offline. Make sure the ai-service is running.'
          : err.response?.data?.detail || 'Failed to reach the AI service.';
      setMessages((prev) => [...prev, { role: 'assistant', text: `⚠️ ${errText}` }]);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    send(input);
  };

  return (
    <div className="panel h-full flex flex-col">
      <div className="panel-title">AI Astromech Assistant</div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto space-y-3 mb-3 min-h-0">
        {messages.map((msg, i) => (
          <div
            key={i}
            className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-[85%] rounded-lg px-3 py-2 text-xs leading-relaxed whitespace-pre-wrap ${
                msg.role === 'user'
                  ? 'bg-accent-blue/20 text-gray-100 border border-accent-blue/30'
                  : 'bg-surface-3 text-gray-200 border border-surface-3'
              }`}
            >
              {msg.text}
            </div>
          </div>
        ))}
        {loading && (
          <div className="flex justify-start">
            <div className="bg-surface-3 rounded-lg px-3 py-2 text-xs text-gray-500 border border-surface-3">
              <span className="animate-pulse">Thinking…</span>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Quick suggestions */}
      <div className="flex flex-wrap gap-1 mb-2">
        {SUGGESTIONS.map((s) => (
          <button
            key={s}
            onClick={() => send(s)}
            disabled={loading}
            className="text-xs px-2 py-0.5 rounded bg-surface-3 text-gray-400 hover:text-accent-blue hover:border-accent-blue/30 border border-surface-3 transition-colors disabled:opacity-40"
          >
            {s}
          </button>
        ))}
      </div>

      {/* Input */}
      <form onSubmit={handleSubmit} className="flex gap-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask about your portfolio…"
          className="input-field flex-1"
          disabled={loading}
        />
        <button
          type="submit"
          disabled={loading || !input.trim()}
          className="px-4 py-2 rounded bg-accent-blue hover:bg-blue-500 text-white text-xs font-semibold transition-colors disabled:opacity-40"
        >
          Send
        </button>
      </form>
    </div>
  );
}
