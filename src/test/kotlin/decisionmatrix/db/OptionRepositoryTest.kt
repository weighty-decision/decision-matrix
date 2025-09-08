package decisionmatrix.db

import decisionmatrix.CriteriaInput
import decisionmatrix.DecisionInput
import decisionmatrix.Option
import decisionmatrix.OptionInput
import decisionmatrix.UserScoreInput
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class OptionRepositoryTest {

    val jdbi = getTestJdbi()

    @Test
    fun `insert and findById`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))
        val found = optionRepository.findById(option.id)
        found shouldNotBe null
        found shouldBe Option(id = option.id, decisionId = decision.id, name = "Option A")
    }

    @Test
    fun `update existing option`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val inserted = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        val updated = requireNotNull(optionRepository.update(inserted.id, "Updated Option A"))
        updated.name shouldBe "Updated Option A"
        updated.id shouldBe inserted.id
        updated.decisionId shouldBe decision.id

        // Verify the update persisted
        val found = requireNotNull(optionRepository.findById(inserted.id))
        found.name shouldBe "Updated Option A"
    }

    @Test
    fun `update nonexistent option returns null`() {
        val optionRepository = OptionRepositoryImpl(jdbi)

        val updated = optionRepository.update(999L, "This should not work")

        updated shouldBe null
    }

    @Test
    fun `delete existing option`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val inserted = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        val deleted = optionRepository.delete(inserted.id)

        deleted.shouldBeTrue()

        // Verify the option is deleted
        val found = optionRepository.findById(inserted.id)
        found shouldBe null
    }

    @Test
    fun `delete nonexistent option returns false`() {
        val optionRepository = OptionRepositoryImpl(jdbi)

        val deleted = optionRepository.delete(999L)

        deleted shouldBe false
    }

    @Test
    fun `delete option cascades to user_scores`() {
        cleanTestDatabase()
        
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        
        // Create test data
        val decision = decisionRepository.insert(DecisionInput(name = "Test Decision"))
        val option = optionRepository.insert(decision.id, OptionInput(name = "Test Option"))
        val criteria = criteriaRepository.insert(decision.id, CriteriaInput(name = "Test Criteria", weight = 5))
        
        // Create user scores referencing the option
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
        
        // Delete the option
        val deleted = optionRepository.delete(option.id)
        deleted.shouldBeTrue()
        
        // Verify option is deleted
        optionRepository.findById(option.id) shouldBe null
        
        // Verify user scores are also deleted (cascaded)
        userScoreRepository.findById(userScore1.id) shouldBe null
        userScoreRepository.findById(userScore2.id) shouldBe null
        
        // Verify other data is unaffected
        decisionRepository.findById(decision.id) shouldNotBe null
        criteriaRepository.findById(criteria.id) shouldNotBe null
    }
}
