package com.aidflow.identity.infrastructure.persistence

import com.aidflow.identity.domain.model.Email
import com.aidflow.identity.domain.model.Role
import com.aidflow.identity.domain.model.User
import com.aidflow.identity.support.PostgresTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaUserRepositoryAdapter::class)
class JpaUserRepositoryAdapterTest : PostgresTest() {

    @Autowired
    private lateinit var userRepository: JpaUserRepositoryAdapter

    @Test
    fun `saves and finds user by email and id`() {
        val id = UUID.randomUUID()
        val email = Email.of("saved-$id@example.com")
        val user = User(
            id = id,
            email = email,
            passwordHash = "hash",
            roles = setOf(Role.VOLUNTEER),
            createdAt = Instant.parse("2026-06-29T10:00:00Z"),
        )

        userRepository.save(user)

        val byEmail = userRepository.findByEmail(email)
        val byId = userRepository.findById(user.id)

        assertNotNull(byEmail)
        assertNotNull(byId)
        assertEquals(user.id, byEmail?.id)
        assertEquals(setOf(Role.VOLUNTEER), byEmail?.roles)
        assertTrue(userRepository.existsByEmail(email))
        assertFalse(userRepository.existsByEmail(Email.of("missing@example.com")))
    }
}
