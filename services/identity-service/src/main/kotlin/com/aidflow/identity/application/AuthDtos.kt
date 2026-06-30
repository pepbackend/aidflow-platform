package com.aidflow.identity.application

import com.aidflow.identity.domain.model.Role
import java.util.UUID

data class AuthUser(
    val id: UUID,
    val email: String,
    val roles: Set<Role>,
)

data class AuthResult(
    val accessToken: String,
    val user: AuthUser,
)

data class CurrentUser(
    val id: UUID,
    val email: String,
    val roles: Set<Role>,
)
