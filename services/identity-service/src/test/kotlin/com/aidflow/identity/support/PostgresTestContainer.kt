package com.aidflow.identity.support

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

private class AidflowPostgreSQLContainer(imageName: String) :
    PostgreSQLContainer<AidflowPostgreSQLContainer>(imageName)

object PostgresTestContainer {
    val instance: PostgreSQLContainer<*> = AidflowPostgreSQLContainer("postgres:16-alpine")
        .withDatabaseName("aidflow")
        .withUsername("aidflow")
        .withPassword("aidflow")
        .also { it.start() }
}

abstract class PostgresTest {
    companion object {
        private val postgres = PostgresTestContainer.instance

        @JvmStatic
        @DynamicPropertySource
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
