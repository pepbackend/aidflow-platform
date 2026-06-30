package com.aidflow.identity.infrastructure.security

import com.aidflow.identity.domain.errors.InvalidTokenException
import com.aidflow.identity.domain.model.Email
import com.aidflow.identity.domain.model.Role
import com.aidflow.identity.domain.model.User
import com.aidflow.identity.domain.ports.AuthenticatedUser
import com.aidflow.identity.domain.ports.TokenService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class JwtTokenService(
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
    @Value("\${auth.jwt.secret:dev-secret-dev-secret-dev-secret-dev-secret}")
    private val secret: String,
    @Value("\${auth.jwt.expiration-seconds:3600}")
    private val expirationSeconds: Long,
) : TokenService {
    override fun issue(user: User): String {
        val now = Instant.now(clock).epochSecond
        val header = mapOf("alg" to "HS256", "typ" to "JWT")
        val payload = mapOf(
            "sub" to user.id.toString(),
            "email" to user.email.value,
            "roles" to user.roles.map { it.name },
            "iat" to now,
            "exp" to now + expirationSeconds,
        )

        val encodedHeader = encodeJson(header)
        val encodedPayload = encodeJson(payload)
        val signature = sign("$encodedHeader.$encodedPayload")

        return "$encodedHeader.$encodedPayload.$signature"
    }

    override fun verify(token: String): AuthenticatedUser {
        val parts = token.split(".")
        if (parts.size != 3) {
            throw InvalidTokenException()
        }

        val signedContent = "${parts[0]}.${parts[1]}"
        if (sign(signedContent) != parts[2]) {
            throw InvalidTokenException()
        }

        val payload = runCatching {
            objectMapper.readTree(base64Decoder.decode(parts[1]))
        }.getOrElse { throw InvalidTokenException() }

        val expiresAt = payload["exp"]?.asLong() ?: throw InvalidTokenException()
        if (Instant.now(clock).epochSecond >= expiresAt) {
            throw InvalidTokenException()
        }

        return AuthenticatedUser(
            id = UUID.fromString(payload["sub"].asText()),
            email = Email.of(payload["email"].asText()),
            roles = payload["roles"].map { Role.valueOf(it.asText()) }.toSet(),
        )
    }

    private fun encodeJson(value: Any): String {
        return base64Encoder.encodeToString(objectMapper.writeValueAsBytes(value))
    }

    private fun sign(content: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return base64Encoder.encodeToString(mac.doFinal(content.toByteArray(StandardCharsets.UTF_8)))
    }

    private companion object {
        val base64Encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
        val base64Decoder: Base64.Decoder = Base64.getUrlDecoder()
    }
}
