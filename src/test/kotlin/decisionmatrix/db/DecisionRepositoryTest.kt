package decisionmatrix.db

import decisionmatrix.CriteriaInput
import decisionmatrix.Decision
import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class DecisionRepositoryTest {

    val jdbi = createTestJdbi()

    @Test
    fun insert_and_findById() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val inserted = decisionRepository.insert(
            DecisionInput(
                name = "My decision",
            )
        )
        val found = decisionRepository.findById(inserted.id)

        found shouldNotBe null
        found shouldBe Decision(id = inserted.id, name = "My decision", criteria = emptyList(), options = emptyList())
    }

    @Test
    fun findById_fully_hydrates_decision_with_criteria_and_options() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)

        // Insert a decision
        val insertedDecision = decisionRepository.insert(
            DecisionInput(
                name = "Choose a car",
            )
        )

        // Insert criteria for the decision
        val insertedCriteria1 = criteriaRepository.insert(decisionId = insertedDecision.id, CriteriaInput(name = "Cost", weight = 3))
        val insertedCriteria2 = criteriaRepository.insert(decisionId = insertedDecision.id, CriteriaInput(name = "Reliability", weight = 5))

        // Insert options for the decision
        val insertedOption1 = optionRepository.insert(decisionId = insertedDecision.id, OptionInput(name = "Honda Civic"))
        val insertedOption2 = optionRepository.insert(decisionId = insertedDecision.id, OptionInput(name = "Toyota Camry"))

        // Retrieve the fully hydrated decision
        val found = requireNotNull(decisionRepository.findById(insertedDecision.id))
        found.name shouldBe "Choose a car"
        found.id shouldBe insertedDecision.id

        // Verify criteria are fully hydrated
        found.criteria.size shouldBe 2
        found.criteria.any { it.id == insertedCriteria1.id && it.name == "Cost" && it.weight == 3 && it.decisionId == insertedDecision.id }
            .shouldBeTrue()
        found.criteria.any { it.id == insertedCriteria2.id && it.name == "Reliability" && it.weight == 5 && it.decisionId == insertedDecision.id }
            .shouldBeTrue()

        // Verify options are fully hydrated
        found.options.size shouldBe 2
        found.options.any { it.id == insertedOption1.id && it.name == "Honda Civic" && it.decisionId == insertedDecision.id }
            .shouldBeTrue()
        found.options.any { it.id == insertedOption2.id && it.name == "Toyota Camry" && it.decisionId == insertedDecision.id }
            .shouldBeTrue()
    }

    @Test
    fun findById_returns_decision_with_only_criteria_when_no_options_exist() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)

        // Insert a decision
        val insertedDecision = decisionRepository.insert(DecisionInput(name = "Criteria only decision"))

        // Insert only criteria
        val insertedCriteria = criteriaRepository.insert(decisionId = insertedDecision.id, CriteriaInput(name = "Budget", weight = 4))

        // Retrieve the decision
        val found = requireNotNull(decisionRepository.findById(insertedDecision.id))

        found.name shouldBe "Criteria only decision"
        found.criteria.size shouldBe 1
        found.criteria[0].id shouldBe insertedCriteria.id
        found.criteria[0].name shouldBe "Budget"
        found.criteria[0].weight shouldBe 4
        found.options.size shouldBe 0
    }

    @Test
    fun findById_returns_decision_with_only_options_when_no_criteria_exist() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)

        // Insert a decision
        val insertedDecision = decisionRepository.insert(DecisionInput(name = "Options only decision"))

        // Insert only options
        val insertedOption = optionRepository.insert(decisionId = insertedDecision.id, OptionInput(name = "Option A"))

        // Retrieve the decision
        val found = requireNotNull(decisionRepository.findById(insertedDecision.id))

        found.name shouldBe "Options only decision"
        found.criteria.size shouldBe 0
        found.options.size shouldBe 1
        found.options[0].id shouldBe insertedOption.id
        found.options[0].name shouldBe "Option A"
    }
}
