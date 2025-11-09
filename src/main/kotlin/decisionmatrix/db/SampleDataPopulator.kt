package decisionmatrix.db

import decisionmatrix.CriteriaInput
import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import decisionmatrix.UserScoreInput
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

private val log = LoggerFactory.getLogger("SampleDataPopulator")

class SampleDataPopulator(
    private val jdbi: Jdbi,
    private val decisionRepository: DecisionRepository,
    private val optionRepository: OptionRepository,
    private val criteriaRepository: CriteriaRepository,
    private val userScoreRepository: UserScoreRepository
) {
    fun populateIfEmpty() {
        val isEmpty = jdbi.withHandle<Boolean, Exception> { handle ->
            val count = handle.createQuery("SELECT COUNT(*) FROM decisions")
                .mapTo(Long::class.java)
                .one()
            count == 0L
        }

        if (!isEmpty) {
            log.info("Database already contains data, skipping sample data population")
            return
        }

        log.info("Populating database with sample data...")
        populateTechStackDecision()
        populateVendorDecisionOld()
        populateVacationDecisionBySampleUser()
        populateCarPurchaseDecisionBySampleUser()
        log.info("Sample data population complete!")
    }

    /**
     * Scenario 1: Tech stack decision created by dev-user today with scores from 10 users
     */
    private fun populateTechStackDecision() {
        val decision = decisionRepository.insert(
            decision = DecisionInput(name = "Backend Framework Selection for New Project", minScore = 1, maxScore = 10),
            createdBy = "dev-user"
        )

        val criteria = listOf(
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Performance", weight = 5)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Developer Experience", weight = 4)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Community Support", weight = 3)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Learning Curve", weight = 2))
        )

        val options = listOf(
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "Spring Boot")),
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "Ktor")),
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "http4k"))
        )

        val users = listOf(
            "alice", "bob", "charlie", "diana", "ethan",
            "frank", "grace", "henry", "isabel", "james"
        )

        // Generate realistic scores - http4k slightly favored
        users.forEach { user ->
            options.forEach { option ->
                criteria.forEach { criterion ->
                    val score = when {
                        option.name == "http4k" && criterion.name == "Performance" -> (8..10).random()
                        option.name == "http4k" && criterion.name == "Developer Experience" -> (7..9).random()
                        option.name == "Spring Boot" && criterion.name == "Community Support" -> (8..10).random()
                        option.name == "Spring Boot" && criterion.name == "Learning Curve" -> (4..6).random()
                        option.name == "Ktor" && criterion.name == "Developer Experience" -> (7..9).random()
                        option.name == "Ktor" && criterion.name == "Performance" -> (7..9).random()
                        else -> (5..8).random()
                    }
                    userScoreRepository.insert(
                        decisionId = decision.id,
                        optionId = option.id,
                        criteriaId = criterion.id,
                        scoredBy = user,
                        score = UserScoreInput(score = score)
                    )
                }
            }
        }

        log.info("Created tech stack decision (ID: ${decision.id}) with 10 users")
    }

    /**
     * Scenario 2: Vendor selection decision created by dev-user 9 months ago
     */
    private fun populateVendorDecisionOld() {
        val decisionName = "Cloud Provider for Production Deployment"
        val decision = decisionRepository.insert(
            decision = DecisionInput(name = decisionName, minScore = 1, maxScore = 10),
            createdBy = "dev-user",
        )

        val criteria = listOf(
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Cost", weight = 5)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Reliability", weight = 5)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Global Coverage", weight = 3)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Ease of Use", weight = 2))
        )

        val options = listOf(
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "AWS")),
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "Google Cloud")),
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "Azure"))
        )

        // Add some scores from dev-user and a couple others
        val users = listOf("dev-user", "alice", "bob")
        users.forEach { user ->
            options.forEach { option ->
                criteria.forEach { criterion ->
                    val score = when {
                        option.name == "AWS" && criterion.name == "Reliability" -> (9..10).random()
                        option.name == "AWS" && criterion.name == "Global Coverage" -> (9..10).random()
                        option.name == "Google Cloud" && criterion.name == "Cost" -> (7..9).random()
                        option.name == "Azure" && criterion.name == "Ease of Use" -> (6..8).random()
                        else -> (5..8).random()
                    }
                    userScoreRepository.insert(
                        decisionId = decision.id,
                        optionId = option.id,
                        criteriaId = criterion.id,
                        scoredBy = user,
                        score = UserScoreInput(score = score)
                    )
                }
            }
        }

        // Update timestamps to 9 months ago
        val nineMonthsAgo = Instant.now().minus(270, ChronoUnit.DAYS)
        updateDecisionTimestamp(decisionId = decision.id, timestamp = nineMonthsAgo)

        // lock the decision
        decisionRepository.update(decision.id, decisionName, 1, 10, true)

        log.info("Created cloud provider decision (ID: ${decision.id}) from 9 months ago")
    }

    /**
     * Scenario 3: Vacation destination decision by sample-user from 2 months ago
     */
    private fun populateVacationDecisionBySampleUser() {
        val decision = decisionRepository.insert(
            decision = DecisionInput(name = "Summer Vacation Destination", minScore = 1, maxScore = 10),
            createdBy = "sample-user"
        )

        val criteria = listOf(
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Beach Quality", weight = 4)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Activities", weight = 3)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Budget", weight = 5)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Flight Duration", weight = 2))
        )

        val options = listOf(
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "Bali, Indonesia")),
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "Santorini, Greece")),
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "Cancun, Mexico")),
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "Maldives"))
        )

        // Only sample-user scored this one
        options.forEach { option ->
            criteria.forEach { criterion ->
                val score = when {
                    option.name == "Bali, Indonesia" && criterion.name == "Budget" -> (8..9).random()
                    option.name == "Santorini, Greece" && criterion.name == "Beach Quality" -> (9..10).random()
                    option.name == "Cancun, Mexico" && criterion.name == "Budget" -> (9..10).random()
                    option.name == "Cancun, Mexico" && criterion.name == "Flight Duration" -> (8..10).random()
                    option.name == "Maldives" && criterion.name == "Beach Quality" -> (10..10).random()
                    option.name == "Maldives" && criterion.name == "Budget" -> (3..5).random()
                    else -> (6..8).random()
                }
                userScoreRepository.insert(
                    decisionId = decision.id,
                    optionId = option.id,
                    criteriaId = criterion.id,
                    scoredBy = "sample-user",
                    score = UserScoreInput(score = score)
                )
            }
        }

        // Update timestamps to 2 months ago
        val twoMonthsAgo = Instant.now().minus(60, ChronoUnit.DAYS)
        updateDecisionTimestamp(decisionId = decision.id, timestamp = twoMonthsAgo)

        log.info("Created vacation decision (ID: ${decision.id}) from 2 months ago")
    }

    /**
     * Scenario 4: Car purchase decision by sample-user from 2 months ago with score from dev-user
     */
    private fun populateCarPurchaseDecisionBySampleUser() {
        val decision = decisionRepository.insert(
            decision = DecisionInput(name = "Family Car Purchase Decision", minScore = 1, maxScore = 10),
            createdBy = "sample-user"
        )

        val criteria = listOf(
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Fuel Economy", weight = 4)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Safety Rating", weight = 5)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Cargo Space", weight = 3)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Price", weight = 5)),
            criteriaRepository.insert(decisionId = decision.id, criteria = CriteriaInput(name = "Reliability", weight = 4))
        )

        val options = listOf(
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "Honda CR-V")),
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "Toyota RAV4")),
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "Mazda CX-5")),
            optionRepository.insert(decisionId = decision.id, option = OptionInput(name = "Subaru Forester"))
        )

        // Both sample-user and dev-user scored this
        val users = listOf("sample-user", "dev-user")
        users.forEach { user ->
            options.forEach { option ->
                criteria.forEach { criterion ->
                    val score = when {
                        option.name == "Honda CR-V" && criterion.name == "Reliability" -> (8..10).random()
                        option.name == "Toyota RAV4" && criterion.name == "Reliability" -> (9..10).random()
                        option.name == "Toyota RAV4" && criterion.name == "Fuel Economy" -> (7..9).random()
                        option.name == "Mazda CX-5" && criterion.name == "Price" -> (7..9).random()
                        option.name == "Subaru Forester" && criterion.name == "Safety Rating" -> (9..10).random()
                        option.name == "Subaru Forester" && criterion.name == "Cargo Space" -> (8..10).random()
                        else -> (6..8).random()
                    }
                    userScoreRepository.insert(
                        decisionId = decision.id,
                        optionId = option.id,
                        criteriaId = criterion.id,
                        scoredBy = user,
                        score = UserScoreInput(score = score)
                    )
                }
            }
        }

        // Update timestamps to 2 months ago
        val twoMonthsAgo = Instant.now().minus(60, ChronoUnit.DAYS)
        updateDecisionTimestamp(decisionId = decision.id, timestamp = twoMonthsAgo)

        log.info("Created car purchase decision (ID: ${decision.id}) from 2 months ago with dev-user score")
    }

    private fun updateDecisionTimestamp(decisionId: Long, timestamp: Instant) {
        jdbi.useHandle<Exception> { handle ->
            handle.createUpdate(
                """
                UPDATE decisions
                SET created_at = :timestamp
                WHERE id = :decisionId
                """.trimIndent()
            )
                .bind("decisionId", decisionId)
                .bind("timestamp", timestamp)
                .execute()
        }
    }
}
