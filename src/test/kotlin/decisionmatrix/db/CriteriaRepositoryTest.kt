package decisionmatrix.db

import decisionmatrix.Criteria
import decisionmatrix.CriteriaInput
import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import decisionmatrix.UserScoreInput
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

    @Test
    fun `delete criteria cascades to user_scores`() {
        cleanTestDatabase()
        
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        
        // Create test data
        val decision = decisionRepository.insert(DecisionInput(name = "Test Decision"))
        val option = optionRepository.insert(decision.id, OptionInput(name = "Test Option"))
        val criteria = criteriaRepository.insert(decision.id, CriteriaInput(name = "Test Criteria", weight = 5))
        
        // Create user scores referencing the criteria
        val userScore1 = userScoreRepository.insert(
            decisionId = decision.id,
            optionId = option.id, 
            criteriaId = criteria.id,
            scoredBy = "user1",
            score = UserScoreInput(score = 3)
        )
        val userScore2 = userScoreRepository.insert(
            decisionId = decision.id,
            optionId = option.id,
            criteriaId = criteria.id, 
            scoredBy = "user2",
            score = UserScoreInput(score = 4)
        )
        
        // Verify user scores exist
        userScoreRepository.findById(userScore1.id) shouldNotBe null
        userScoreRepository.findById(userScore2.id) shouldNotBe null
        
        // Delete the criteria
        val deleted = criteriaRepository.delete(criteria.id)
        deleted.shouldBeTrue()
        
        // Verify criteria is deleted
        criteriaRepository.findById(criteria.id) shouldBe null
        
        // Verify user scores are also deleted (cascaded)
        userScoreRepository.findById(userScore1.id) shouldBe null
        userScoreRepository.findById(userScore2.id) shouldBe null
        
        // Verify other data is unaffected
        decisionRepository.findById(decision.id) shouldNotBe null
        optionRepository.findById(option.id) shouldNotBe null
    }
}
