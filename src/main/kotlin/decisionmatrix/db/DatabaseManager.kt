package decisionmatrix.db

import org.jdbi.v3.core.Jdbi
import java.io.File
import kotlin.io.path.createTempFile

fun loadDatabase(databasePath: File = File("/tmp/decision_matrix.sqlite")): Jdbi {
    val jdbi = Jdbi.create("jdbc:sqlite:${databasePath.path}")
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
                    name TEXT NOT NULL
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
                CREATE TABLE IF NOT EXISTS option_scores (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    option_id INTEGER NOT NULL,
                    score INTEGER NOT NULL,
                    FOREIGN KEY(option_id) REFERENCES options(id)
                )
                """.trimIndent()
        )
    }
}