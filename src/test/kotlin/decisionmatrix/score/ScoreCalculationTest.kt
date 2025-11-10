package decisionmatrix.score

import decisionmatrix.Criteria
import decisionmatrix.Decision
import decisionmatrix.DecisionAggregate
import decisionmatrix.Option
import decisionmatrix.UserScore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

class ScoreCalculationTest {

    // Test helpers with sensible defaults
    private fun testDecision(
        id: Long = 1L,
        name: String = "Test Decision",
        minScore: Int = 1,
        maxScore: Int = 10,
        locked: Boolean = false,
        createdBy: String = "test-user",
        createdAt: Instant = Instant.EPOCH,
    ) = Decision(
        id = id,
        name = name,
        minScore = minScore,
        maxScore = maxScore,
        locked = locked,
        createdBy = createdBy,
        createdAt = createdAt,
    )

    private fun testUserScore(
        id: Long,
        decisionId: Long = 1L,
        optionId: Long,
        criteriaId: Long,
        scoredBy: String,
        score: Int,
        createdAt: Instant = Instant.EPOCH,
    ) = UserScore(
        id = id,
        decisionId = decisionId,
        optionId = optionId,
        criteriaId = criteriaId,
        scoredBy = scoredBy,
        createdAt = createdAt,
        score = score,
    )

    @Test
    fun `calculateOptionScores should calculate correct weighted scores for single option and criterion`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = listOf(criterion),
            options = listOf(option)
        )
        val scores = listOf(
            testUserScore(id = 1L, optionId = 1L, criteriaId = 1L, scoredBy = "user1", score = 5)
        )

        val result = decisionAggregate.calculateOptionScores(scores)

        result.totalScores[option] shouldBe BigDecimal(15).setScale(2, RoundingMode.HALF_UP)
    }

    @Test
    fun `calculateOptionScores should calculate correct weighted scores for multiple options and criteria`() {
        val criteria = listOf(
            Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3),
            Criteria(id = 2L, decisionId = 1L, name = "Quality", weight = 2)
        )
        val options = listOf(
            Option(id = 1L, decisionId = 1L, name = "Option A"),
            Option(id = 2L, decisionId = 1L, name = "Option B")
        )
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = criteria,
            options = options
        )
        val scores = listOf(
            testUserScore(id = 1L, optionId = 1L, criteriaId = 1L, scoredBy = "user1", score = 4), // Option A, Cost: 4
            testUserScore(id = 2L, optionId = 1L, criteriaId = 2L, scoredBy = "user1", score = 5), // Option A, Quality: 5
            testUserScore(id = 3L, optionId = 2L, criteriaId = 1L, scoredBy = "user1", score = 3), // Option B, Cost: 3
            testUserScore(id = 4L, optionId = 2L, criteriaId = 2L, scoredBy = "user1", score = 4)  // Option B, Quality: 4
        )

        val result = decisionAggregate.calculateOptionScores(scores)

        result.totalScores[options[0]] shouldBe BigDecimal(22).setScale(2, RoundingMode.HALF_UP)
        result.totalScores[options[1]] shouldBe BigDecimal(17).setScale(2, RoundingMode.HALF_UP)
    }

    @Test
    fun `calculateOptionScores should average multiple scores for same option-criterion combination`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 2)
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = listOf(criterion),
            options = listOf(option)
        )
        val scores = listOf(
            testUserScore(id = 1L, optionId = 1L, criteriaId = 1L, scoredBy = "user1", score = 3),
            testUserScore(id = 2L, optionId = 1L, criteriaId = 1L, scoredBy = "user2", score = 5),
            testUserScore(id = 3L, optionId = 1L, criteriaId = 1L, scoredBy = "user3", score = 4)
        )

        val result = decisionAggregate.calculateOptionScores(scores)

        // Average: (3 + 5 + 4) / 3 = 4, Weighted: 4 * 2 = 8
        result.totalScores[option] shouldBe BigDecimal(8).setScale(2, RoundingMode.HALF_UP)
    }

    @Test
    fun `calculateOptionScores should handle decimal averages with proper rounding`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = listOf(criterion),
            options = listOf(option)
        )
        val scores = listOf(
            testUserScore(id = 1L, optionId = 1L, criteriaId = 1L, scoredBy = "user1", score = 1),
            testUserScore(id = 2L, optionId = 1L, criteriaId = 1L, scoredBy = "user2", score = 2)
        )

        val result = decisionAggregate.calculateOptionScores(scores)

        // Average: (1 + 2) / 2 = 1.5, Weighted: 1.5 * 3 = 4.5
        result.totalScores[option] shouldBe BigDecimal("4.50")
    }

    @Test
    fun `calculateOptionScores should handle zero weight criteria`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 0)
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = listOf(criterion),
            options = listOf(option)
        )
        val scores = listOf(
            testUserScore(id = 1L, optionId = 1L, criteriaId = 1L, scoredBy = "user1", score = 5)
        )

        val result = decisionAggregate.calculateOptionScores(scores)

        result.totalScores[option] shouldBe BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }

    @Test
    fun `calculateOptionScores should handle missing scores for some option-criterion combinations`() {
        val criteria = listOf(
            Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3),
            Criteria(id = 2L, decisionId = 1L, name = "Quality", weight = 2)
        )
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = criteria,
            options = listOf(option)
        )
        val scores = listOf(
            testUserScore(id = 1L, optionId = 1L, criteriaId = 1L, scoredBy = "user1", score = 4) // Only Cost score, no Quality score
        )

        val result = decisionAggregate.calculateOptionScores(scores)

        // Cost weighted score: 4 * 3 = 12
        // Total weight scored: 3 (only Cost)
        // Total possible weight: 5 (Cost + Quality)
        // Normalized: (12 / 3) * 5 = 4 * 5 = 20
        // This ensures omitted scores don't penalize the option
        result.totalScores[option] shouldBe BigDecimal(20).setScale(2, RoundingMode.HALF_UP)
    }

    @Test
    fun `calculateOptionScores should return zero for options with no scores`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val options = listOf(
            Option(id = 1L, decisionId = 1L, name = "Option A"),
            Option(id = 2L, decisionId = 1L, name = "Option B")
        )
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = listOf(criterion),
            options = options
        )
        val scores = listOf(
            testUserScore(id = 1L, optionId = 1L, criteriaId = 1L, scoredBy = "user1", score = 4) // Only for Option A
        )

        val result = decisionAggregate.calculateOptionScores(scores)

        result.totalScores[options[0]] shouldBe BigDecimal(12).setScale(2, RoundingMode.HALF_UP)
        result.totalScores[options[1]] shouldBe BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }

    @Test
    fun `calculateOptionScores should throw exception when options are empty`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = listOf(criterion),
            options = emptyList()
        )
        val scores = listOf(
            testUserScore(id = 1L, optionId = 1L, criteriaId = 1L, scoredBy = "user1", score = 4)
        )

        // When & Then
        val exception = shouldThrow<IllegalArgumentException> {
            decisionAggregate.calculateOptionScores(scores)
        }
        exception.message shouldBe "Missing required options"
    }

    @Test
    fun `calculateOptionScores should throw exception when criteria are empty`() {
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = emptyList(),
            options = listOf(option)
        )
        val scores = listOf(
            testUserScore(id = 1L, optionId = 1L, criteriaId = 1L, scoredBy = "user1", score = 4)
        )

        val exception = shouldThrow<IllegalArgumentException> {
            decisionAggregate.calculateOptionScores(scores)
        }
        exception.message shouldBe "Missing required criteria"
    }

    @Test
    fun `calculateOptionScores should throw exception when scores are empty`() {
        // Given
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = listOf(criterion),
            options = listOf(option)
        )

        val exception = shouldThrow<IllegalArgumentException> {
            decisionAggregate.calculateOptionScores(emptyList())
        }
        exception.message shouldBe "Missing required scores"
    }

    @Test
    fun `calculateOptionScores should maintain option order in result map`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 1)
        val options = listOf(
            Option(id = 3L, decisionId = 1L, name = "Option C"),
            Option(id = 1L, decisionId = 1L, name = "Option A"),
            Option(id = 2L, decisionId = 1L, name = "Option B")
        )
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = listOf(criterion),
            options = options
        )
        val scores = listOf(
            testUserScore(id = 1L, optionId = 3L, criteriaId = 1L, scoredBy = "user1", score = 3),
            testUserScore(id = 2L, optionId = 1L, criteriaId = 1L, scoredBy = "user1", score = 1),
            testUserScore(id = 3L, optionId = 2L, criteriaId = 1L, scoredBy = "user1", score = 2)
        )

        val result = decisionAggregate.calculateOptionScores(scores)

        result.totalScores.keys.toList() shouldBe options
    }

    @Test fun `calculateOptionScores should handle multiple users with partial scoring patterns`() {
        // Scenario: Two options, two criteria, two users
        // User1 scores only Option A (with one criterion omitted)
        // User2 scores both options completely
        val criteria = listOf(
            Criteria(id = 1L, decisionId = 1L, name = "Performance", weight = 5),
            Criteria(id = 2L, decisionId = 1L, name = "Cost", weight = 3)
        )
        val options = listOf(
            Option(id = 1L, decisionId = 1L, name = "Option A"),
            Option(id = 2L, decisionId = 1L, name = "Option B")
        )
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = criteria,
            options = options
        )
        val scores = listOf(
            // User1 scores only Option A, and omits Cost
            testUserScore(id = 1L, optionId = 1L, criteriaId = 1L, scoredBy = "user1", score = 10), // Option A, Performance: 10
            // Cost omitted for user1

            // User2 scores both options completely
            testUserScore(id = 2L, optionId = 1L, criteriaId = 1L, scoredBy = "user2", score = 8),  // Option A, Performance: 8
            testUserScore(id = 3L, optionId = 1L, criteriaId = 2L, scoredBy = "user2", score = 6),  // Option A, Cost: 6
            testUserScore(id = 4L, optionId = 2L, criteriaId = 1L, scoredBy = "user2", score = 7),  // Option B, Performance: 7
            testUserScore(id = 5L, optionId = 2L, criteriaId = 2L, scoredBy = "user2", score = 9)   // Option B, Cost: 9
        )

        val result = decisionAggregate.calculateOptionScores(scores)

        // Option A calculation:
        // - Performance: avg(10, 8) = 9, weighted = 9 * 5 = 45
        // - Cost: avg(6) = 6, weighted = 6 * 3 = 18
        // - Weighted sum: 45 + 18 = 63
        // - Total weight scored: 5 + 3 = 8 (both criteria scored by at least one user)
        // - Total possible weight: 5 + 3 = 8
        // - Normalized: (63 / 8) * 8 = 63
        result.totalScores[options[0]] shouldBe BigDecimal("63.00")

        // Option B calculation:
        // - Performance: avg(7) = 7, weighted = 7 * 5 = 35
        // - Cost: avg(9) = 9, weighted = 9 * 3 = 27
        // - Weighted sum: 35 + 27 = 62
        // - Total weight scored: 5 + 3 = 8
        // - Total possible weight: 5 + 3 = 8
        // - Normalized: (62 / 8) * 8 = 62
        result.totalScores[options[1]] shouldBe BigDecimal("62.00")
    }

    @Test fun `calculateOptionScores should normalize independently per option when users omit different criteria`() {
        // Scenario: Tests that normalization happens per-option, not globally
        // Two options, three criteria
        // User1 omits different criteria for each option
        val criteria = listOf(
            Criteria(id = 1L, decisionId = 1L, name = "Performance", weight = 5),
            Criteria(id = 2L, decisionId = 1L, name = "Cost", weight = 3),
            Criteria(id = 3L, decisionId = 1L, name = "Usability", weight = 4)
        )
        val options = listOf(
            Option(id = 1L, decisionId = 1L, name = "Option A"),
            Option(id = 2L, decisionId = 1L, name = "Option B")
        )
        val decisionAggregate = DecisionAggregate(
            decision = testDecision(),
            criteria = criteria,
            options = options
        )
        val scores = listOf(
            // Option A: Only Performance and Usability scored (Cost omitted)
            testUserScore(id = 1L, optionId = 1L, criteriaId = 1L, scoredBy = "user1", score = 10), // Option A, Performance: 10
            testUserScore(id = 2L, optionId = 1L, criteriaId = 3L, scoredBy = "user1", score = 10), // Option A, Usability: 10
            // Cost omitted

            // Option B: Only Cost and Usability scored (Performance omitted)
            testUserScore(id = 3L, optionId = 2L, criteriaId = 2L, scoredBy = "user1", score = 10), // Option B, Cost: 10
            testUserScore(id = 4L, optionId = 2L, criteriaId = 3L, scoredBy = "user1", score = 10)  // Option B, Usability: 10
            // Performance omitted
        )

        val result = decisionAggregate.calculateOptionScores(scores)

        // Option A calculation:
        // - Performance: 10 * 5 = 50
        // - Usability: 10 * 4 = 40
        // - Weighted sum: 50 + 40 = 90
        // - Total weight scored: 5 + 4 = 9
        // - Total possible weight: 5 + 3 + 4 = 12
        // - Normalized: (90 / 9) * 12 = 10 * 12 = 120
        result.totalScores[options[0]] shouldBe BigDecimal("120.00")

        // Option B calculation:
        // - Cost: 10 * 3 = 30
        // - Usability: 10 * 4 = 40
        // - Weighted sum: 30 + 40 = 70
        // - Total weight scored: 3 + 4 = 7
        // - Total possible weight: 5 + 3 + 4 = 12
        // - Normalized: (70 / 7) * 12 = 10 * 12 = 120
        result.totalScores[options[1]] shouldBe BigDecimal("120.00")

        // Both options get perfect score of 120 since all evaluated criteria were scored 10/10
        // This validates that omitted criteria don't penalize options
    }
}
