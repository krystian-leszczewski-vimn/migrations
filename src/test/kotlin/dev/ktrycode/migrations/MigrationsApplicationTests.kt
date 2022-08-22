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
import org.testcontainers.containers.BindMode.READ_ONLY
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@ContextConfiguration(initializers = [DevInitializer::class])
class DevMigrationTest : MigrationsApplicationTests("dev")

@ContextConfiguration(initializers = [QaInitializer::class])
class QaMigrationTest : MigrationsApplicationTests("qa")

@ContextConfiguration(initializers = [ProdInitializer::class])
class ProdMigrationTest : MigrationsApplicationTests("prod")

@Testcontainers
@JdbcTest
@AutoConfigureTestDatabase(replace = NONE)
@ContextConfiguration(initializers = [CommonInitializer::class])
abstract class MigrationsApplicationTests(
    val env: String
) {
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

    private companion object : KLogging() {
        const val DATABASE_NAME = "migrations"
        const val CONTAINER_NAME = "${DATABASE_NAME}_test_db"

        private const val dumpFilenameTemplate = "B7__{{env}}_dump.pgsql"
        private fun createDumpFilename(env: String) = dumpFilenameTemplate.replace("{{env}}", env)

        @Container
        private val POSTGRES: KPostgresContainer = createContainer().apply {
            start()
            importDump()
        }

        private fun createContainer(env: String) = KPostgresContainer("11.8")
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

    open class CommonInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            val values = TestPropertyValues.of(
                "spring.datasource.url=" + POSTGRES.jdbcUrl,
                "spring.datasource.username=user",
                "spring.datasource.password=password",
                "spring.flyway.user=user",
                "spring.flyway.password=password",
                "spring.datasource.hikari.maximum-pool-size=20"
            )
            values.applyTo(configurableApplicationContext)
        }
    }

    class DevInitializer : EnvSpecificInitializer("dev")

    class QaInitializer : EnvSpecificInitializer("qa")

    class ProdInitializer : EnvSpecificInitializer("prod")

    open class EnvSpecificInitializer(private val flywayEnvironmentPlaceholder: String) :
        ApplicationContextInitializer<ConfigurableApplicationContext> {

        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            val values = TestPropertyValues.of(
                "spring.flyway.placeholders.env=$flywayEnvironmentPlaceholder"
            )
            values.applyTo(configurableApplicationContext)
        }
    }
}

class KPostgresContainer(
    postgresVersion: String
) : PostgreSQLContainer<KPostgresContainer>("postgres:$postgresVersion")