package decisionmatrix.db

import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import decisionmatrix.OptionScoreInput
import decisionmatrix.OptionScore
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class OptionScoreRepositoryTest {

    @Test
    fun insert_and_findById() {
        val jdbi = createTestJdbi()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        val optionScoreRepository = OptionScoreRepositoryImpl(jdbi)
        val optionScore = optionScoreRepository.insert(optionId = option.id, OptionScoreInput(score = 9))
        val found = optionScoreRepository.findById(optionScore.id)
        found shouldNotBe null
        found shouldBe OptionScore(id = optionScore.id, optionId = option.id, score = 9)
    }

    @Test
    fun update_existing_option_score() {
        val jdbi = createTestJdbi()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        val optionScoreRepository = OptionScoreRepositoryImpl(jdbi)
        val inserted = optionScoreRepository.insert(optionId = option.id, OptionScoreInput(score = 5))

        val updated = requireNotNull(optionScoreRepository.update(inserted.id, 8))
        updated.score shouldBe 8
        updated.id shouldBe inserted.id
        updated.optionId shouldBe option.id

        // Verify the update persisted
        val found = requireNotNull(optionScoreRepository.findById(inserted.id))
        found.score shouldBe 8
    }

    @Test
    fun update_nonexistent_option_score_returns_null() {
        val jdbi = createTestJdbi()
        val optionScoreRepository = OptionScoreRepositoryImpl(jdbi)

        val updated = optionScoreRepository.update(999L, 10)

        updated shouldBe null
    }

    @Test
    fun delete_existing_option_score() {
        val jdbi = createTestJdbi()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        val optionScoreRepository = OptionScoreRepositoryImpl(jdbi)
        val inserted = optionScoreRepository.insert(optionId = option.id, OptionScoreInput(score = 7))

        val deleted = optionScoreRepository.delete(inserted.id)

        deleted.shouldBeTrue()

        // Verify the option score is deleted
        val found = optionScoreRepository.findById(inserted.id)
        found shouldBe null
    }

    @Test
    fun delete_nonexistent_option_score_returns_false() {
        val jdbi = createTestJdbi()
        val optionScoreRepository = OptionScoreRepositoryImpl(jdbi)

        val deleted = optionScoreRepository.delete(999L)

        deleted shouldBe false
    }
}
