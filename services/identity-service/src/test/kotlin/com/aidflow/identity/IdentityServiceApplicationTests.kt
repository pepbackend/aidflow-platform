package com.aidflow.identity

import com.aidflow.identity.support.PostgresTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
class IdentityServiceApplicationTests : PostgresTest() {

	@Test
	fun contextLoads() {
	}

	companion object {
		@JvmStatic
		@DynamicPropertySource
		fun authProperties(registry: DynamicPropertyRegistry) {
			registry.add("auth.jwt.secret") { "test-secret-test-secret-test-secret-test-secret" }
		}
	}
}
