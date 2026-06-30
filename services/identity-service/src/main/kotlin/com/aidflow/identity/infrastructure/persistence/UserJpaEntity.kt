package com.aidflow.identity.infrastructure.persistence

import com.aidflow.identity.domain.model.Email
import com.aidflow.identity.domain.model.Role
import com.aidflow.identity.domain.model.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserJpaEntity(
    @Id
    var id: UUID? = null,

    @Column(nullable = false, unique = true)
    var email: String = "",

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",

    @Column(nullable = false)
    var roles: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.EPOCH,
) {
    fun toDomain(): User {
        return User(
            id = requireNotNull(id),
            email = Email.of(email),
            passwordHash = passwordHash,
            roles = roles.split(",")
                .filter { it.isNotBlank() }
                .map { Role.valueOf(it.trim()) }
                .toSet(),
            createdAt = createdAt,
        )
    }

    companion object {
        fun fromDomain(user: User): UserJpaEntity {
            return UserJpaEntity(
                id = user.id,
                email = user.email.value,
                passwordHash = user.passwordHash,
                roles = user.roles.joinToString(",") { it.name },
                createdAt = user.createdAt,
            )
        }
    }
}
