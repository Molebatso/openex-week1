package com.openex.backend.ledger

import com.openex.backend.entity.EntryType
import com.openex.backend.entity.LedgerEntry
import com.openex.backend.entity.Trade
import com.openex.backend.repository.LedgerEntryRepository
import com.openex.backend.repository.WalletRepository
import com.openex.backend.service.WalletService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * LedgerService enforces the double-entry accounting invariant.
 *
 * For each trade, four ledger entries are created atomically:
 *
 *   BUYER:
 *     DEBIT  quote-currency (e.g. USD)  — funds leave buyer
 *     CREDIT base-currency  (e.g. BTC)  — asset arrives for buyer
 *
 *   SELLER:
 *     DEBIT  base-currency  (e.g. BTC)  — asset leaves seller
 *     CREDIT quote-currency (e.g. USD)  — funds arrive for seller
 *
 * Wallet balances are updated inside the same transaction so they stay
 * consistent with the ledger at all times.
 */
@Service
class LedgerService(
    private val ledgerEntryRepository: LedgerEntryRepository,
    private val walletRepository: WalletRepository,
    private val walletService: WalletService,
) {
    private val log = LoggerFactory.getLogger(LedgerService::class.java)

    /**
     * Record trade settlement.
     *
     * @param trade  the executed trade
     * @param baseCurrency   asset being exchanged  (e.g. "BTC")
     * @param quoteCurrency  payment currency        (e.g. "USD")
     */
    @Transactional
    fun settleTrade(
        trade: Trade,
        baseCurrency: String,
        quoteCurrency: String,
    ) {
        val buyer  = trade.buyOrder.user
        val seller = trade.sellOrder.user

        val quoteAmount = trade.price.multiply(trade.quantity)  // total USD cost
        val baseAmount  = trade.quantity                         // total BTC amount

        // ── Wallets (pessimistic lock) ────────────────────────────────────────
        val buyerQuoteWallet  = walletService.debit(buyer,  quoteCurrency, quoteAmount)
        val buyerBaseWallet   = walletService.credit(buyer,  baseCurrency,  baseAmount)
        val sellerBaseWallet  = walletService.debit(seller, baseCurrency,  baseAmount)
        val sellerQuoteWallet = walletService.credit(seller, quoteCurrency, quoteAmount)

        // ── Ledger entries ────────────────────────────────────────────────────
        val entries = listOf(
            // Buyer: USD leaves
            LedgerEntry(
                trade = trade, wallet = buyerQuoteWallet,
                entryType = EntryType.DEBIT, currency = quoteCurrency, amount = quoteAmount,
            ),
            // Buyer: BTC arrives
            LedgerEntry(
                trade = trade, wallet = buyerBaseWallet,
                entryType = EntryType.CREDIT, currency = baseCurrency, amount = baseAmount,
            ),
            // Seller: BTC leaves
            LedgerEntry(
                trade = trade, wallet = sellerBaseWallet,
                entryType = EntryType.DEBIT, currency = baseCurrency, amount = baseAmount,
            ),
            // Seller: USD arrives
            LedgerEntry(
                trade = trade, wallet = sellerQuoteWallet,
                entryType = EntryType.CREDIT, currency = quoteCurrency, amount = quoteAmount,
            ),
        )

        ledgerEntryRepository.saveAll(entries)

        // ── Invariant check ───────────────────────────────────────────────────
        assertLedgerBalances(entries, baseCurrency, quoteCurrency)

        log.info(
            "Settled trade {} — {} {} @ {} {} (buyer: {}, seller: {})",
            trade.id, baseAmount, baseCurrency, trade.price, quoteCurrency,
            buyer.username, seller.username,
        )
    }

    /**
     * Assert the double-entry invariant:
     * sum(CREDIT amounts) == sum(DEBIT amounts) for each currency.
     */
    private fun assertLedgerBalances(
        entries: List<LedgerEntry>,
        vararg currencies: String,
    ) {
        for (currency in currencies) {
            val debits  = entries.filter { it.currency == currency && it.entryType == EntryType.DEBIT  }.sumOf { it.amount }
            val credits = entries.filter { it.currency == currency && it.entryType == EntryType.CREDIT }.sumOf { it.amount }
            check(debits.compareTo(credits) == 0) {
                "Double-entry invariant violated for $currency: debits=$debits credits=$credits"
            }
        }
    }

    private fun <T> Iterable<T>.sumOf(selector: (T) -> BigDecimal): BigDecimal =
        fold(BigDecimal.ZERO) { acc, item -> acc + selector(item) }
}
