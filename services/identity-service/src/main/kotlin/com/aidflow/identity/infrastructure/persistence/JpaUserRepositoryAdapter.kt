package com.aidflow.identity.infrastructure.persistence

import com.aidflow.identity.domain.model.Email
import com.aidflow.identity.domain.model.User
import com.aidflow.identity.domain.ports.UserRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaUserRepositoryAdapter(
    private val springDataUserRepository: SpringDataUserRepository,
) : UserRepository {
    override fun existsByEmail(email: Email): Boolean {
        return springDataUserRepository.existsByEmail(email.value)
    }

    override fun findByEmail(email: Email): User? {
        return springDataUserRepository.findByEmail(email.value)?.toDomain()
    }

    override fun findById(id: UUID): User? {
        return springDataUserRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun save(user: User): User {
        return springDataUserRepository.save(UserJpaEntity.fromDomain(user)).toDomain()
    }
}
