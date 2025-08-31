package decisionmatrix.db

import decisionmatrix.Decision
import decisionmatrix.Option
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class OptionRepositoryTest {

    val jdbi = createTestJdbi()

    @Test
    fun insert_and_findById() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(
            Decision(name = "My decision", criteria = emptyList(), options = emptyList())
        )
        requireNotNull(decision.id)

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(Option(decisionId = decision.id, name = "Option A"))
        requireNotNull(option.id)
        val found = optionRepository.findById(option.id)
        found shouldNotBe null
        found shouldBe Option(id = option.id, decisionId = decision.id, name = "Option A")
    }
}
