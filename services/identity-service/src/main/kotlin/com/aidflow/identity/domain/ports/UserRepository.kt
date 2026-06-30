package com.aidflow.identity.domain.ports

import com.aidflow.identity.domain.model.Email
import com.aidflow.identity.domain.model.User
import java.util.UUID

interface UserRepository {
    fun existsByEmail(email: Email): Boolean
    fun findByEmail(email: Email): User?
    fun findById(id: UUID): User?
    fun save(user: User): User
}
