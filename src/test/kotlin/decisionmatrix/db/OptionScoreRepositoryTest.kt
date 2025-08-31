package decisionmatrix.db

import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import decisionmatrix.OptionScoreInput
import decisionmatrix.OptionScore
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
        val option = optionRepository.insert(OptionInput(decisionId = decision.id, name = "Option A"))

        val optionScoreRepository = OptionScoreRepositoryImpl(jdbi)
        val optionScore = optionScoreRepository.insert(OptionScoreInput(optionId = option.id, score = 9))
        val found = optionScoreRepository.findById(optionScore.id)
        found shouldNotBe null
        found shouldBe OptionScore(id = optionScore.id, optionId = option.id, score = 9)
    }
}
