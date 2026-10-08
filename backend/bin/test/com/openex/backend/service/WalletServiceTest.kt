package com.openex.backend.service

import com.openex.backend.entity.User
import com.openex.backend.entity.Wallet
import com.openex.backend.exception.InsufficientFundsException
import com.openex.backend.exception.ResourceNotFoundException
import com.openex.backend.repository.WalletRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class WalletServiceTest {

    private lateinit var walletRepository: WalletRepository
    private lateinit var walletService: WalletService

    private val user = User(username = "alice", email = "alice@ex.com", passwordHash = "hash")

    @BeforeEach
    fun setup() {
        walletRepository = mockk()
        walletService    = WalletService(walletRepository)

        every { walletRepository.save(any()) } answers { firstArg() }
        every { walletRepository.saveAll(any<List<Wallet>>()) } answers { firstArg() }
    }

    @Test
    fun `createDefaultWallets creates one wallet per default currency`() {
        walletService.createDefaultWallets(user)

        verify(exactly = 1) {
            walletRepository.saveAll(match<List<Wallet>> { wallets ->
                wallets.size == WalletService.DEFAULT_CURRENCIES.size &&
                wallets.all { it.balance == BigDecimal.ZERO }
            })
        }
    }

    @Test
    fun `credit increases balance correctly`() {
        val wallet = Wallet(user = user, currency = "USD", balance = BigDecimal("100"))
        every { walletRepository.findByUserAndCurrency(user, "USD") } returns wallet

        walletService.credit(user, "USD", BigDecimal("50"))

        assertEquals(BigDecimal("150"), wallet.balance)
    }

    @Test
    fun `debit decreases balance correctly`() {
        val wallet = Wallet(user = user, currency = "BTC", balance = BigDecimal("2"))
        every { walletRepository.findByUserAndCurrencyForUpdate(user, "BTC") } returns wallet

        walletService.debit(user, "BTC", BigDecimal("0.5"))

        assertEquals(BigDecimal("1.5"), wallet.balance)
    }

    @Test
    fun `debit throws InsufficientFundsException when balance is too low`() {
        val wallet = Wallet(user = user, currency = "BTC", balance = BigDecimal("0.1"))
        every { walletRepository.findByUserAndCurrencyForUpdate(user, "BTC") } returns wallet

        assertThrows<InsufficientFundsException> {
            walletService.debit(user, "BTC", BigDecimal("1"))
        }
    }

    @Test
    fun `debit throws ResourceNotFoundException when wallet does not exist`() {
        every { walletRepository.findByUserAndCurrencyForUpdate(user, "ETH") } returns null

        assertThrows<ResourceNotFoundException> {
            walletService.debit(user, "ETH", BigDecimal("1"))
        }
    }

    @Test
    fun `credit rejects zero or negative amounts`() {
        every { walletRepository.findByUserAndCurrency(user, "USD") } returns
            Wallet(user = user, currency = "USD", balance = BigDecimal("100"))

        assertThrows<IllegalArgumentException> { walletService.credit(user, "USD", BigDecimal.ZERO) }
        assertThrows<IllegalArgumentException> { walletService.credit(user, "USD", BigDecimal("-1")) }
    }
}
