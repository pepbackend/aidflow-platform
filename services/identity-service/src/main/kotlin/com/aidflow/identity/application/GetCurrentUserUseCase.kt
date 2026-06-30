package com.aidflow.identity.application

import com.aidflow.identity.domain.errors.UserNotFoundException
import com.aidflow.identity.domain.ports.TokenService
import com.aidflow.identity.domain.ports.UserRepository
import org.springframework.stereotype.Service

@Service
class GetCurrentUserUseCase(
    private val userRepository: UserRepository,
    private val tokenService: TokenService,
) {
    fun execute(token: String): CurrentUser {
        val authenticatedUser = tokenService.verify(token)
        val user = userRepository.findById(authenticatedUser.id)
            ?: throw UserNotFoundException()

        return CurrentUser(
            id = user.id,
            email = user.email.value,
            roles = user.roles,
        )
    }
}
