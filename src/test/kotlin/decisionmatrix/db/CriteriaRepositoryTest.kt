package decisionmatrix.db

import decisionmatrix.CriteriaInput
import decisionmatrix.DecisionInput
import decisionmatrix.Criteria
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class CriteriaRepositoryTest {

    val jdbi = createTestJdbi()

    @Test
    fun insert_and_findById() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val criteria = criteriaRepository.insert(CriteriaInput(decisionId = decision.id, name = "Cost", weight = 5))
        val found = criteriaRepository.findById(criteria.id)
        found shouldNotBe null
        found shouldBe Criteria(id = criteria.id, decisionId = decision.id, name = "Cost", weight = 5)
    }
}
