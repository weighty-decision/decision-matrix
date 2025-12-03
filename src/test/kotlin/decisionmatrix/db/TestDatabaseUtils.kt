package decisionmatrix.db

import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import java.sql.DriverManager
import java.sql.SQLException

fun getTestJdbi(): Jdbi {
    val host = "localhost"
    val port = "5432"
    val testDbName = "decision_matrix_test"
    val dbUsername = "decision_matrix"
    val dbPassword = "decision_matrix_password"
    val adminJdbcUrl = "jdbc:postgresql://$host:$port/decision_matrix"
    val testJdbcUrl = "jdbc:postgresql://$host:$port/$testDbName"

    // Ensure test database exists
    ensureTestDatabaseExists(adminJdbcUrl, testDbName, dbUsername, dbPassword)

    // Run migrations if needed
    runMigrationsIfNeeded(testJdbcUrl, dbUsername, dbPassword)

    val dataSource = PGSimpleDataSource().apply {
        setUrl(testJdbcUrl)
        user = dbUsername
        password = dbPassword
    }
    return Jdbi.create(dataSource)
}

fun cleanTestDatabase() {
    val host = "localhost"
    val port = "5432"
    val testDbName = "decision_matrix_test"
    val dbUsername = "decision_matrix"
    val dbPassword = "decision_matrix_password"
    val testJdbcUrl = "jdbc:postgresql://$host:$port/$testDbName"

    try {
        DriverManager.getConnection(testJdbcUrl, dbUsername, dbPassword).use { connection ->
            val cleanupStatements = listOf(
                "TRUNCATE user_scores, criteria, options, decisions, decision_tags, tags CASCADE"
            )

            connection.createStatement().use { statement ->
                for (sql in cleanupStatements) {
                    statement.executeUpdate(sql)
                }
            }
        }
    } catch (e: SQLException) {
        throw RuntimeException("Failed to clean test database: ${e.message}", e)
    }
}

private fun ensureTestDatabaseExists(
    adminJdbcUrl: String,
    testDbName: String,
    dbUsername: String,
    dbPassword: String
) {
    try {
        DriverManager.getConnection(adminJdbcUrl, dbUsername, dbPassword).use { connection ->
            val checkDbQuery = "SELECT 1 FROM pg_database WHERE datname = ?"
            connection.prepareStatement(checkDbQuery).use { statement ->
                statement.setString(1, testDbName)
                val resultSet = statement.executeQuery()
                
                if (!resultSet.next()) {
                    connection.createStatement().use { createStatement ->
                        createStatement.executeUpdate("CREATE DATABASE $testDbName")
                    }
                    println("Created test database: $testDbName")
                }
            }
        }
    } catch (e: SQLException) {
        throw RuntimeException("Failed to ensure test database exists: ${e.message}", e)
    }
}

private fun runMigrationsIfNeeded(jdbcUrl: String, dbUsername: String, dbPassword: String) {
    try {
        val flyway = Flyway.configure()
            .dataSource(jdbcUrl, dbUsername, dbPassword)
            .locations("classpath:db/migration")
            .load()

        // Check if migrations are needed
        val info = flyway.info()
        val pendingMigrations = info.pending()
        
        if (pendingMigrations.isNotEmpty()) {
            println("Running ${pendingMigrations.size} pending database migrations for tests")
            flyway.migrate()
        }
    } catch (e: Exception) {
        throw RuntimeException("Failed to run database migrations: ${e.message}", e)
    }
}
