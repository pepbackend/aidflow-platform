package com.aidflow.identity.domain.model

@JvmInline
value class Email private constructor(val value: String) {
    companion object {
        private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        fun of(rawValue: String): Email {
            val normalized = rawValue.trim().lowercase()
            require(normalized.isNotBlank()) { "Email must not be blank" }
            require(emailPattern.matches(normalized)) { "Email is invalid" }
            return Email(normalized)
        }
    }
}
