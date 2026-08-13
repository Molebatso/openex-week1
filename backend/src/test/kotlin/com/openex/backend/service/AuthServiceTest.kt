package com.openex.backend.service

import com.openex.backend.dto.LoginRequest
import com.openex.backend.dto.RegisterRequest
import com.openex.backend.entity.User
import com.openex.backend.exception.DuplicateResourceException
import com.openex.backend.repository.UserRepository
import com.openex.backend.security.JwtTokenProvider
import com.openex.backend.security.OpenExUserDetails
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var authenticationManager: AuthenticationManager
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var walletService: WalletService
    private lateinit var authService: AuthService

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        passwordEncoder = mockk()
        authenticationManager = mockk()
        jwtTokenProvider = mockk()
        walletService = mockk(relaxed = true)

        authService = AuthService(
            userRepository,
            passwordEncoder,
            authenticationManager,
            jwtTokenProvider,
            walletService
        )
    }

    @Test
    fun `register creates a new user and returns JWT`() {

        val request = RegisterRequest(
            "alice",
            "alice@ex.com",
            "password123"
        )

        val savedUser = User(
            id = UUID.randomUUID(),
            username = "alice",
            email = "alice@ex.com",
            passwordHash = "hashed"
        )

        every {
            userRepository.existsByUsername("alice")
        } returns false

        every {
            userRepository.existsByEmail("alice@ex.com")
        } returns false

        every {
            passwordEncoder.encode("password123")
        } returns "hashed"

        every {
            userRepository.save(any())
        } returns savedUser

        val authToken = UsernamePasswordAuthenticationToken(
            OpenExUserDetails.from(savedUser),
            null,
            emptyList()
        )

        every {
            authenticationManager.authenticate(any())
        } returns authToken

        every {
            jwtTokenProvider.generateToken(authToken)
        } returns "jwt.token.here"

        every {
            userRepository.findByUsername("alice")
        } returns savedUser

        val response = authService.register(request)

        assertEquals("jwt.token.here", response.token)
        assertEquals("alice", response.username)
        assertEquals("alice@ex.com", response.email)

        verify(exactly = 1) {
            userRepository.save(any())
        }

        verify(exactly = 1) {
            walletService.createDefaultWallets(savedUser)
        }
    }

    @Test
    fun `register throws DuplicateResourceException when username is taken`() {

        every {
            userRepository.existsByUsername("alice")
        } returns true

        assertThrows<DuplicateResourceException> {
            authService.register(
                RegisterRequest(
                    "alice",
                    "new@ex.com",
                    "password123"
                )
            )
        }

        verify(exactly = 0) {
            userRepository.save(any())
        }
    }

    @Test
    fun `register throws DuplicateResourceException when email is taken`() {

        every {
            userRepository.existsByUsername("alice2")
        } returns false

        every {
            userRepository.existsByEmail("alice@ex.com")
        } returns true

        assertThrows<DuplicateResourceException> {
            authService.register(
                RegisterRequest(
                    "alice2",
                    "alice@ex.com",
                    "password123"
                )
            )
        }

        verify(exactly = 0) {
            userRepository.save(any())
        }
    }

    @Test
    fun `login returns JWT for valid credentials`() {

        val user = User(
            id = UUID.randomUUID(),
            username = "bob",
            email = "bob@ex.com",
            passwordHash = "hashed"
        )

        val authToken = UsernamePasswordAuthenticationToken(
            OpenExUserDetails.from(user),
            null,
            emptyList()
        )

        every {
            authenticationManager.authenticate(any())
        } returns authToken

        every {
            jwtTokenProvider.generateToken(authToken)
        } returns "valid.jwt"

        every {
            userRepository.findByUsername("bob")
        } returns user

        val response = authService.login(
            LoginRequest(
                "bob",
                "password123"
            )
        )

        assertEquals("valid.jwt", response.token)
        assertEquals("bob", response.username)
        assertEquals("bob@ex.com", response.email)
    }
}