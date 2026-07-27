package com.openex.backend.service

import com.openex.backend.dto.AuthResponse
import com.openex.backend.dto.LoginRequest
import com.openex.backend.dto.RegisterRequest
import com.openex.backend.entity.User
import com.openex.backend.exception.DuplicateResourceException
import com.openex.backend.repository.UserRepository
import com.openex.backend.security.JwtTokenProvider
import com.openex.backend.security.OpenExUserDetails
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider,
    private val walletService: WalletService,
) {
    private val log = LoggerFactory.getLogger(AuthService::class.java)

    /**
     * Register a new user account.
     * Creates the user and provisions default USD and BTC wallets.
     */
    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByUsername(request.username)) {
            throw DuplicateResourceException("Username '${request.username}' is already taken")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateResourceException("Email '${request.email}' is already registered")
        }

        val user = userRepository.save(
            User(
                username = request.username,
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password),
            )
        )
        log.info("Registered new user: ${user.username} (${user.id})")

        // Provision default wallets
        walletService.createDefaultWallets(user)

        // Log the new user in immediately
        return login(LoginRequest(request.username, request.password))
    }

    /**
     * Authenticate an existing user and return a JWT.
     */
    fun login(request: LoginRequest): AuthResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )

        val token = jwtTokenProvider.generateToken(authentication)
        val principal = authentication.principal as OpenExUserDetails

        log.info("User logged in: ${principal.username}")
        val user = userRepository.findByUsername(principal.username)!!

        return AuthResponse(
            token = token,
            username = user.username,
            email = user.email,
        )
    }
}
