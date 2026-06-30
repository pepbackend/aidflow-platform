package com.aidflow.identity.application

import com.aidflow.identity.domain.errors.EmailAlreadyRegisteredException
import com.aidflow.identity.domain.model.Email
import com.aidflow.identity.domain.model.Role
import com.aidflow.identity.domain.model.User
import com.aidflow.identity.domain.ports.PasswordHasher
import com.aidflow.identity.domain.ports.TokenService
import com.aidflow.identity.domain.ports.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenService: TokenService,
    private val clock: Clock,
) {
    @Transactional
    fun execute(email: String, password: String, role: Role): AuthResult {
        require(password.length >= 8) { "Password must have at least 8 characters" }

        val normalizedEmail = Email.of(email)
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw EmailAlreadyRegisteredException()
        }

        val user = User.register(
            id = UUID.randomUUID(),
            email = normalizedEmail,
            passwordHash = passwordHasher.hash(password),
            role = role,
            createdAt = Instant.now(clock),
        )

        val savedUser = userRepository.save(user)
        return savedUser.toAuthResult(tokenService.issue(savedUser))
    }
}
