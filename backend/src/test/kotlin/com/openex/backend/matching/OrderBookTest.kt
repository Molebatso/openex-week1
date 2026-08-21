package com.openex.backend.matching

import com.openex.backend.entity.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class OrderBookTest {

    private lateinit var book: OrderBook

    private val buyer  = User(username = "buyer",  email = "b@ex.com", passwordHash = "h")
    private val seller = User(username = "seller", email = "s@ex.com", passwordHash = "h")

    @BeforeEach
    fun setup() { book = OrderBook("BTC/USD") }

    private fun bid(price: String, qty: String) = Order(
        id = UUID.randomUUID(),
        user = buyer,  symbol = "BTC/USD", side = OrderSide.BUY,
        type = OrderType.LIMIT, price = BigDecimal(price), quantity = BigDecimal(qty)
    )

    private fun ask(price: String, qty: String) = Order(
        id = UUID.randomUUID(),
        user = seller, symbol = "BTC/USD", side = OrderSide.SELL,
        type = OrderType.LIMIT, price = BigDecimal(price), quantity = BigDecimal(qty)
    )

    @Test
    fun `no match when bid is below ask`() {
        book.add(ask("51000", "1"))
        val incoming = bid("50000", "1")
        val results = book.match(incoming)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `exact match — bid equals ask`() {
        book.add(ask("50000", "1"))
        val incoming = bid("50000", "1")
        val results = book.match(incoming)

        assertEquals(1, results.size)
        assertEquals(BigDecimal("50000"), results[0].price)
        assertEquals(BigDecimal("1"), results[0].quantity)
    }

    @Test
    fun `bid matches at ask price (price-time priority)`() {
        book.add(ask("50000", "1"))
        book.add(ask("49000", "1")) // better ask, should match first

        val incoming = bid("51000", "1")
        val results = book.match(incoming)

        assertEquals(1, results.size)
        assertEquals(BigDecimal("49000"), results[0].price, "Lowest ask must match first")
    }

    @Test
    fun `highest bid matches first when two bids compete for one ask`() {
        book.add(bid("49000", "1"))
        book.add(bid("51000", "1")) // should match first

        val incoming = ask("48000", "1")
        val results = book.match(incoming)

        assertEquals(1, results.size)
        assertEquals(BigDecimal("51000"), results[0].price)
    }

    @Test
    fun `partial fill — resting order partially consumed`() {
        val restingAsk = ask("50000", "2")
        book.add(restingAsk)

        val incoming = bid("50000", "1")
        val results = book.match(incoming)

        assertEquals(1, results.size)
        assertEquals(BigDecimal("1"), results[0].quantity)
        assertEquals(BigDecimal("1"), restingAsk.filledQuantity)
    }

    @Test
    fun `remove order — not available for matching`() {
        val restingAsk = ask("50000", "1")
        book.add(restingAsk)
        book.remove(restingAsk)

        val incoming = bid("50000", "1")
        val results = book.match(incoming)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `bestBid and bestAsk return correct prices`() {
        book.add(bid("48000", "1"))
        book.add(bid("49000", "1"))
        book.add(ask("50000", "1"))
        book.add(ask("51000", "1"))

        assertEquals(BigDecimal("49000"), book.bestBid())
        assertEquals(BigDecimal("50000"), book.bestAsk())
    }

    @Test
    fun `market buy matches regardless of price`() {
        book.add(ask("55000", "1"))

        val marketBuy = Order(
            id = UUID.randomUUID(),
            user = buyer, symbol = "BTC/USD", side = OrderSide.BUY,
            type = OrderType.MARKET, price = null, quantity = BigDecimal("1")
        )
        val results = book.match(marketBuy)

        assertEquals(1, results.size)
        assertEquals(BigDecimal("55000"), results[0].price)
    }

    @Test
    fun `market sell with no bids — no match`() {
        val marketSell = Order(
            id = UUID.randomUUID(),
            user = seller, symbol = "BTC/USD", side = OrderSide.SELL,
            type = OrderType.MARKET, price = null, quantity = BigDecimal("1")
        )
        val results = book.match(marketSell)
        assertTrue(results.isEmpty())
    }
}