package decisionmatrix.db

import decisionmatrix.Criteria
import decisionmatrix.Decision
import decisionmatrix.Option
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DecisionRepositoryTest {

    val jdbi = createTestJdbi()

    @Test
    fun insert_and_findById() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val inserted = decisionRepository.insert(Decision(name = "My decision", criteria = emptyList(), options = emptyList()))
        requireNotNull(inserted.id)
        val found = decisionRepository.findById(inserted.id)

        assertNotNull(found)
        assertEquals(
            Decision(id = inserted.id, name = "My decision", criteria = emptyList(), options = emptyList()),
            found
        )
    }

    @Test
    fun findById_fully_hydrates_decision_with_criteria_and_options() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)

        // Insert a decision
        val insertedDecision = decisionRepository.insert(Decision(name = "Choose a car", criteria = emptyList(), options = emptyList()))
        requireNotNull(insertedDecision.id)

        // Insert criteria for the decision
        val insertedCriteria1 = criteriaRepository.insert(Criteria(decisionId = insertedDecision.id, name = "Cost", weight = 3))
        val insertedCriteria2 = criteriaRepository.insert(Criteria(decisionId = insertedDecision.id, name = "Reliability", weight = 5))

        // Insert options for the decision
        val insertedOption1 = optionRepository.insert(Option(decisionId = insertedDecision.id, name = "Honda Civic"))
        val insertedOption2 = optionRepository.insert(Option(decisionId = insertedDecision.id, name = "Toyota Camry"))

        // Retrieve the fully hydrated decision
        val found = decisionRepository.findById(insertedDecision.id)

        assertNotNull(found)
        assertEquals("Choose a car", found!!.name)
        assertEquals(insertedDecision.id, found.id)

        // Verify criteria are fully hydrated
        assertEquals(2, found.criteria.size)
        assertTrue(found.criteria.any { it.id == insertedCriteria1.id && it.name == "Cost" && it.weight == 3 && it.decisionId == insertedDecision.id })
        assertTrue(found.criteria.any { it.id == insertedCriteria2.id && it.name == "Reliability" && it.weight == 5 && it.decisionId == insertedDecision.id })

        // Verify options are fully hydrated
        assertEquals(2, found.options.size)
        assertTrue(found.options.any { it.id == insertedOption1.id && it.name == "Honda Civic" && it.decisionId == insertedDecision.id })
        assertTrue(found.options.any { it.id == insertedOption2.id && it.name == "Toyota Camry" && it.decisionId == insertedDecision.id })
    }

    @Test
    fun findById_returns_decision_with_only_criteria_when_no_options_exist() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)

        // Insert a decision
        val insertedDecision = decisionRepository.insert(Decision(name = "Criteria only decision", criteria = emptyList(), options = emptyList()))
        requireNotNull(insertedDecision.id)

        // Insert only criteria
        val insertedCriteria = criteriaRepository.insert(Criteria(decisionId = insertedDecision.id, name = "Budget", weight = 4))

        // Retrieve the decision
        val found = decisionRepository.findById(insertedDecision.id)

        assertNotNull(found)
        assertEquals("Criteria only decision", found!!.name)
        assertEquals(1, found.criteria.size)
        assertEquals(insertedCriteria.id, found.criteria[0].id)
        assertEquals("Budget", found.criteria[0].name)
        assertEquals(4, found.criteria[0].weight)
        assertEquals(0, found.options.size)
    }

    @Test
    fun findById_returns_decision_with_only_options_when_no_criteria_exist() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)

        // Insert a decision
        val insertedDecision = decisionRepository.insert(Decision(name = "Options only decision", criteria = emptyList(), options = emptyList()))
        requireNotNull(insertedDecision.id)

        // Insert only options
        val insertedOption = optionRepository.insert(Option(decisionId = insertedDecision.id, name = "Option A"))

        // Retrieve the decision
        val found = decisionRepository.findById(insertedDecision.id)

        assertNotNull(found)
        assertEquals("Options only decision", found!!.name)
        assertEquals(0, found.criteria.size)
        assertEquals(1, found.options.size)
        assertEquals(insertedOption.id, found.options[0].id)
        assertEquals("Option A", found.options[0].name)
    }
}
