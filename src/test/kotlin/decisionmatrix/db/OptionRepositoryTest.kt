package decisionmatrix.db

import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import decisionmatrix.Option
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class OptionRepositoryTest {

    val jdbi = createTestJdbi()

    @Test
    fun insert_and_findById() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val decision = decisionRepository.insert(DecisionInput(name = "My decision"))

        val optionRepository = OptionRepositoryImpl(jdbi)
        val option = optionRepository.insert(OptionInput(decisionId = decision.id, name = "Option A"))
        val found = optionRepository.findById(option.id)
        found shouldNotBe null
        found shouldBe Option(id = option.id, decisionId = decision.id, name = "Option A")
    }
}
