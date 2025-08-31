package decisionmatrix.db

import decisionmatrix.Criteria
import decisionmatrix.Decision
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class CriteriaRepositoryTest {

    val jdbi = createTestJdbi()

    @Test
    fun insert_and_findById() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(Decision(name = "My decision", criteria = emptyList(), options = emptyList()))
        requireNotNull(decision.id)

        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val criteria = criteriaRepository.insert(Criteria(decisionId = decision.id, name = "Cost", weight = 5))
        requireNotNull(criteria.id)

        val found = criteriaRepository.findById(criteria.id)

        assertNotNull(found)
        assertEquals(Criteria(id = criteria.id, decisionId = decision.id, name = "Cost", weight = 5), found)
    }
}
