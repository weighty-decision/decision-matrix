package decisionmatrix.db

import decisionmatrix.Decision
import decisionmatrix.Option
import decisionmatrix.OptionScore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class OptionScoreRepositoryTest {

    @Test
    fun insert_and_findById() {
        val jdbi = createTestJdbi()

        val decisionRepository = DecisionRepository(jdbi)
        val decision = decisionRepository.insert(Decision(name = "My decision", criteria = emptyList(), options = emptyList()))
        requireNotNull(decision.id)

        val optionRepository = OptionRepository(jdbi)
        val option = optionRepository.insert(Option(decisionId = decision.id, name = "Option A"))
        requireNotNull(option.id)

        val optionScoreRepository = OptionScoreRepository(jdbi)
        val optionScore = optionScoreRepository.insert(OptionScore(optionId = option.id, score = 9))
        requireNotNull(optionScore.id)
        val found = optionScoreRepository.findById(optionScore.id)

        assertNotNull(found)
        assertEquals(OptionScore(id = optionScore.id, optionId = option.id, score = 9), found)
    }
}
