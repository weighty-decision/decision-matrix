package decisionmatrix.db

import decisionmatrix.CriteriaInput
import decisionmatrix.DecisionInput
import decisionmatrix.OptionCriteriaScore
import decisionmatrix.OptionCriteriaScoreInput
import decisionmatrix.OptionInput
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class OptionCriteriaScoreRepositoryTest {

    @Test
    fun insert_and_findById() {
        val jdbi = createTempDatabase()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val criteria = criteriaRepository.insert(decisionId = decision.id, CriteriaInput(name = "Cost", weight = 5))

        val optionCriteriaScoreRepository = OptionCriteriaScoreRepositoryImpl(jdbi)
        val optionScore = optionCriteriaScoreRepository.insert(
            decisionId = decision.id,
            optionId = option.id,
            criteriaId = criteria.id,
            scoredBy = "joe",
            score = OptionCriteriaScoreInput(score = 9)
        )
        val found = optionCriteriaScoreRepository.findById(optionScore.id)
        found shouldNotBe null
        found shouldBe OptionCriteriaScore(id = optionScore.id, decisionId = decision.id, optionId = option.id, criteriaId = criteria.id, scoredBy = "joe", score = 9)
    }

    @Test
    fun update_existing_option_score() {
        val jdbi = createTempDatabase()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val criteria = criteriaRepository.insert(decisionId = decision.id, CriteriaInput(name = "Cost", weight = 5))

        val optionCriteriaScoreRepository = OptionCriteriaScoreRepositoryImpl(jdbi)
        val inserted = optionCriteriaScoreRepository.insert(
            decisionId = decision.id,
            optionId = option.id,
            criteriaId = criteria.id,
            scoredBy = "joe",
            score = OptionCriteriaScoreInput(score = 9)
        )

        val updated = requireNotNull(optionCriteriaScoreRepository.update(inserted.id, 8))
        updated.score shouldBe 8
        updated.id shouldBe inserted.id
        updated.decisionId shouldBe decision.id
        updated.optionId shouldBe option.id
        updated.criteriaId shouldBe criteria.id
        updated.scoredBy shouldBe "joe"

        // Verify the update persisted
        val found = requireNotNull(optionCriteriaScoreRepository.findById(inserted.id))
        found.score shouldBe 8
    }

    @Test
    fun update_nonexistent_option_score_returns_null() {
        val jdbi = createTempDatabase()
        val optionScoreRepository = OptionCriteriaScoreRepositoryImpl(jdbi)

        val updated = optionScoreRepository.update(999L, 10)

        updated shouldBe null
    }

    @Test
    fun delete_existing_option_score() {
        val jdbi = createTempDatabase()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val criteria = criteriaRepository.insert(decisionId = decision.id, CriteriaInput(name = "Cost", weight = 5))

        val optionCriteriaScoreRepository = OptionCriteriaScoreRepositoryImpl(jdbi)
        val inserted = optionCriteriaScoreRepository.insert(
            decisionId = decision.id,
            optionId = option.id,
            criteriaId = criteria.id,
            scoredBy = "joe",
            score = OptionCriteriaScoreInput(score = 9)
        )

        val deleted = optionCriteriaScoreRepository.delete(inserted.id)

        deleted.shouldBeTrue()

        // Verify the option score is deleted
        val found = optionCriteriaScoreRepository.findById(inserted.id)
        found shouldBe null
    }

    @Test
    fun delete_nonexistent_option_score_returns_false() {
        val jdbi = createTempDatabase()
        val optionScoreRepository = OptionCriteriaScoreRepositoryImpl(jdbi)

        val deleted = optionScoreRepository.delete(999L)

        deleted shouldBe false
    }
}
