package com.aidflow.identity.infrastructure.http

import com.aidflow.identity.application.GetCurrentUserUseCase
import com.aidflow.identity.domain.errors.InvalidTokenException
import com.aidflow.identity.infrastructure.http.dto.MeResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalAuthController(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
) {
    @GetMapping("/internal/auth/me")
    fun me(@RequestHeader("Authorization", required = false) authorization: String?): MeResponse {
        val token = authorization
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?: throw InvalidTokenException()

        return MeResponse.from(getCurrentUserUseCase.execute(token))
    }
}
