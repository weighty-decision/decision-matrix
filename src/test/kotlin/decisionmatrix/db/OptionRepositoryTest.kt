package decisionmatrix.db

import decisionmatrix.DecisionInput
import decisionmatrix.Option
import decisionmatrix.OptionInput
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
}
