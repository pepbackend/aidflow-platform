package com.aidflow.identity.infrastructure.http

import com.aidflow.identity.domain.errors.EmailAlreadyRegisteredException
import com.aidflow.identity.domain.errors.InvalidCredentialsException
import com.aidflow.identity.domain.errors.InvalidTokenException
import com.aidflow.identity.domain.errors.UserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class HttpExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class, MethodArgumentNotValidException::class)
    fun badRequest(exception: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = "bad_request", message = exception.message ?: "Bad request"))
    }

    @ExceptionHandler(InvalidCredentialsException::class, InvalidTokenException::class, UserNotFoundException::class)
    fun unauthorized(exception: RuntimeException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(error = "unauthorized", message = exception.message ?: "Unauthorized"))
    }

    @ExceptionHandler(EmailAlreadyRegisteredException::class)
    fun conflict(exception: EmailAlreadyRegisteredException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = "email_already_registered", message = exception.message ?: "Conflict"))
    }
}

data class ErrorResponse(
    val error: String,
    val message: String,
)
