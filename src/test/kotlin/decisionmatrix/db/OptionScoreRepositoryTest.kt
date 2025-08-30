package decisionmatrix.db

import decisionmatrix.OptionScore
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class OptionScoreRepositoryTest {

    private fun jdbi(): Jdbi =
        Jdbi.create("jdbc:sqlite:file:memdb_option_score?mode=memory&cache=shared")

    private fun createSchema(jdbi: Jdbi) {
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

    @Test
    fun insert_and_findById() {
        val jdbi = jdbi()
        createSchema(jdbi)

        // Seed a decision and option to reference
        val decisionId = jdbi.withHandle<Long, Exception> { handle ->
            handle.createUpdate("INSERT INTO decisions (name) VALUES (:name)")
                .bind("name", "Seed decision")
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Long::class.javaObjectType)
                .one()
        }
        val optionId = jdbi.withHandle<Long, Exception> { handle ->
            handle.createUpdate("INSERT INTO options (decision_id, name) VALUES (:decisionId, :name)")
                .bind("decisionId", decisionId)
                .bind("name", "Option A")
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Long::class.javaObjectType)
                .one()
        }

        val repo = OptionScoreRepository(jdbi)
        val id = repo.insert(OptionScore(optionId = optionId, score = 9))
        val found = repo.findById(id)

        assertNotNull(found)
        assertEquals(OptionScore(id = id, optionId = optionId, score = 9), found)
    }
}
