package com.aidflow.identity.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class EmailTest {

    @Test
    fun `normalizes email by trimming and lowercasing`() {
        val email = Email.of("  USER@Example.COM  ")

        assertEquals("user@example.com", email.value)
    }

    @Test
    fun `rejects blank email`() {
        assertThrows(IllegalArgumentException::class.java) {
            Email.of(" ")
        }
    }

    @Test
    fun `rejects invalid email`() {
        assertThrows(IllegalArgumentException::class.java) {
            Email.of("not-an-email")
        }
    }
}
