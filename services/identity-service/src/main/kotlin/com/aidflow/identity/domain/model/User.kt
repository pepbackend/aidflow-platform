package com.aidflow.identity.domain.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: Email,
    val passwordHash: String,
    val roles: Set<Role>,
    val createdAt: Instant,
) {
    fun hasRole(role: Role): Boolean = role in roles

    companion object {
        fun register(
            id: UUID,
            email: Email,
            passwordHash: String,
            role: Role,
            createdAt: Instant,
        ): User {
            require(passwordHash.isNotBlank()) { "Password hash must not be blank" }

            return User(
                id = id,
                email = email,
                passwordHash = passwordHash,
                roles = setOf(role),
                createdAt = createdAt,
            )
        }
    }
}
