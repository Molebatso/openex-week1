package com.openex.backend.matching

import com.openex.backend.entity.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import com.openex.backend.ledger.LedgerService
import com.openex.backend.repository.OrderRepository
import com.openex.backend.repository.TradeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.util.*

class MatchingEngineTest {

    private lateinit var orderRepository: OrderRepository
    private lateinit var tradeRepository: TradeRepository
    private lateinit var ledgerService: LedgerService
    private lateinit var engine: MatchingEngineService

    private val seller = User(username = "seller", email = "s@ex.com", passwordHash = "hash")
    private val buyer  = User(username = "buyer",  email = "b@ex.com", passwordHash = "hash")

    @BeforeEach
    fun setup() {
        orderRepository = mockk(relaxed = true)
        tradeRepository = mockk(relaxed = true)
        ledgerService   = mockk(relaxed = true)
        engine          = MatchingEngineService(orderRepository, tradeRepository, ledgerService)

        // Return saved entities as-is
        every { orderRepository.save(any()) } answers { firstArg() }
        every { tradeRepository.save(any()) } answers {
            firstArg<Trade>().copy(id = UUID.randomUUID())
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun limitSell(price: String, qty: String) = Order(
        user = seller, symbol = "BTC/USD", side = OrderSide.SELL,
        type = OrderType.LIMIT, price = BigDecimal(price), quantity = BigDecimal(qty)
    )

    private fun limitBuy(price: String, qty: String) = Order(
        user = buyer, symbol = "BTC/USD", side = OrderSide.BUY,
        type = OrderType.LIMIT, price = BigDecimal(price), quantity = BigDecimal(qty)
    )

    private fun marketBuy(qty: String) = Order(
        user = buyer, symbol = "BTC/USD", side = OrderSide.BUY,
        type = OrderType.MARKET, price = null, quantity = BigDecimal(qty)
    )

    private fun marketSell(qty: String) = Order(
        user = seller, symbol = "BTC/USD", side = OrderSide.SELL,
        type = OrderType.MARKET, price = null, quantity = BigDecimal(qty)
    )

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `limit sell resting, matching limit buy produces a trade`() {
        val sell = limitSell("50000", "1")
        engine.submit(sell)

        val buy = limitBuy("50000", "1")
        val trades = engine.submit(buy)

        assertEquals(1, trades.size)
        assertEquals(BigDecimal("50000"), trades[0].price)
        assertEquals(BigDecimal("1"), trades[0].quantity)
        assertEquals(OrderStatus.FILLED, sell.status)
        assertEquals(OrderStatus.FILLED, buy.status)
    }

    @Test
    fun `buy limit order does NOT match when bid price is below ask`() {
        val sell = limitSell("51000", "1")
        engine.submit(sell)

        val buy = limitBuy("50000", "1")
        val trades = engine.submit(buy)

        assertTrue(trades.isEmpty(), "No trade should occur when bid < ask")
        assertEquals(OrderStatus.OPEN, buy.status)
        assertEquals(OrderStatus.OPEN, sell.status)
    }

    @Test
    fun `partial fill — buy order larger than sell order`() {
        val sell = limitSell("50000", "0.5")
        engine.submit(sell)

        val buy = limitBuy("50000", "1")
        val trades = engine.submit(buy)

        assertEquals(1, trades.size)
        assertEquals(BigDecimal("0.5"), trades[0].quantity)
        assertEquals(OrderStatus.FILLED,  sell.status)
        assertEquals(OrderStatus.PARTIAL, buy.status)
        assertEquals(BigDecimal("0.5"), buy.filledQuantity)
    }

    @Test
    fun `partial fill — multiple resting sells consumed by one large buy`() {
        engine.submit(limitSell("50000", "0.3"))
        engine.submit(limitSell("50000", "0.3"))
        engine.submit(limitSell("50000", "0.3"))

        val buy = limitBuy("50000", "0.9")
        val trades = engine.submit(buy)

        assertEquals(3, trades.size)
        assertEquals(BigDecimal("0.9"), buy.filledQuantity)
        assertEquals(OrderStatus.FILLED, buy.status)
    }

    @Test
    fun `price-time priority — lowest ask matched first`() {
        engine.submit(limitSell("51000", "1"))
        engine.submit(limitSell("50000", "1"))  // should match first

        val buy = limitBuy("52000", "1")
        val trades = engine.submit(buy)

        assertEquals(1, trades.size)
        assertEquals(BigDecimal("50000"), trades[0].price, "Should match the lowest ask first")
    }

    @Test
    fun `market buy order matches immediately against best ask`() {
        engine.submit(limitSell("49500", "2"))

        val buy = marketBuy("1.5")
        val trades = engine.submit(buy)

        assertEquals(1, trades.size)
        assertEquals(BigDecimal("49500"), trades[0].price)
        assertEquals(BigDecimal("1.5"), trades[0].quantity)
        assertEquals(OrderStatus.FILLED, buy.status)
    }

    @Test
    fun `market sell order matches immediately against best bid`() {
        engine.submit(limitBuy("49500", "2"))

        val sell = marketSell("1")
        val trades = engine.submit(sell)

        assertEquals(1, trades.size)
        assertEquals(BigDecimal("49500"), trades[0].price)
        assertEquals(OrderStatus.FILLED, sell.status)
    }

    @Test
    fun `cancelled order is removed from book — no further matches`() {
        val sell = limitSell("50000", "1")
        engine.submit(sell)

        every { orderRepository.findById(sell.id) } returns Optional.of(sell)
        engine.cancel(sell.id)

        val buy = limitBuy("50000", "1")
        val trades = engine.submit(buy)

        assertTrue(trades.isEmpty(), "Cancelled order should not be matched")
    }

    @Test
    fun `ledger service is called for every executed trade`() {
        engine.submit(limitSell("50000", "1"))
        engine.submit(limitBuy("50000", "1"))

        verify(exactly = 1) { ledgerService.settleTrade(any(), "BTC", "USD") }
    }

    @Test
    fun `10 concurrent orders — all matching orders produce trades`() {
        // 5 sells at 50000
        repeat(5) { engine.submit(limitSell("50000", "1")) }

        // 5 buys at 50000 — should match all 5 sells
        var totalTrades = 0
        repeat(5) { totalTrades += engine.submit(limitBuy("50000", "1")).size }

        assertEquals(5, totalTrades)
    }
}
