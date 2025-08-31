package decisionmatrix.db

import decisionmatrix.Decision
import decisionmatrix.Option
import decisionmatrix.OptionScore
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class OptionScoreRepositoryTest {

    @Test
    fun insert_and_findById() {
        val jdbi = createTestJdbi()

        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(Decision(name = "My decision", criteria = emptyList(), options = emptyList()))
        requireNotNull(decision.id)

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(Option(decisionId = decision.id, name = "Option A"))
        requireNotNull(option.id)

        val optionScoreRepository = OptionScoreRepositoryImpl(jdbi)
        val optionScore = optionScoreRepository.insert(OptionScore(optionId = option.id, score = 9))
        requireNotNull(optionScore.id)
        val found = optionScoreRepository.findById(optionScore.id)
        found shouldNotBe null
        found shouldBe OptionScore(id = optionScore.id, optionId = option.id, score = 9)
    }
}
