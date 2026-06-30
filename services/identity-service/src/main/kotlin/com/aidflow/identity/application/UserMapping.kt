package com.aidflow.identity.application

import com.aidflow.identity.domain.model.User

fun User.toAuthResult(accessToken: String): AuthResult {
    return AuthResult(
        accessToken = accessToken,
        user = AuthUser(
            id = id,
            email = email.value,
            roles = roles,
        ),
    )
}
