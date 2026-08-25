package com.openex.backend.service

import com.openex.backend.entity.Order
import com.openex.backend.entity.OrderSide
import com.openex.backend.entity.OrderStatus
import com.openex.backend.entity.OrderType
import com.openex.backend.entity.Trade
import com.openex.backend.entity.User
import com.openex.backend.entity.UserRole
import com.openex.backend.repository.TradeRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

class MarketDataServiceTest {

    private lateinit var tradeRepository: TradeRepository
    private lateinit var service: MarketDataService

    private val symbol = "BTC/USD"

    @BeforeEach
    fun setUp() {
        tradeRepository = mockk()
        service = MarketDataService(tradeRepository)
    }

    @Test
    fun `updateLatestPrice stores and retrieves the latest price`() {
        val price = BigDecimal("50000.00")
        service.updateLatestPrice(symbol, price)

        every { tradeRepository.findFirstBySymbolOrderByExecutedAtDesc(symbol) } returns null
        every { tradeRepository.findAllBySymbolAndExecutedAtAfter(any(), any()) } returns emptyList()

        val response = service.getLatestPrice(symbol)
        assertEquals(price, response.price)
        assertEquals(symbol, response.symbol)
    }

    @Test
    fun `getLatestPrice falls back to DB when cache is cold`() {
        val trade = buildTrade(BigDecimal("45000.00"))
        every { tradeRepository.findFirstBySymbolOrderByExecutedAtDesc(symbol) } returns trade
        every { tradeRepository.findAllBySymbolAndExecutedAtAfter(any(), any()) } returns listOf(trade)

        val response = service.getLatestPrice(symbol)
        assertEquals(BigDecimal("45000.00"), response.price)
    }

    @Test
    fun `getStats returns zero volume when no trades in 24h window`() {
        every { tradeRepository.findAllBySymbolAndExecutedAtAfter(any(), any()) } returns emptyList()
        every { tradeRepository.findFirstBySymbolOrderByExecutedAtDesc(any()) } returns null

        val stats = service.getStats(symbol)
        assertEquals(BigDecimal.ZERO, stats.volume24h)
        assertNull(stats.high24h)
        assertNull(stats.low24h)
    }

    @Test
    fun `getStats computes high low volume from 24h trades`() {
        val trades = listOf(
            buildTrade(BigDecimal("50000"), BigDecimal("0.5")),
            buildTrade(BigDecimal("51000"), BigDecimal("1.0")),
            buildTrade(BigDecimal("49500"), BigDecimal("0.25")),
        )
        every { tradeRepository.findAllBySymbolAndExecutedAtAfter(any(), any()) } returns trades
        every { tradeRepository.findFirstBySymbolOrderByExecutedAtDesc(any()) } returns trades.last()

        val stats = service.getStats(symbol)
        assertEquals(BigDecimal("51000"), stats.high24h)
        assertEquals(BigDecimal("49500"), stats.low24h)
        assertEquals(0, stats.volume24h.compareTo(BigDecimal("1.75")))
    }

    @Test
    fun `updateLatestPrice overrides previous value`() {
        service.updateLatestPrice(symbol, BigDecimal("50000"))
        service.updateLatestPrice(symbol, BigDecimal("51500"))

        every { tradeRepository.findAllBySymbolAndExecutedAtAfter(any(), any()) } returns emptyList()

        val response = service.getLatestPrice(symbol)
        assertEquals(BigDecimal("51500"), response.price)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildUser() = User(
        username = "trader",
        email = "trader@test.com",
        passwordHash = "hash",
        role = UserRole.USER,
    )

    private fun buildOrder(user: User = buildUser()) = Order(
        user = user,
        symbol = symbol,
        side = OrderSide.BUY,
        type = OrderType.LIMIT,
        price = BigDecimal("50000"),
        quantity = BigDecimal("1.0"),
        status = OrderStatus.OPEN,
    )

    private fun buildTrade(
        price: BigDecimal,
        quantity: BigDecimal = BigDecimal("1.0"),
        executedAt: Instant = Instant.now().minus(1, ChronoUnit.HOURS),
    ): Trade {
        val buyer = buildUser()
        val seller = buildUser()
        return Trade(
            buyOrder = buildOrder(buyer),
            sellOrder = buildOrder(seller),
            symbol = symbol,
            price = price,
            quantity = quantity,
            executedAt = executedAt,
        )
    }
}
