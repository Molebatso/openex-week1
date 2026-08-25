package com.openex.backend.repository

import com.openex.backend.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByUsername(username: String): User?
    fun findByEmail(email: String): User?
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
}
