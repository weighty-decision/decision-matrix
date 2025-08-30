package decisionmatrix.db

import decisionmatrix.Decision
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class DecisionRepositoryTest {

    private fun jdbi(): Jdbi =
        Jdbi.create("jdbc:sqlite:file:memdb_decision?mode=memory&cache=shared")

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
        }
    }

    @Test
    fun insert_and_findById() {
        val jdbi = jdbi()
        createSchema(jdbi)

        val repo = DecisionRepository(jdbi)
        val id = repo.insert(Decision(name = "My decision", criteria = emptyList(), options = emptyList()))
        val found = repo.findById(id)

        assertNotNull(found)
        assertEquals(Decision(id = id, name = "My decision", criteria = emptyList(), options = emptyList()), found)
    }
}
