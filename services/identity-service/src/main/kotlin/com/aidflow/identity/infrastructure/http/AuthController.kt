package com.aidflow.identity.infrastructure.http

import com.aidflow.identity.application.LoginUseCase
import com.aidflow.identity.application.RegisterUserUseCase
import com.aidflow.identity.infrastructure.http.dto.AuthResponse
import com.aidflow.identity.infrastructure.http.dto.LoginRequest
import com.aidflow.identity.infrastructure.http.dto.RegisterRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUseCase: LoginUseCase,
) {
    @PostMapping("/auth/register")
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse {
        return AuthResponse.from(registerUserUseCase.execute(request.email, request.password))
    }

    @PostMapping("/auth/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        return AuthResponse.from(loginUseCase.execute(request.email, request.password))
    }
}
