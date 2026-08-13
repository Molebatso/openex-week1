package com.openex.backend.service

import com.openex.backend.dto.WalletResponse
import com.openex.backend.entity.User
import com.openex.backend.entity.Wallet
import com.openex.backend.exception.ResourceNotFoundException
import com.openex.backend.repository.WalletRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class WalletService(
    private val walletRepository: WalletRepository,
) {
    private val log = LoggerFactory.getLogger(WalletService::class.java)

    companion object {
        /** Default currencies provisioned for every new user */
        val DEFAULT_CURRENCIES = listOf("USD", "BTC", "ETH")
    }

    /** Create one wallet per default currency for a new user. */
    @Transactional
    fun createDefaultWallets(user: User): List<Wallet> {
        val wallets = DEFAULT_CURRENCIES.map { currency ->
            Wallet(user = user, currency = currency)
        }
        return walletRepository.saveAll(wallets).also {
            log.info("Provisioned ${it.size} wallets for user ${user.username}")
        }
    }

    /** Return all wallets belonging to the given user. */
    @Transactional(readOnly = true)
    fun getWallets(user: User): List<WalletResponse> =
        walletRepository.findAllByUser(user).map { it.toResponse() }

    /**
     * Credit a wallet (add funds).
     * Called by LedgerService only — never call directly.
     */
    @Transactional
    fun credit(user: User, currency: String, amount: BigDecimal): Wallet {
        require(amount > BigDecimal.ZERO) { "Credit amount must be positive" }
        val wallet = getOrCreateWallet(user, currency)
        wallet.balance = wallet.balance.add(amount)
        return walletRepository.save(wallet).also {
            log.debug("Credited {} {} to user {}", amount, currency, user.username)
        }
    }

    /**
     * Debit a wallet (remove funds).
     * Called by LedgerService only — never call directly.
     * Throws [InsufficientFundsException] if balance would go negative.
     */
    @Transactional
    fun debit(user: User, currency: String, amount: BigDecimal): Wallet {
        require(amount > BigDecimal.ZERO) { "Debit amount must be positive" }
        val wallet = walletRepository.findByUserAndCurrencyForUpdate(user, currency)
            ?: throw ResourceNotFoundException("Wallet not found for currency $currency")

        if (wallet.balance < amount) {
            throw com.openex.backend.exception.InsufficientFundsException(
                "Insufficient $currency balance: have ${wallet.balance}, need $amount"
            )
        }
        wallet.balance = wallet.balance.subtract(amount)
        return walletRepository.save(wallet).also {
            log.debug("Debited {} {} from user {}", amount, currency, user.username)
        }
    }

    /** Find a wallet or create it with zero balance. */
    @Transactional
    fun getOrCreateWallet(user: User, currency: String): Wallet {
        return walletRepository.findByUserAndCurrency(user, currency)
            ?: walletRepository.save(Wallet(user = user, currency = currency))
    }

    private fun Wallet.toResponse() = WalletResponse(
        id = requireNotNull(id),
        currency = currency,
        balance = balance,
    )
}
