package com.openex.backend.repository

import com.openex.backend.entity.User
import com.openex.backend.entity.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import jakarta.persistence.LockModeType
import java.util.UUID

interface WalletRepository : JpaRepository<Wallet, UUID> {
    fun findAllByUser(user: User): List<Wallet>
    fun findByUserAndCurrency(user: User, currency: String): Wallet?

    /** Pessimistic write lock — use when updating balances */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user = :user AND w.currency = :currency")
    fun findByUserAndCurrencyForUpdate(user: User, currency: String): Wallet?
}
