package dev.ktrycode.migrations

import dev.ktrycode.migrations.MigrationsApplicationTests.*
import mu.KLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.BindMode.READ_ONLY
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@ContextConfiguration(initializers = [DevInitializer::class])
class DevMigrationTest : MigrationsApplicationTests() {
    companion object {
        @Container
        private val POSTGRES = setupContainer("dev")

        @JvmStatic
        @DynamicPropertySource
        fun addDatasourceUrl(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl)
        }
    }
}

@ContextConfiguration(initializers = [QaInitializer::class])
class QaMigrationTest : MigrationsApplicationTests() {
    companion object {
        @Container
        private val POSTGRES = setupContainer("qa")

        @JvmStatic
        @DynamicPropertySource
        fun addDatasourceUrl(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl)
        }
    }
}

@ContextConfiguration(initializers = [ProdInitializer::class])
class ProdMigrationTest : MigrationsApplicationTests() {
    companion object {
        @Container
        private val POSTGRES = setupContainer("prod")

        @JvmStatic
        @DynamicPropertySource
        fun addDatasourceUrl(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl)
        }
    }
}

@Testcontainers
@JdbcTest
@AutoConfigureTestDatabase(replace = NONE)
abstract class MigrationsApplicationTests {

    @Value("\${env}")
    private lateinit var env: String

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Test
    fun shouldRunMigrationsAndCreateEntityManager() {
        val rowsCount = jdbcTemplate.queryForObject(
            """
                SELECT count(*) FROM migrations.test_table
                    WHERE env = :env
                """,
            mapOf<String, String>("env" to env),
            Int::class.java
        )

        assertThat(rowsCount).isEqualTo(1)
    }

    companion object : KLogging() {
        private const val DATABASE_NAME = "migrations"
        private const val CONTAINER_NAME = "${DATABASE_NAME}_test_db"

        private const val dumpFilenameTemplate = "B7__{{env}}_dump.pgsql"
        private fun createDumpFilename(env: String) = dumpFilenameTemplate.replace("{{env}}", env)

        fun setupContainer(env: String) = createContainer(env).apply {
            start()
            importDump()
        }

        private fun createContainer(env: String): KPostgresContainer = KPostgresContainer("11.8")
            .withCreateContainerCmdModifier { it.withName(CONTAINER_NAME) }
            .withDatabaseName(DATABASE_NAME)
            .withUsername("user")
            .withPassword("password")
            .withClasspathResourceMapping("db/${createDumpFilename(env)}", "/snapshot.pgsql", READ_ONLY)
            .withInitScript("db/init-tests.sql")

        private fun KPostgresContainer.importDump() {
            execInContainer("sh", "-c", "psql -U user migrations < snapshot.pgsql")
        }
    }

    class DevInitializer : EnvSpecificInitializer("dev")
    class QaInitializer : EnvSpecificInitializer("qa")
    class ProdInitializer : EnvSpecificInitializer("prod")

    open class EnvSpecificInitializer(private val env: String) : CommonInitializer() {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            super.initialize(configurableApplicationContext)
            val values = TestPropertyValues.of("env=$env")
            values.applyTo(configurableApplicationContext)
        }
    }

    open class CommonInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            val values = TestPropertyValues.of(
                "spring.datasource.username=user",
                "spring.datasource.password=password",
                "spring.flyway.user=user",
                "spring.flyway.password=password",
                "spring.datasource.hikari.maximum-pool-size=20"
            )
            values.applyTo(configurableApplicationContext)
        }
    }
}

class KPostgresContainer(
    postgresVersion: String
) : PostgreSQLContainer<KPostgresContainer>("postgres:$postgresVersion")