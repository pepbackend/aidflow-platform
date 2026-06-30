package com.aidflow.identity.infrastructure.http.dto

import com.aidflow.identity.application.AuthResult
import com.aidflow.identity.application.CurrentUser
import java.util.UUID

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val user: UserResponse,
) {
    companion object {
        fun from(result: AuthResult): AuthResponse {
            return AuthResponse(
                accessToken = result.accessToken,
                user = UserResponse(
                    id = result.user.id,
                    email = result.user.email,
                    roles = result.user.roles.map { it.name },
                ),
            )
        }
    }
}

data class UserResponse(
    val id: UUID,
    val email: String,
    val roles: List<String>,
)

data class MeResponse(
    val id: UUID,
    val email: String,
    val roles: List<String>,
) {
    companion object {
        fun from(currentUser: CurrentUser): MeResponse {
            return MeResponse(
                id = currentUser.id,
                email = currentUser.email,
                roles = currentUser.roles.map { it.name },
            )
        }
    }
}
