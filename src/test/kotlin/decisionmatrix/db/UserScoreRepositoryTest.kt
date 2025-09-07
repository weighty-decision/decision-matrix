package decisionmatrix.db

import decisionmatrix.CriteriaInput
import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import decisionmatrix.UserScoreInput
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class UserScoreRepositoryTest {

    @Test
    fun `insert and findById`() {
        val jdbi = getTestJdbi()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val criteria = criteriaRepository.insert(decisionId = decision.id, CriteriaInput(name = "Cost", weight = 5))

        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        val optionScore = userScoreRepository.insert(
            decisionId = decision.id,
            optionId = option.id,
            criteriaId = criteria.id,
            scoredBy = "joe",
            score = UserScoreInput(score = 9)
        )
        val found = requireNotNull(userScoreRepository.findById(optionScore.id))
        found.id shouldBe optionScore.id
        found.decisionId shouldBe decision.id
        found.optionId shouldBe option.id
        found.criteriaId shouldBe criteria.id
        found.scoredBy shouldBe "joe"
        found.score shouldBe 9
        found.createdAt shouldNotBe null
    }

    @Test
    fun `update existing option score`() {
        val jdbi = getTestJdbi()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val criteria = criteriaRepository.insert(decisionId = decision.id, CriteriaInput(name = "Cost", weight = 5))

        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        val inserted = userScoreRepository.insert(
            decisionId = decision.id,
            optionId = option.id,
            criteriaId = criteria.id,
            scoredBy = "joe",
            score = UserScoreInput(score = 9)
        )

        val updated = requireNotNull(userScoreRepository.update(inserted.id, 8))
        updated.score shouldBe 8
        updated.id shouldBe inserted.id
        updated.decisionId shouldBe decision.id
        updated.optionId shouldBe option.id
        updated.criteriaId shouldBe criteria.id
        updated.scoredBy shouldBe "joe"
        updated.createdAt shouldNotBe null

        // Verify the update persisted
        val found = requireNotNull(userScoreRepository.findById(inserted.id))
        found.score shouldBe 8
        found.createdAt shouldNotBe null
    }

    @Test
    fun `update nonexistent option score returns null`() {
        val jdbi = getTestJdbi()
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)

        val updated = userScoreRepository.update(999L, 10)

        updated shouldBe null
    }

    @Test
    fun `delete existing option score`() {
        val jdbi = getTestJdbi()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val criteria = criteriaRepository.insert(decisionId = decision.id, CriteriaInput(name = "Cost", weight = 5))

        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        val inserted = userScoreRepository.insert(
            decisionId = decision.id,
            optionId = option.id,
            criteriaId = criteria.id,
            scoredBy = "joe",
            score = UserScoreInput(score = 9)
        )

        val deleted = userScoreRepository.delete(inserted.id)

        deleted.shouldBeTrue()

        // Verify the option score is deleted
        val found = userScoreRepository.findById(inserted.id)
        found shouldBe null
    }

    @Test
    fun `delete nonexistent option score returns false`() {
        val jdbi = getTestJdbi()
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)

        val deleted = userScoreRepository.delete(999L)

        deleted shouldBe false
    }

    @Test
    fun `findAllByDecisionId returns all scores for decision`() {
        val jdbi = getTestJdbi()

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

        val userScoreRepository = UserScoreRepositoryImpl(jdbi)

        // Insert scores for decision1
        val score1 = userScoreRepository.insert(
            decisionId = decision1.id,
            optionId = option1.id,
            criteriaId = criteria1.id,
            scoredBy = "alice",
            score = UserScoreInput(score = 7)
        )
        val score2 = userScoreRepository.insert(
            decisionId = decision1.id,
            optionId = option2.id,
            criteriaId = criteria2.id,
            scoredBy = "bob",
            score = UserScoreInput(score = 9)
        )

        // Insert score for decision2 (should not be returned)
        userScoreRepository.insert(
            decisionId = decision2.id,
            optionId = option3.id,
            criteriaId = criteria3.id,
            scoredBy = "charlie",
            score = UserScoreInput(score = 6)
        )

        val scoresForDecision1 = userScoreRepository.findAllByDecisionId(decision1.id)

        scoresForDecision1.size shouldBe 2

        val firstScore = scoresForDecision1[0]
        firstScore.id shouldBe score1.id
        firstScore.decisionId shouldBe decision1.id
        firstScore.optionId shouldBe option1.id
        firstScore.criteriaId shouldBe criteria1.id
        firstScore.scoredBy shouldBe "alice"
        firstScore.score shouldBe 7
        firstScore.createdAt shouldNotBe null

        val secondScore = scoresForDecision1[1]
        secondScore.id shouldBe score2.id
        secondScore.decisionId shouldBe decision1.id
        secondScore.optionId shouldBe option2.id
        secondScore.criteriaId shouldBe criteria2.id
        secondScore.scoredBy shouldBe "bob"
        secondScore.score shouldBe 9
        secondScore.createdAt shouldNotBe null

        // Verify decision2 has its own score
        val scoresForDecision2 = userScoreRepository.findAllByDecisionId(decision2.id)
        scoresForDecision2.size shouldBe 1
        scoresForDecision2[0].decisionId shouldBe decision2.id
        scoresForDecision2[0].createdAt shouldNotBe null
    }

    @Test
    fun `findAllByDecisionId returns empty list when no scores exist`() {
        val jdbi = getTestJdbi()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        val scores = userScoreRepository.findAllByDecisionId(decision.id)

        scores shouldBe emptyList()
    }
}
