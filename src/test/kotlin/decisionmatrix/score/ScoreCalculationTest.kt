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

class ScoreCalculationTest {

    @Test
    fun `calculateOptionScores should calculate correct weighted scores for single option and criterion`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decisionAggregate = DecisionAggregate(
            decision = Decision(id = 1L, name = "Test Decision"),
            criteria = listOf(criterion),
            options = listOf(option)
        )
        val scores = listOf(
            UserScore(
                id = 1L,
                decisionId = 1L,
                optionId = 1L,
                criteriaId = 1L,
                scoredBy = "user1",
                createdAt = null,
                score = 5
            )
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
            decision = Decision(id = 1L, name = "Test Decision"),
            criteria = criteria,
            options = options
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", null, 4), // Option A, Cost: 4
            UserScore(2L, 1L, 1L, 2L, "user1", null, 5), // Option A, Quality: 5
            UserScore(3L, 1L, 2L, 1L, "user1", null, 3), // Option B, Cost: 3
            UserScore(4L, 1L, 2L, 2L, "user1", null, 4)  // Option B, Quality: 4
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
            decision = Decision(id = 1L, name = "Test Decision"),
            criteria = listOf(criterion),
            options = listOf(option)
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", null, 3),
            UserScore(2L, 1L, 1L, 1L, "user2", null, 5),
            UserScore(3L, 1L, 1L, 1L, "user3", null, 4)
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
            decision = Decision(id = 1L, name = "Test Decision"),
            criteria = listOf(criterion),
            options = listOf(option)
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", null, 1),
            UserScore(2L, 1L, 1L, 1L, "user2", null, 2)
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
            decision = Decision(id = 1L, name = "Test Decision"),
            criteria = listOf(criterion),
            options = listOf(option)
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", null, 5)
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
            decision = Decision(id = 1L, name = "Test Decision"),
            criteria = criteria,
            options = listOf(option)
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", null, 4) // Only Cost score, no Quality score
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
            decision = Decision(id = 1L, name = "Test Decision"),
            criteria = listOf(criterion),
            options = options
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", null, 4) // Only for Option A
        )

        val result = decisionAggregate.calculateOptionScores(scores)

        result.totalScores[options[0]] shouldBe BigDecimal(12).setScale(2, RoundingMode.HALF_UP)
        result.totalScores[options[1]] shouldBe BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }

    @Test
    fun `calculateOptionScores should throw exception when options are empty`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val decisionAggregate = DecisionAggregate(
            decision = Decision(id = 1L, name = "Test Decision"),
            criteria = listOf(criterion),
            options = emptyList()
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", null, 4)
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
            decision = Decision(id = 1L, name = "Test Decision"),
            criteria = emptyList(),
            options = listOf(option)
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", null, 4)
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
            decision = Decision(id = 1L, name = "Test Decision"),
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
            decision = Decision(id = 1L, name = "Test Decision"),
            criteria = listOf(criterion),
            options = options
        )
        val scores = listOf(
            UserScore(1L, 1L, 3L, 1L, "user1", null, 3),
            UserScore(2L, 1L, 1L, 1L, "user1", null, 1),
            UserScore(3L, 1L, 2L, 1L, "user1", null, 2)
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
            decision = Decision(id = 1L, name = "Test Decision"),
            criteria = criteria,
            options = options
        )
        val scores = listOf(
            // User1 scores only Option A, and omits Cost
            UserScore(1L, 1L, 1L, 1L, "user1", null, 10), // Option A, Performance: 10
            // Cost omitted for user1

            // User2 scores both options completely
            UserScore(2L, 1L, 1L, 1L, "user2", null, 8),  // Option A, Performance: 8
            UserScore(3L, 1L, 1L, 2L, "user2", null, 6),  // Option A, Cost: 6
            UserScore(4L, 1L, 2L, 1L, "user2", null, 7),  // Option B, Performance: 7
            UserScore(5L, 1L, 2L, 2L, "user2", null, 9)   // Option B, Cost: 9
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
            decision = Decision(id = 1L, name = "Test Decision"),
            criteria = criteria,
            options = options
        )
        val scores = listOf(
            // Option A: Only Performance and Usability scored (Cost omitted)
            UserScore(1L, 1L, 1L, 1L, "user1", null, 10), // Option A, Performance: 10
            UserScore(2L, 1L, 1L, 3L, "user1", null, 10), // Option A, Usability: 10
            // Cost omitted

            // Option B: Only Cost and Usability scored (Performance omitted)
            UserScore(3L, 1L, 2L, 2L, "user1", null, 10), // Option B, Cost: 10
            UserScore(4L, 1L, 2L, 3L, "user1", null, 10)  // Option B, Usability: 10
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
