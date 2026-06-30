package com.aidflow.identity.application

import com.aidflow.identity.domain.errors.InvalidCredentialsException
import com.aidflow.identity.domain.model.Email
import com.aidflow.identity.domain.ports.PasswordHasher
import com.aidflow.identity.domain.ports.TokenService
import com.aidflow.identity.domain.ports.UserRepository
import org.springframework.stereotype.Service

@Service
class LoginUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenService: TokenService,
) {
    fun execute(email: String, password: String): AuthResult {
        val user = userRepository.findByEmail(Email.of(email))
            ?: throw InvalidCredentialsException()

        if (!passwordHasher.matches(password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        return user.toAuthResult(tokenService.issue(user))
    }
}
