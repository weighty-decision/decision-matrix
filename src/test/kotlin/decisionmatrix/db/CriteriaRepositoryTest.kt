package decisionmatrix.db

import decisionmatrix.Criteria
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class CriteriaRepositoryTest {

    private fun jdbi(): Jdbi =
        Jdbi.create("jdbc:sqlite:file:memdb_criteria?mode=memory&cache=shared")

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
                CREATE TABLE IF NOT EXISTS criteria (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    decision_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    weight INTEGER NOT NULL,
                    FOREIGN KEY(decision_id) REFERENCES decisions(id)
                )
                """.trimIndent()
            )
        }
    }

    @Test
    fun insert_and_findById() {
        val jdbi = jdbi()
        createSchema(jdbi)

        // Seed a decision to reference
        val decisionId = jdbi.withHandle<Long, Exception> { handle ->
            handle.createUpdate("INSERT INTO decisions (name) VALUES (:name)")
                .bind("name", "Seed decision")
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Long::class.javaObjectType)
                .one()
        }

        val repo = CriteriaRepository(jdbi)
        val id = repo.insert(Criteria(decisionId = decisionId, name = "Cost", weight = 5))
        val found = repo.findById(id)

        assertNotNull(found)
        assertEquals(Criteria(id = id, decisionId = decisionId, name = "Cost", weight = 5), found)
    }
}
