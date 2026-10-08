package com.openex.backend.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class UserRole { USER, ADMIN }

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    val id: UUID? = null,   // <-- null until Hibernate assigns it on persist

    @Column(nullable = false, unique = true, length = 50)
    val username: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: UserRole = UserRole.USER,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)