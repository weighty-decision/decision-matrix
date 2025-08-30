package decisionmatrix.db

import decisionmatrix.Option
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class OptionRepositoryTest {

    private fun jdbi(): Jdbi =
        Jdbi.create("jdbc:sqlite:file:memdb_option?mode=memory&cache=shared")

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

        val repo = OptionRepository(jdbi)
        val id = repo.insert(Option(decisionId = decisionId, name = "Option A"))
        val found = repo.findById(id)

        assertNotNull(found)
        assertEquals(Option(id = id, decisionId = decisionId, name = "Option A"), found)
    }
}
