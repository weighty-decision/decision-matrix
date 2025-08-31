package decisionmatrix.db

import decisionmatrix.Decision
import decisionmatrix.Option
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class OptionRepositoryTest {

    val jdbi = createTestJdbi()

    @Test
    fun insert_and_findById() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(
            Decision(name = "My decision", criteria = emptyList(), options = emptyList())
        )
        requireNotNull(decision.id)

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(Option(decisionId = decision.id, name = "Option A"))
        requireNotNull(option.id)
        val found = optionRepository.findById(option.id)

        assertNotNull(found)
        assertEquals(Option(id = option.id, decisionId = decision.id, name = "Option A"), found)
    }
}
