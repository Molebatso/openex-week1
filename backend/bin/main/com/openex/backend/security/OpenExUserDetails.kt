package com.openex.backend.security

import com.openex.backend.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

data class OpenExUserDetails(
    val userId: UUID,
    private val usernameVal: String,
    private val passwordVal: String,
    private val authoritiesVal: Collection<GrantedAuthority>,
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = authoritiesVal
    override fun getPassword(): String = passwordVal
    override fun getUsername(): String = usernameVal
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    companion object {
        fun from(user: User) = OpenExUserDetails(
            userId = user.id,
            usernameVal = user.username,
            passwordVal = user.passwordHash,
            authoritiesVal = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")),
        )
    }
}
