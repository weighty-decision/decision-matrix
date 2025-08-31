package decisionmatrix.db

import decisionmatrix.Decision
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class DecisionRepositoryTest {

    val jdbi = createTestJdbi()

    @Test
    fun insert_and_findById() {
        val repo = DecisionRepository(jdbi)
        val inserted = repo.insert(Decision(name = "My decision", criteria = emptyList(), options = emptyList()))
        requireNotNull(inserted.id)
        val found = repo.findById(inserted.id)

        assertNotNull(found)
        assertEquals(
            Decision(id = inserted.id, name = "My decision", criteria = emptyList(), options = emptyList()),
            found
        )
    }
}
