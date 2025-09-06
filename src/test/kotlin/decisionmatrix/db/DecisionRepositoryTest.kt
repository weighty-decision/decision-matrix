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
import org.junit.jupiter.api.Test

class DecisionRepositoryTest {

    val jdbi = createTempDatabase()

    @Test fun `insert and findById`() {
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

    @Test fun `findById fully hydrates decision with criteria and options`() {
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

    @Test fun `findById returns decision with only criteria when no options exist`() {
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

    @Test fun `findById returns decision with only options when no criteria exist`() {
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

    @Test fun `update existing decision`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val inserted = decisionRepository.insert(DecisionInput(name = "Original decision"))

        val updated = requireNotNull(decisionRepository.update(inserted.id, "Updated decision"))
        updated.name shouldBe "Updated decision"
        updated.id shouldBe inserted.id

        // Verify the update persisted
        val found = requireNotNull(decisionRepository.findById(inserted.id))
        found.name shouldBe "Updated decision"
    }

    @Test fun `update nonexistent decision returns null`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)

        val updated = decisionRepository.update(999L, "This should not work")

        updated shouldBe null
    }

    @Test fun `delete existing decision`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val inserted = decisionRepository.insert(DecisionInput(name = "Decision to delete"))

        val deleted = decisionRepository.delete(inserted.id)

        deleted.shouldBeTrue()

        // Verify the decision is deleted
        val found = decisionRepository.findById(inserted.id)
        found shouldBe null
    }

    @Test fun `delete nonexistent decision returns false`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)

        val deleted = decisionRepository.delete(999L)

        deleted shouldBe false
    }

    @Test fun `insert decision with custom score range`() {
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

    @Test fun `update decision with new score range`() {
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

    @Test fun `update decision name only preserves existing score range`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val inserted = decisionRepository.insert(
            DecisionInput(
                name = "Original decision", 
                minScore = 3, 
                maxScore = 7
            )
        )

        val updated = requireNotNull(decisionRepository.update(inserted.id, "Updated decision name"))
        updated.name shouldBe "Updated decision name"
        updated.minScore shouldBe 3  // Should preserve existing values
        updated.maxScore shouldBe 7  // Should preserve existing values
        updated.id shouldBe inserted.id
    }

    @Test fun `findAllInvolvedDecisions returns empty list when user has no involvement`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        
        val decisions = decisionRepository.findAllInvolvedDecisions("nonexistent-user")
        
        decisions shouldBe emptyList()
    }

    @Test fun `findAllInvolvedDecisions returns decisions created by user`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        
        val decision1 = decisionRepository.insert(DecisionInput(name = "User's Decision 1"), createdBy = "user1")
        val decision2 = decisionRepository.insert(DecisionInput(name = "User's Decision 2"), createdBy = "user1")
        decisionRepository.insert(DecisionInput(name = "Other User's Decision"), createdBy = "user2")
        
        val decisions = decisionRepository.findAllInvolvedDecisions("user1")
        
        decisions.size shouldBe 2
        decisions.map { it.id }.toSet() shouldBe setOf(decision1.id, decision2.id)
        decisions.all { it.createdBy == "user1" }.shouldBeTrue()
    }

    @Test fun `findAllInvolvedDecisions returns decisions where user has scored options`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        
        // Create a decision by someone else
        val decision = decisionRepository.insert(DecisionInput(name = "Team Decision"), createdBy = "admin")
        val criteria = criteriaRepository.insert(decision.id, CriteriaInput(name = "Quality", weight = 5))
        val option = optionRepository.insert(decision.id, OptionInput(name = "Option A"))
        
        // User scores an option in this decision
        userScoreRepository.insert(decision.id, option.id, criteria.id, "user1", UserScoreInput(score = 8))
        
        val decisions = decisionRepository.findAllInvolvedDecisions("user1")
        
        decisions.size shouldBe 1
        decisions[0].id shouldBe decision.id
        decisions[0].name shouldBe "Team Decision"
        decisions[0].createdBy shouldBe "admin"
    }

    @Test fun `findAllInvolvedDecisions returns decisions where user both created and scored`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        
        // User creates a decision and also scores it
        val decision = decisionRepository.insert(DecisionInput(name = "Self-Scored Decision"), createdBy = "user1")
        val criteria = criteriaRepository.insert(decision.id, CriteriaInput(name = "Feasibility", weight = 3))
        val option = optionRepository.insert(decision.id, OptionInput(name = "Option B"))
        
        userScoreRepository.insert(decision.id, option.id, criteria.id, "user1", UserScoreInput(score = 6))
        
        val decisions = decisionRepository.findAllInvolvedDecisions("user1")
        
        decisions.size shouldBe 1
        decisions[0].id shouldBe decision.id
        decisions[0].createdBy shouldBe "user1"
    }

    @Test fun `findAllInvolvedDecisions returns decisions with full hydration including criteria and options`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        
        // Create decision with criteria and options
        val decision = decisionRepository.insert(DecisionInput(name = "Complex Decision"), createdBy = "admin")
        val criteria1 = criteriaRepository.insert(decision.id, CriteriaInput(name = "Cost", weight = 4))
        val criteria2 = criteriaRepository.insert(decision.id, CriteriaInput(name = "Speed", weight = 2))
        val option1 = optionRepository.insert(decision.id, OptionInput(name = "Option X"))
        val option2 = optionRepository.insert(decision.id, OptionInput(name = "Option Y"))
        
        // User scores an option
        userScoreRepository.insert(decision.id, option1.id, criteria1.id, "user1", UserScoreInput(score = 7))
        
        val decisions = decisionRepository.findAllInvolvedDecisions("user1")
        
        decisions.size shouldBe 1
        val foundDecision = decisions[0]
        foundDecision.name shouldBe "Complex Decision"
        
        // Verify criteria are fully hydrated
        foundDecision.criteria.size shouldBe 2
        foundDecision.criteria.any { it.name == "Cost" && it.weight == 4 }.shouldBeTrue()
        foundDecision.criteria.any { it.name == "Speed" && it.weight == 2 }.shouldBeTrue()
        
        // Verify options are fully hydrated
        foundDecision.options.size shouldBe 2
        foundDecision.options.any { it.name == "Option X" }.shouldBeTrue()
        foundDecision.options.any { it.name == "Option Y" }.shouldBeTrue()
    }

    @Test fun `findAllInvolvedDecisions returns decisions for multiple involvement types`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        
        // Decision 1: Created by user
        val decision1 = decisionRepository.insert(DecisionInput(name = "Own Decision"), createdBy = "user1")
        
        // Decision 2: Scored by user
        val decision2 = decisionRepository.insert(DecisionInput(name = "Scored Decision"), createdBy = "admin")
        val criteria2 = criteriaRepository.insert(decision2.id, CriteriaInput(name = "Priority", weight = 3))
        val option2 = optionRepository.insert(decision2.id, OptionInput(name = "Option Z"))
        userScoreRepository.insert(decision2.id, option2.id, criteria2.id, "user1", UserScoreInput(score = 5))
        
        // Decision 3: Not involved
        decisionRepository.insert(DecisionInput(name = "Unrelated Decision"), createdBy = "other-user")
        
        val decisions = decisionRepository.findAllInvolvedDecisions("user1")
        
        decisions.size shouldBe 2
        decisions.map { it.name }.toSet() shouldBe setOf("Own Decision", "Scored Decision")
    }

    @Test fun `findAllRecentDecisions returns empty list when no decisions exist`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        
        val decisions = decisionRepository.findAllRecentDecisions()
        
        decisions shouldBe emptyList()
    }

    @Test fun `findAllRecentDecisions returns all decisions created within last 3 months`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        
        val decision1 = decisionRepository.insert(DecisionInput(name = "Recent Decision 1"))
        val decision2 = decisionRepository.insert(DecisionInput(name = "Recent Decision 2"))
        val decision3 = decisionRepository.insert(DecisionInput(name = "Recent Decision 3"))
        
        val decisions = decisionRepository.findAllRecentDecisions()
        
        decisions.size shouldBe 3
        decisions.map { it.id }.toSet() shouldBe setOf(decision1.id, decision2.id, decision3.id)
        decisions.all { it.createdAt != null }.shouldBeTrue()
    }

    @Test fun `findAllRecentDecisions orders results by created_at descending`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        
        val decision1 = decisionRepository.insert(DecisionInput(name = "First Decision"))
        Thread.sleep(1100) // Wait over 1 second to ensure different timestamps
        val decision2 = decisionRepository.insert(DecisionInput(name = "Second Decision"))
        Thread.sleep(1100)
        val decision3 = decisionRepository.insert(DecisionInput(name = "Third Decision"))
        
        val decisions = decisionRepository.findAllRecentDecisions()
        
        decisions.size shouldBe 3
        
        // Verify ordering: most recent first (decisions should be sorted by createdAt DESC)
        for (i in 0 until decisions.size - 1) {
            val current = requireNotNull(decisions[i].createdAt)
            val next = requireNotNull(decisions[i + 1].createdAt)
            current.isAfter(next) shouldBe true
        }
        
        // Verify the specific order
        decisions[0].name shouldBe "Third Decision"
        decisions[1].name shouldBe "Second Decision" 
        decisions[2].name shouldBe "First Decision"
    }

    @Test fun `findAllRecentDecisions returns decisions with hydrated criteria and options`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        
        val decision = decisionRepository.insert(DecisionInput(name = "Decision with Relations"))
        val insertedCriteria = criteriaRepository.insert(decision.id, CriteriaInput(name = "Cost", weight = 5))
        val insertedOption = optionRepository.insert(decision.id, OptionInput(name = "Option A"))
        
        val decisions = decisionRepository.findAllRecentDecisions()
        
        decisions.size shouldBe 1
        val foundDecision = decisions[0]
        foundDecision.name shouldBe "Decision with Relations"
        foundDecision.criteria.size shouldBe 1
        foundDecision.criteria[0].id shouldBe insertedCriteria.id
        foundDecision.criteria[0].name shouldBe "Cost"
        foundDecision.criteria[0].weight shouldBe 5
        foundDecision.options.size shouldBe 1
        foundDecision.options[0].id shouldBe insertedOption.id
        foundDecision.options[0].name shouldBe "Option A"
    }

    @Test fun `findDecisions with no filters returns all decisions`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        
        val decision1 = decisionRepository.insert(DecisionInput(name = "First Decision"), createdBy = "user1")
        val decision2 = decisionRepository.insert(DecisionInput(name = "Second Decision"), createdBy = "user2")
        
        val filters = DecisionSearchFilters()
        val decisions = decisionRepository.findDecisions(filters)
        
        decisions.size shouldBe 2
        decisions.map { it.id } shouldContain decision1.id
        decisions.map { it.id } shouldContain decision2.id
    }

    @Test fun `findDecisions with search filter returns matching decisions`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        
        decisionRepository.insert(DecisionInput(name = "Laptop Selection"), createdBy = "user1")
        decisionRepository.insert(DecisionInput(name = "Car Purchase"), createdBy = "user1")
        decisionRepository.insert(DecisionInput(name = "Server Selection"), createdBy = "user2")
        
        val filters = DecisionSearchFilters(search = "Selection")
        val decisions = decisionRepository.findDecisions(filters)
        
        decisions.size shouldBe 2
        decisions.map { it.name }.shouldContainAll("Laptop Selection", "Server Selection")
    }

    @Test fun `findDecisions with involvement filter returns decisions user is involved in`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        
        val decision1 = decisionRepository.insert(DecisionInput(name = "Created By User"), createdBy = "user1")
        val decision2 = decisionRepository.insert(DecisionInput(name = "Scored By User"), createdBy = "user2")
        val decision3 = decisionRepository.insert(DecisionInput(name = "Not Involved"), createdBy = "user3")
        
        // Add option and criteria to decision2 so user1 can score it
        val option = optionRepository.insert(decision2.id, OptionInput(name = "Option"))
        val criteria = criteriaRepository.insert(decision2.id, CriteriaInput(name = "Criteria", weight = 1))
        userScoreRepository.insert(decision2.id, option.id, criteria.id, "user1", UserScoreInput(score = 5))
        
        val filters = DecisionSearchFilters(involvedOnly = true, userId = "user1")
        val decisions = decisionRepository.findDecisions(filters)
        
        decisions.size shouldBe 2
        decisions.map { it.name }.shouldContainAll("Created By User", "Scored By User")
    }

    @Test fun `findDecisions with recent filter returns recent decisions`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        
        // Create a decision that's not recent (we can't easily create an old decision, 
        // so we'll test with recent decisions and verify the SQL is correct)
        val decision1 = decisionRepository.insert(DecisionInput(name = "Recent Decision"), createdBy = "user1")
        val decision2 = decisionRepository.insert(DecisionInput(name = "Also Recent"), createdBy = "user2")
        
        val filters = DecisionSearchFilters(recentOnly = true)
        val decisions = decisionRepository.findDecisions(filters)
        
        // Both decisions should be recent since they were just created
        decisions.size shouldBe 2
    }

    @Test fun `findDecisions with multiple filters combines them with AND`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        
        val decision1 = decisionRepository.insert(DecisionInput(name = "Team Meeting"), createdBy = "user1")
        val decision2 = decisionRepository.insert(DecisionInput(name = "Personal Meeting"), createdBy = "user2")
        val decision3 = decisionRepository.insert(DecisionInput(name = "Team Project"), createdBy = "user1")
        
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
