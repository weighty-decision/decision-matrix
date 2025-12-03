package decisionmatrix.db

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SampleDataPopulatorTest {

    val jdbi = getTestJdbi()

    @BeforeEach
    fun setup() {
        cleanTestDatabase()
    }

    @AfterEach
    fun cleanup() {
        cleanTestDatabase()
    }

    @Test
    fun `populateIfEmpty creates sample data when database is empty`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        val tagRepository = TagRepositoryImpl(jdbi)

        val populator = SampleDataPopulator(
            jdbi = jdbi,
            decisionRepository = decisionRepository,
            optionRepository = optionRepository,
            criteriaRepository = criteriaRepository,
            userScoreRepository = userScoreRepository,
            tagRepository = tagRepository
        )

        populator.populateIfEmpty()

        // Verify 4 decisions were created
        val decisions = decisionRepository.findDecisions(DecisionSearchFilters(timeRange = TimeRange.ALL))
        decisions shouldHaveSize 4
    }

    @Test
    fun `populateIfEmpty does not create data when database already has data`() {
        val decisionRepository = DecisionRepositoryImpl(jdbi)
        val optionRepository = OptionRepositoryImpl(jdbi)
        val criteriaRepository = CriteriaRepositoryImpl(jdbi)
        val userScoreRepository = UserScoreRepositoryImpl(jdbi)
        val tagRepository = TagRepositoryImpl(jdbi)

        // Create a single decision first
        decisionRepository.insert(
            decision = decisionmatrix.DecisionInput(name = "Existing Decision"),
            createdBy = "existing-user"
        )

        val populator = SampleDataPopulator(
            jdbi = jdbi,
            decisionRepository = decisionRepository,
            optionRepository = optionRepository,
            criteriaRepository = criteriaRepository,
            userScoreRepository = userScoreRepository,
            tagRepository = tagRepository
        )

        populator.populateIfEmpty()

        // Verify only 1 decision exists (the one we created, sample data was not added)
        val decisions = decisionRepository.findDecisions(DecisionSearchFilters(timeRange = TimeRange.ALL))
        decisions shouldHaveSize 1
        decisions[0].name shouldBe "Existing Decision"
    }

}
