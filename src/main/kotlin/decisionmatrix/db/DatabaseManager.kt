package decisionmatrix.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi

fun loadJdbi(
    host: String = System.getenv("DB_HOST") ?: "localhost",
    port: String = System.getenv("DB_PORT") ?: "5432",
    database: String = System.getenv("DB_NAME") ?: "decision_matrix",
    username: String = System.getenv("DB_USER") ?: "decision_matrix",
    password: String = System.getenv("DB_PASSWORD") ?: "decision_matrix_password"
): Jdbi {
    val jdbcUrl = "jdbc:postgresql://$host:$port/$database"
    
    Flyway.configure()
        .dataSource(jdbcUrl, username, password)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    val config = HikariConfig()
    config.jdbcUrl = jdbcUrl
    config.username = username
    config.password = password
    config.maximumPoolSize = 10
    config.minimumIdle = 2
    config.connectionTimeout = 30000
    config.idleTimeout = 600000
    config.maxLifetime = 1800000

    val dataSource = HikariDataSource(config)
    val jdbi = Jdbi.create(dataSource)
    return jdbi
}
