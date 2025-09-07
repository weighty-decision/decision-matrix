package decisionmatrix.db

import decisionmatrix.Criteria
import decisionmatrix.CriteriaInput
import decisionmatrix.DecisionInput
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class CriteriaRepositoryTest {

    val jdbi = getTestJdbi()

    @Test
    fun `insert and findById`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val criteria = criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Cost", weight = 5))
        val found = criteriaRepository.findById(criteria.id)
        found shouldNotBe null
        found shouldBe Criteria(id = criteria.id, decisionId = decision.id, name = "Cost", weight = 5)
    }

    @Test
    fun `update existing criteria`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val inserted = criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Cost", weight = 5))

        val updated = requireNotNull(criteriaRepository.update(inserted.id, "Updated Cost", 7))
        updated.name shouldBe "Updated Cost"
        updated.weight shouldBe 7
        updated.id shouldBe inserted.id
        updated.decisionId shouldBe decision.id

        // Verify the update persisted
        val found = requireNotNull(criteriaRepository.findById(inserted.id))
        found.name shouldBe "Updated Cost"
        found.weight shouldBe 7
    }

    @Test
    fun `update nonexistent criteria returns null`() {
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)

        val updated = criteriaRepository.update(999L, "This should not work", 1)

        updated shouldBe null
    }

    @Test
    fun `delete existing criteria`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val inserted = criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Cost", weight = 5))

        val deleted = criteriaRepository.delete(inserted.id)

        deleted.shouldBeTrue()

        // Verify the criteria is deleted
        val found = criteriaRepository.findById(inserted.id)
        found shouldBe null
    }

    @Test
    fun `delete nonexistent criteria returns false`() {
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)

        val deleted = criteriaRepository.delete(999L)

        deleted shouldBe false
    }
}
