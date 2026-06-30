package com.aidflow.identity.acceptance

import com.aidflow.identity.support.PostgresTest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowAcceptanceTest : PostgresTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `user can register login and resolve current user`() {
        val email = "flow-${UUID.randomUUID()}@example.com"

        val registerResponse = postJson(
            "/auth/register",
            mapOf("email" to email, "password" to "secret123"),
        )

        val registeredToken = registerResponse["accessToken"].asText()
        assertNotNull(registeredToken)
        assertEquals(email, registerResponse["user"]["email"].asText())
        assertEquals("USER", registerResponse["user"]["roles"][0].asText())

        val loginResponse = postJson(
            "/auth/login",
            mapOf("email" to email, "password" to "secret123"),
        )

        val loginToken = loginResponse["accessToken"].asText()
        assertNotNull(loginToken)

        val meResponse = mockMvc.perform(
            get("/internal/auth/me")
                .header("Authorization", "Bearer $loginToken"),
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it) }

        assertEquals(registerResponse["user"]["id"].asText(), meResponse["id"].asText())
        assertEquals(email, meResponse["email"].asText())
        assertEquals("USER", meResponse["roles"][0].asText())
    }

    private fun postJson(path: String, body: Map<String, String>): JsonNode {
        return mockMvc.perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it) }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun authProperties(registry: DynamicPropertyRegistry) {
            registry.add("auth.jwt.secret") { "test-secret-test-secret-test-secret-test-secret" }
        }
    }
}
