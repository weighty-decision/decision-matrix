package decisionmatrix.db

import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi

fun loadDatabase(
    host: String = System.getenv("DB_HOST") ?: "localhost",
    port: String = System.getenv("DB_PORT") ?: "5432",
    database: String = System.getenv("DB_NAME") ?: "decision_matrix",
    username: String = System.getenv("DB_USER") ?: "decision_matrix",
    password: String = System.getenv("DB_PASSWORD") ?: "decision_matrix_password"
): Jdbi {
    val jdbcUrl = "jdbc:postgresql://$host:$port/$database"
    
    // Run Flyway migrations
    val flyway = Flyway.configure()
        .dataSource(jdbcUrl, username, password)
        .locations("classpath:db/migration")
        .load()
    flyway.migrate()
    
    return Jdbi.create(jdbcUrl, username, password)
}

fun createTempDatabase(): Jdbi {
    // For testing, use a test database on the same PostgreSQL instance
    val testDatabase = "decision_matrix_test_${System.currentTimeMillis()}"
    val host = System.getenv("DB_HOST") ?: "localhost"
    val port = System.getenv("DB_PORT") ?: "5432"
    val username = System.getenv("DB_USER") ?: "decision_matrix"
    val password = System.getenv("DB_PASSWORD") ?: "decision_matrix_password"
    
    // First connect to the default postgres database to create the test database
    val adminJdbi = Jdbi.create("jdbc:postgresql://$host:$port/postgres", username, password)
    adminJdbi.useHandle<Exception> { handle ->
        handle.execute("CREATE DATABASE \"$testDatabase\"")
    }
    
    val testJdbcUrl = "jdbc:postgresql://$host:$port/$testDatabase"
    
    // Run Flyway migrations on the test database
    val flyway = Flyway.configure()
        .dataSource(testJdbcUrl, username, password)
        .locations("classpath:db/migration")
        .load()
    flyway.migrate()
    
    println("Using PostgreSQL test database: $testDatabase")
    return Jdbi.create(testJdbcUrl, username, password)
}
