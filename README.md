# OpenEx 3.0 — Simulated Crypto Exchange & AI Trading Terminal

OpenEx 3.0 is a lightweight, full-stack **simulated cryptocurrency exchange and AI trading terminal** developed as a capstone project.

The platform combines a simulated crypto exchange with real-time trading functionality, including:

* 🔐 JWT-based authentication
* 💰 Wallets and simulated deposits
* 📒 Double-entry accounting
* 📈 In-memory order matching
* 🧾 LIMIT and MARKET orders
* ⚡ Partial order fills
* 📖 Live order book
* 🔄 Real-time trade and market updates over WebSockets
* 📊 Simulated cryptocurrency market data
* 🤖 Local-LLM-powered AI trading assistant
* 🗄️ PostgreSQL persistence
* 🧪 Automated backend testing

> **Important:** OpenEx is strictly an educational and demonstration project. All accounts, balances, orders, trades, and funds are simulated. The application does not connect to real cryptocurrency exchanges and does not involve real money.

---

## 📑 Table of Contents

* [Architecture](#architecture)
* [Services](#services)
* [Technology Stack](#technology-stack)
* [Prerequisites](#prerequisites)
* [Getting Started](#getting-started)

  * [1. Start PostgreSQL](#1-start-postgresql)
  * [2. Start the Backend](#2-start-the-backend)
  * [3. Start the Frontend](#3-start-the-frontend)
  * [4. Start the Market Simulator](#4-start-the-market-simulator)
  * [5. Start the AI Assistant](#5-start-the-ai-assistant)
* [Running the Complete System](#running-the-complete-system)
* [API Reference](#api-reference)
* [WebSocket API](#websocket-api)
* [Core Architecture Decisions](#core-architecture-decisions)
* [Testing](#testing)
* [Known Limitations](#known-limitations)
* [Troubleshooting](#troubleshooting)
* [Project Structure](#project-structure)
* [License](#license)

---

# 🏗️ Architecture

OpenEx consists of four independently running services:

```text
                              REST + WebSocket
                    ┌─────────────────────────────────┐
                    │                                 │
                    ▼                                 │
┌────────────────────────────┐              ┌─────────┴──────────────────┐
│                            │              │                            │
│     React Trading UI       │              │     Kotlin / Spring Boot   │
│     React + Vite + TS      │◄────────────►│     Core Exchange Engine   │
│                            │              │                            │
│          :3000             │              │           :8080             │
│                            │              │                            │
└─────────────┬──────────────┘              │       PostgreSQL :5432      │
              │                             │                            │
              │ /api/sim/*                  └────────────┬───────────────┘
              │                                          │
              ▼                                          │
┌────────────────────────────┐                           │
│                            │                           │
│     Market Simulator       │                           │
│     Python + Flask         │                           │
│                            │                           │
│          :5001             │                           │
│                            │                           │
└────────────────────────────┘                           │
                                                         │
                                                         │ GET /api/wallet
                                                         │
                                                         │ Authenticated
                                                         │ request using
                                                         │ user's JWT
                                                         │
                                             ┌───────────┴──────────────┐
                                             │                          │
                                             │      AI Assistant        │
                                             │  Python + Flask          │
                                             │  LangChain + Ollama      │
                                             │                          │
                                             │          :8001           │
                                             │                          │
                                             └──────────────────────────┘
```

### Service Overview

| Service          |   Port | Responsibility                                                                  |
| ---------------- | -----: | ------------------------------------------------------------------------------- |
| Backend          | `8080` | Authentication, wallets, orders, matching engine, ledger, trades and WebSockets |
| Frontend         | `3000` | Trading terminal and user interface                                             |
| Market Simulator | `5001` | Synthetic, continuously moving cryptocurrency prices                            |
| AI Assistant     | `8001` | Local-LLM chat assistant and wallet lookup                                      |
| PostgreSQL       | `5432` | Persistent application data                                                     |

The **backend and frontend form the core exchange** and can operate independently.

The market simulator and AI assistant are optional extensions:

* The **market simulator** provides synthetic price movement for charts.
* The **AI assistant** provides a natural-language interface for interacting with exchange information.
* Neither Python service depends on the other.

---

# 🧩 Project Structure

```text
openex-week1/
│
├── backend/
│   ├── src/
│   ├── build.gradle.kts
│   ├── gradlew
│   └── gradlew.bat
│
├── frontend/
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
│
├── market-sim/
│   ├── app.py
│   ├── requirements.txt
│   └── venv/
│
├── ai-assistant/
│   ├── app.py
│   ├── requirements.txt
│   └── venv/
│
└── README.md
```

---

# 🛠️ Technology Stack

| Layer               | Technology                  |
| ------------------- | --------------------------- |
| Backend             | Kotlin, Spring Boot 3.x     |
| Security            | Spring Security, JWT        |
| Persistence         | Spring Data JPA, PostgreSQL |
| Database Migrations | Flyway                      |
| Matching Engine     | Kotlin in-memo              |
