package com.aidflow.identity.domain.ports

import com.aidflow.identity.domain.model.Email
import com.aidflow.identity.domain.model.Role
import com.aidflow.identity.domain.model.User
import java.util.UUID

data class AuthenticatedUser(
    val id: UUID,
    val email: Email,
    val roles: Set<Role>,
)

interface TokenService {
    fun issue(user: User): String
    fun verify(token: String): AuthenticatedUser
}
