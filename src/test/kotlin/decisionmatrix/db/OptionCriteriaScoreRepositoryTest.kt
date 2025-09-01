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

    @Test fun `insert and findById`() {
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

    @Test fun `update existing option score`() {
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

    @Test fun `update nonexistent option score returns null`() {
        val jdbi = createTempDatabase()
        val optionScoreRepository = OptionCriteriaScoreRepositoryImpl(jdbi)

        val updated = optionScoreRepository.update(999L, 10)

        updated shouldBe null
    }

    @Test fun `delete existing option score`() {
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

    @Test fun `delete nonexistent option score returns false`() {
        val jdbi = createTempDatabase()
        val optionScoreRepository = OptionCriteriaScoreRepositoryImpl(jdbi)

        val deleted = optionScoreRepository.delete(999L)

        deleted shouldBe false
    }

    @Test fun `findAllByDecisionId returns all scores for decision`() {
        val jdbi = createTempDatabase()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision1 = decisionRepository.insert(DecisionInput(name = "Decision 1"))
        val decision2 = decisionRepository.insert(DecisionInput(name = "Decision 2"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option1 = optionRepository.insert(decisionId = decision1.id, OptionInput(name = "Option A"))
        val option2 = optionRepository.insert(decisionId = decision1.id, OptionInput(name = "Option B"))
        val option3 = optionRepository.insert(decisionId = decision2.id, OptionInput(name = "Option C"))

        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val criteria1 = criteriaRepository.insert(decisionId = decision1.id, CriteriaInput(name = "Cost", weight = 5))
        val criteria2 = criteriaRepository.insert(decisionId = decision1.id, CriteriaInput(name = "Quality", weight = 8))
        val criteria3 = criteriaRepository.insert(decisionId = decision2.id, CriteriaInput(name = "Speed", weight = 3))

        val optionCriteriaScoreRepository = OptionCriteriaScoreRepositoryImpl(jdbi)

        // Insert scores for decision1
        val score1 = optionCriteriaScoreRepository.insert(
            decisionId = decision1.id,
            optionId = option1.id,
            criteriaId = criteria1.id,
            scoredBy = "alice",
            score = OptionCriteriaScoreInput(score = 7)
        )
        val score2 = optionCriteriaScoreRepository.insert(
            decisionId = decision1.id,
            optionId = option2.id,
            criteriaId = criteria2.id,
            scoredBy = "bob",
            score = OptionCriteriaScoreInput(score = 9)
        )

        // Insert score for decision2 (should not be returned)
        optionCriteriaScoreRepository.insert(
            decisionId = decision2.id,
            optionId = option3.id,
            criteriaId = criteria3.id,
            scoredBy = "charlie",
            score = OptionCriteriaScoreInput(score = 6)
        )

        val scoresForDecision1 = optionCriteriaScoreRepository.findAllByDecisionId(decision1.id)

        scoresForDecision1.size shouldBe 2
        scoresForDecision1[0] shouldBe OptionCriteriaScore(
            id = score1.id,
            decisionId = decision1.id,
            optionId = option1.id,
            criteriaId = criteria1.id,
            scoredBy = "alice",
            score = 7
        )
        scoresForDecision1[1] shouldBe OptionCriteriaScore(
            id = score2.id,
            decisionId = decision1.id,
            optionId = option2.id,
            criteriaId = criteria2.id,
            scoredBy = "bob",
            score = 9
        )

        // Verify decision2 has its own score
        val scoresForDecision2 = optionCriteriaScoreRepository.findAllByDecisionId(decision2.id)
        scoresForDecision2.size shouldBe 1
        scoresForDecision2[0].decisionId shouldBe decision2.id
    }

    @Test fun `findAllByDecisionId returns empty list when no scores exist`() {
        val jdbi = createTempDatabase()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionCriteriaScoreRepository = OptionCriteriaScoreRepositoryImpl(jdbi)
        val scores = optionCriteriaScoreRepository.findAllByDecisionId(decision.id)

        scores shouldBe emptyList()
    }
}
