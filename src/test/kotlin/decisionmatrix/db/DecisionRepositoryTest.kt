package decisionmatrix.db

import decisionmatrix.CriteriaInput
import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import decisionmatrix.UserScoreInput
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class DecisionRepositoryTest {

    val jdbi = getTestJdbi()

    @AfterEach
    fun cleanup() {
        cleanTestDatabase()
    }

    @Test
    fun `insert and findById`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val inserted = decisionRepository.insert(
            DecisionInput(
                name = "My decision",
            )
        )
        val found = requireNotNull(decisionRepository.findById(inserted.id))
        assertSoftly(found) {
            id shouldBe inserted.id
            name shouldBe "My decision"
            minScore shouldBe 1
            maxScore shouldBe 10
            createdBy shouldBe "unknown"
            createdAt shouldNotBe null
            criteria shouldBe emptyList()
            options shouldBe emptyList()
        }
    }

    @Test
    fun `findById fully hydrates decision with criteria and options`() {
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
    fun `findById returns decision with only criteria when no options exist`() {
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
    fun `findById returns decision with only options when no criteria exist`() {
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

    @Test
    fun `delete existing decision`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val inserted = decisionRepository.insert(DecisionInput(name = "Decision to delete"))

        val deleted = decisionRepository.delete(inserted.id)

        deleted.shouldBeTrue()

        // Verify the decision is deleted
        val found = decisionRepository.findById(inserted.id)
        found shouldBe null
    }

    @Test
    fun `delete nonexistent decision returns false`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)

        val deleted = decisionRepository.delete(999L)

        deleted shouldBe false
    }

    @Test
    fun `insert decision with custom score range`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val inserted = decisionRepository.insert(
            DecisionInput(
                name = "Custom score range decision",
                minScore = 0,
                maxScore = 5
            )
        )
        val found = requireNotNull(decisionRepository.findById(inserted.id))

        found.name shouldBe "Custom score range decision"
        found.minScore shouldBe 0
        found.maxScore shouldBe 5
    }

    @Test
    fun `update decision`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val inserted = decisionRepository.insert(DecisionInput(name = "Original decision"))

        val updated = requireNotNull(decisionRepository.update(inserted.id, "Updated decision", 2, 8))
        updated.name shouldBe "Updated decision"
        updated.minScore shouldBe 2
        updated.maxScore shouldBe 8
        updated.id shouldBe inserted.id

        // Verify the update persisted
        val found = requireNotNull(decisionRepository.findById(inserted.id))
        found.name shouldBe "Updated decision"
        found.minScore shouldBe 2
        found.maxScore shouldBe 8
    }

    @Test
    fun `findDecisions with no filters returns all decisions`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)

        val decision1 = decisionRepository.insert(DecisionInput(name = "First Decision"), createdBy = "user1")
        val decision2 = decisionRepository.insert(DecisionInput(name = "Second Decision"), createdBy = "user2")

        val filters = DecisionSearchFilters()
        val decisions = decisionRepository.findDecisions(filters)

        decisions.size shouldBe 2
        decisions.map { it.id } shouldContain decision1.id
        decisions.map { it.id } shouldContain decision2.id
    }

    @Test
    fun `findDecisions with search filter returns matching decisions`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)

        decisionRepository.insert(DecisionInput(name = "Laptop Selection"), createdBy = "user1")
        decisionRepository.insert(DecisionInput(name = "Car Purchase"), createdBy = "user1")
        decisionRepository.insert(DecisionInput(name = "Server Selection"), createdBy = "user2")

        val filters = DecisionSearchFilters(search = "Selection")
        val decisions = decisionRepository.findDecisions(filters)

        decisions.size shouldBe 2
        decisions.map { it.name }.shouldContainAll("Laptop Selection", "Server Selection")
    }

    @Test
    fun `findDecisions with involvement filter returns decisions user is involved in`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)

        decisionRepository.insert(DecisionInput(name = "Created By User"), createdBy = "user1")
        val decision2 = decisionRepository.insert(DecisionInput(name = "Scored By User"), createdBy = "user2")
        decisionRepository.insert(DecisionInput(name = "Not Involved"), createdBy = "user3")

        // Add option and criteria to decision2 so user1 can score it
        val option = optionRepository.insert(decision2.id, OptionInput(name = "Option"))
        val criteria = criteriaRepository.insert(decision2.id, CriteriaInput(name = "Criteria", weight = 1))
        userScoreRepository.insert(decision2.id, option.id, criteria.id, "user1", UserScoreInput(score = 5))

        val filters = DecisionSearchFilters(involvedOnly = true, userId = "user1")
        val decisions = decisionRepository.findDecisions(filters)

        decisions.size shouldBe 2
        decisions.map { it.name }.shouldContainAll("Created By User", "Scored By User")
    }

    @Test
    fun `findDecisions with recent filter returns recent decisions`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)

        decisionRepository.insert(DecisionInput(name = "Recent Decision"), createdBy = "user1")

        val filters = DecisionSearchFilters(search = "Recent Decision", recentOnly = true)
        val decisions = decisionRepository.findDecisions(filters)

        decisions.size shouldBe 1
    }

    @Test
    fun `findDecisions with multiple filters combines them with AND`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)

        decisionRepository.insert(DecisionInput(name = "Team Meeting"), createdBy = "user1")
        decisionRepository.insert(DecisionInput(name = "Personal Meeting"), createdBy = "user2")
        decisionRepository.insert(DecisionInput(name = "Team Project"), createdBy = "user1")

        val filters = DecisionSearchFilters(
            search = "Meeting",
            involvedOnly = true,
            userId = "user1"
        )
        val decisions = decisionRepository.findDecisions(filters)

        // Only decision1 should match: has "Meeting" in name AND is created by user1
        decisions.size shouldBe 1
        decisions[0].name shouldBe "Team Meeting"
    }
}
