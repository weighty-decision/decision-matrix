package decisionmatrix.db

import org.jdbi.v3.core.Jdbi
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createTempFile

fun loadDatabase(databasePath: Path = Paths.get(System.getProperty("user.home"), "decision_matrix.sqlite")): Jdbi {
    val jdbi = Jdbi.create("jdbc:sqlite:$databasePath")
    jdbi.useHandle<Exception> { it.execute("PRAGMA foreign_keys = ON") }
    if (!schemaExists(jdbi)) {
        createSchema(jdbi)
    }
    return jdbi
}

private fun schemaExists(jdbi: Jdbi): Boolean {
    return jdbi.withHandle<Boolean, Exception> { handle ->
        handle.createQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='decisions'")
            .mapTo(String::class.java)
            .findFirst()
            .isPresent
    }
}

fun createTempDatabase(): Jdbi {
    val tempDbFile = createTempFile("decision_matrix", ".db").toFile().apply {
        println("Using database at $absolutePath")
        deleteOnExit()
    }
    val jdbi = Jdbi.create("jdbc:sqlite:${tempDbFile.absolutePath}")
    jdbi.useHandle<Exception> { it.execute("PRAGMA foreign_keys = ON") }
    createSchema(jdbi)

    return jdbi
}

fun createSchema(jdbi: Jdbi) {
    jdbi.useHandle<Exception> { handle ->
        handle.execute(
            """
                CREATE TABLE IF NOT EXISTS decisions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    min_score INTEGER NOT NULL,
                    max_score INTEGER NOT NULL,
                    created_by TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.trimIndent()
        )
        handle.execute(
            """
                CREATE TABLE IF NOT EXISTS criteria (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    decision_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    weight INTEGER NOT NULL,
                    FOREIGN KEY(decision_id) REFERENCES decisions(id)
                )
                """.trimIndent()
        )
        handle.execute(
            """
                CREATE TABLE IF NOT EXISTS options (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    decision_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    FOREIGN KEY(decision_id) REFERENCES decisions(id)
                )
                """.trimIndent()
        )
        handle.execute(
            """
                CREATE TABLE IF NOT EXISTS user_scores (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    decision_id INTEGER NOT NULL,
                    option_id INTEGER NOT NULL,
                    criteria_id INTEGER NOT NULL,
                    score INTEGER NOT NULL,
                    scored_by TEXT NOT NULL,
                    FOREIGN KEY(decision_id) REFERENCES decisions(id)
                    FOREIGN KEY(option_id) REFERENCES options(id)
                    FOREIGN KEY(criteria_id) REFERENCES criteria(id)
                )
                """.trimIndent()
        )
    }
}
