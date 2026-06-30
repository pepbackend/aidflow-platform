package com.aidflow.identity.infrastructure.security

import com.aidflow.identity.domain.ports.PasswordHasher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordHasher : PasswordHasher {
    private val encoder = BCryptPasswordEncoder()

    override fun hash(rawPassword: String): String {
        return encoder.encode(rawPassword)
    }

    override fun matches(rawPassword: String, passwordHash: String): Boolean {
        return encoder.matches(rawPassword, passwordHash)
    }
}
