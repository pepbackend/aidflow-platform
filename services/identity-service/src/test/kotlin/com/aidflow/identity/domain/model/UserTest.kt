package com.aidflow.identity.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UserTest {

    @Test
    fun `registers a user with default USER role`() {
        val id = UUID.fromString("d84cd8cb-356b-49de-bbec-a0bc705e7a0e")
        val createdAt = Instant.parse("2026-06-29T10:00:00Z")

        val user = User.register(
            id = id,
            email = Email.of("person@example.com"),
            passwordHash = "hashed-password",
            createdAt = createdAt,
        )

        assertEquals(id, user.id)
        assertEquals("person@example.com", user.email.value)
        assertEquals("hashed-password", user.passwordHash)
        assertEquals(createdAt, user.createdAt)
        assertEquals(setOf(Role.USER), user.roles)
        assertTrue(user.hasRole(Role.USER))
    }
}
