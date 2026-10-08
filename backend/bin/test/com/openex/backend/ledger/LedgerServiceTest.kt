package com.openex.backend.ledger

import com.openex.backend.entity.*
import com.openex.backend.repository.LedgerEntryRepository
import com.openex.backend.repository.WalletRepository
import com.openex.backend.service.WalletService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class LedgerServiceTest {

    private lateinit var ledgerEntryRepository: LedgerEntryRepository
    private lateinit var walletRepository: WalletRepository
    private lateinit var walletService: WalletService
    private lateinit var ledgerService: LedgerService

    private val buyer  = User(username = "buyer",  email = "b@ex.com", passwordHash = "h")
    private val seller = User(username = "seller", email = "s@ex.com", passwordHash = "h")

    @BeforeEach
    fun setup() {
        ledgerEntryRepository = mockk()
        walletRepository      = mockk()
        walletService         = mockk()
        ledgerService         = LedgerService(ledgerEntryRepository, walletRepository, walletService)

        every { ledgerEntryRepository.saveAll(any<List<LedgerEntry>>()) } answers { firstArg() }
    }

    private fun makeTrade(price: String, qty: String): Trade {
        val buyOrder = Order(
            user = buyer, symbol = "BTC/USD",
            side = OrderSide.BUY, type = OrderType.LIMIT,
            price = BigDecimal(price), quantity = BigDecimal(qty)
        )
        val sellOrder = Order(
            user = seller, symbol = "BTC/USD",
            side = OrderSide.SELL, type = OrderType.LIMIT,
            price = BigDecimal(price), quantity = BigDecimal(qty)
        )
        return Trade(
            buyOrder = buyOrder, sellOrder = sellOrder,
            symbol = "BTC/USD",
            price = BigDecimal(price), quantity = BigDecimal(qty),
        )
    }

    @Test
    fun `settleTrade creates exactly four ledger entries`() {
        val trade = makeTrade("50000", "1")

        val usdWallet = Wallet(user = buyer,  currency = "USD", balance = BigDecimal("50000"))
        val btcBuyer  = Wallet(user = buyer,  currency = "BTC", balance = BigDecimal.ZERO)
        val btcSeller = Wallet(user = seller, currency = "BTC", balance = BigDecimal("1"))
        val usdSeller = Wallet(user = seller, currency = "USD", balance = BigDecimal.ZERO)

        every { walletService.debit(buyer,  "USD", BigDecimal("50000")) } returns usdWallet
        every { walletService.credit(buyer, "BTC", BigDecimal("1"))     } returns btcBuyer
        every { walletService.debit(seller, "BTC", BigDecimal("1"))     } returns btcSeller
        every { walletService.credit(seller,"USD", BigDecimal("50000")) } returns usdSeller

        val savedSlot = slot<List<LedgerEntry>>()
        every { ledgerEntryRepository.saveAll(capture(savedSlot)) } answers { firstArg() }

        ledgerService.settleTrade(trade, "BTC", "USD")

        val entries = savedSlot.captured
        assertEquals(4, entries.size, "Four ledger entries must be created per trade")
    }

    @Test
    fun `double-entry invariant — debits equal credits for each currency`() {
        val trade = makeTrade("40000", "0.5")

        val price = BigDecimal("40000")
        val qty   = BigDecimal("0.5")
        val total = price.multiply(qty) // 20000

        val usdWallet = Wallet(user = buyer,  currency = "USD", balance = BigDecimal("20000"))
        val btcBuyer  = Wallet(user = buyer,  currency = "BTC", balance = BigDecimal.ZERO)
        val btcSeller = Wallet(user = seller, currency = "BTC", balance = BigDecimal("0.5"))
        val usdSeller = Wallet(user = seller, currency = "USD", balance = BigDecimal.ZERO)

        every { walletService.debit(buyer,  "USD", total) } returns usdWallet
        every { walletService.credit(buyer, "BTC", qty)   } returns btcBuyer
        every { walletService.debit(seller, "BTC", qty)   } returns btcSeller
        every { walletService.credit(seller,"USD", total) } returns usdSeller

        val savedSlot = slot<List<LedgerEntry>>()
        every { ledgerEntryRepository.saveAll(capture(savedSlot)) } answers { firstArg() }

        // Should not throw
        assertDoesNotThrow { ledgerService.settleTrade(trade, "BTC", "USD") }

        val entries = savedSlot.captured

        // USD: 1 debit (buyer) + 1 credit (seller) — both 20000
        val usdDebit  = entries.filter { it.currency == "USD" && it.entryType == EntryType.DEBIT  }.sumOf { it.amount }
        val usdCredit = entries.filter { it.currency == "USD" && it.entryType == EntryType.CREDIT }.sumOf { it.amount }
        assertEquals(0, usdDebit.compareTo(usdCredit), "USD debits must equal USD credits")

        // BTC: 1 debit (seller) + 1 credit (buyer) — both 0.5
        val btcDebit  = entries.filter { it.currency == "BTC" && it.entryType == EntryType.DEBIT  }.sumOf { it.amount }
        val btcCredit = entries.filter { it.currency == "BTC" && it.entryType == EntryType.CREDIT }.sumOf { it.amount }
        assertEquals(0, btcDebit.compareTo(btcCredit), "BTC debits must equal BTC credits")
    }

    @Test
    fun `ledger entries are immutable — no updates are called`() {
        val trade = makeTrade("50000", "1")

        every { walletService.debit(any(), any(), any())  } returns mockk(relaxed = true)
        every { walletService.credit(any(), any(), any()) } returns mockk(relaxed = true)
        every { ledgerEntryRepository.saveAll(any<List<LedgerEntry>>()) } answers { firstArg() }

        ledgerService.settleTrade(trade, "BTC", "USD")

        // Confirm only saveAll was called — never update or delete
        verify(exactly = 0) { ledgerEntryRepository.save(any()) }
        verify(exactly = 0) { ledgerEntryRepository.deleteAll() }
        verify(exactly = 0) { ledgerEntryRepository.deleteById(any()) }
    }

    private fun <T> Iterable<T>.sumOf(selector: (T) -> BigDecimal): BigDecimal =
        fold(BigDecimal.ZERO) { acc, item -> acc + selector(item) }
}
