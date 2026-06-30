package com.aidflow.identity.domain.errors

class EmailAlreadyRegisteredException : RuntimeException("Email is already registered")

class InvalidCredentialsException : RuntimeException("Invalid credentials")

class InvalidTokenException : RuntimeException("Invalid token")

class UserNotFoundException : RuntimeException("User not found")
