package com.openex.backend.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.expiration-ms}") private val jwtExpirationMs: Long,
) {
    private val log = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    /** Generate a signed JWT for the authenticated principal. */
    fun generateToken(authentication: Authentication): String {
        val principal = authentication.principal as OpenExUserDetails
        val now = Date()
        val expiry = Date(now.time + jwtExpirationMs)

        return Jwts.builder()
            .subject(principal.username)
            .claim("userId", principal.userId.toString())
            .claim("role", principal.authorities.first().authority)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact()
    }

    /** Extract username from a valid JWT. */
    fun getUsernameFromToken(token: String): String =
        parseClaims(token).subject

    /** Validate the token signature and expiry. */
    fun validateToken(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (ex: JwtException) {
            log.warn("Invalid JWT: ${ex.message}")
            false
        } catch (ex: IllegalArgumentException) {
            log.warn("JWT claims string is empty: ${ex.message}")
            false
        }
    }

    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
