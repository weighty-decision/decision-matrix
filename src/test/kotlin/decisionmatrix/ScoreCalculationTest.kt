package decisionmatrix

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

class ScoreCalculationTest {

    @Test fun `calculateOptionScores should calculate correct weighted scores for single option and criterion`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decision = Decision(
            id = 1L,
            name = "Test Decision",
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
                score = 5
            )
        )

        val result = decision.calculateOptionScores(scores)

        result.totalScores[option] shouldBe BigDecimal(15).setScale(2, RoundingMode.HALF_UP)
    }

    @Test fun `calculateOptionScores should calculate correct weighted scores for multiple options and criteria`() {
        val criteria = listOf(
            Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3),
            Criteria(id = 2L, decisionId = 1L, name = "Quality", weight = 2)
        )
        val options = listOf(
            Option(id = 1L, decisionId = 1L, name = "Option A"),
            Option(id = 2L, decisionId = 1L, name = "Option B")
        )
        val decision = Decision(
            id = 1L,
            name = "Test Decision",
            criteria = criteria,
            options = options
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", 4), // Option A, Cost: 4
            UserScore(2L, 1L, 1L, 2L, "user1", 5), // Option A, Quality: 5
            UserScore(3L, 1L, 2L, 1L, "user1", 3), // Option B, Cost: 3
            UserScore(4L, 1L, 2L, 2L, "user1", 4)  // Option B, Quality: 4
        )

        val result = decision.calculateOptionScores(scores)

        result.totalScores[options[0]] shouldBe BigDecimal(22).setScale(2, RoundingMode.HALF_UP)
        result.totalScores[options[1]] shouldBe BigDecimal(17).setScale(2, RoundingMode.HALF_UP)
    }

    @Test fun `calculateOptionScores should average multiple scores for same option-criterion combination`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 2)
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decision = Decision(
            id = 1L,
            name = "Test Decision",
            criteria = listOf(criterion),
            options = listOf(option)
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", 3),
            UserScore(2L, 1L, 1L, 1L, "user2", 5),
            UserScore(3L, 1L, 1L, 1L, "user3", 4)
        )

        val result = decision.calculateOptionScores(scores)

        // Average: (3 + 5 + 4) / 3 = 4, Weighted: 4 * 2 = 8
        result.totalScores[option] shouldBe BigDecimal(8).setScale(2, RoundingMode.HALF_UP)
    }

    @Test fun `calculateOptionScores should handle decimal averages with proper rounding`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decision = Decision(
            id = 1L,
            name = "Test Decision",
            criteria = listOf(criterion),
            options = listOf(option)
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", 1),
            UserScore(2L, 1L, 1L, 1L, "user2", 2)
        )

        val result = decision.calculateOptionScores(scores)

        // Average: (1 + 2) / 2 = 1.5, Weighted: 1.5 * 3 = 4.5
        result.totalScores[option] shouldBe BigDecimal("4.50")
    }

    @Test fun `calculateOptionScores should handle zero weight criteria`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 0)
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decision = Decision(
            id = 1L,
            name = "Test Decision",
            criteria = listOf(criterion),
            options = listOf(option)
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", 5)
        )

        val result = decision.calculateOptionScores(scores)

        result.totalScores[option] shouldBe BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }

    @Test fun `calculateOptionScores should handle missing scores for some option-criterion combinations`() {
        val criteria = listOf(
            Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3),
            Criteria(id = 2L, decisionId = 1L, name = "Quality", weight = 2)
        )
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decision = Decision(
            id = 1L,
            name = "Test Decision",
            criteria = criteria,
            options = listOf(option)
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", 4) // Only Cost score, no Quality score
        )

        val result = decision.calculateOptionScores(scores)

        // Only Cost contributes: 4 * 3 = 12, Quality contributes 0 (no scores)
        result.totalScores[option] shouldBe BigDecimal(12).setScale(2, RoundingMode.HALF_UP)
    }

    @Test fun `calculateOptionScores should return zero for options with no scores`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val options = listOf(
            Option(id = 1L, decisionId = 1L, name = "Option A"),
            Option(id = 2L, decisionId = 1L, name = "Option B")
        )
        val decision = Decision(
            id = 1L,
            name = "Test Decision",
            criteria = listOf(criterion),
            options = options
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", 4) // Only for Option A
        )

        val result = decision.calculateOptionScores(scores)

        result.totalScores[options[0]] shouldBe BigDecimal(12).setScale(2, RoundingMode.HALF_UP)
        result.totalScores[options[1]] shouldBe BigDecimal.ZERO
    }

    @Test fun `calculateOptionScores should throw exception when options are empty`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val decision = Decision(
            id = 1L,
            name = "Test Decision",
            criteria = listOf(criterion),
            options = emptyList()
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", 4)
        )

        // When & Then
        val exception = shouldThrow<IllegalArgumentException> {
            decision.calculateOptionScores(scores)
        }
        exception.message shouldBe "Missing required options"
    }

    @Test fun `calculateOptionScores should throw exception when criteria are empty`() {
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decision = Decision(
            id = 1L,
            name = "Test Decision",
            criteria = emptyList(),
            options = listOf(option)
        )
        val scores = listOf(
            UserScore(1L, 1L, 1L, 1L, "user1", 4)
        )

        val exception = shouldThrow<IllegalArgumentException> {
            decision.calculateOptionScores(scores)
        }
        exception.message shouldBe "Missing required criteria"
    }

    @Test fun `calculateOptionScores should throw exception when scores are empty`() {
        // Given
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val option = Option(id = 1L, decisionId = 1L, name = "Option A")
        val decision = Decision(
            id = 1L,
            name = "Test Decision",
            criteria = listOf(criterion),
            options = listOf(option)
        )

        val exception = shouldThrow<IllegalArgumentException> {
            decision.calculateOptionScores(emptyList())
        }
        exception.message shouldBe "Missing required scores"
    }

    @Test fun `calculateOptionScores should maintain option order in result map`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 1)
        val options = listOf(
            Option(id = 3L, decisionId = 1L, name = "Option C"),
            Option(id = 1L, decisionId = 1L, name = "Option A"),
            Option(id = 2L, decisionId = 1L, name = "Option B")
        )
        val decision = Decision(
            id = 1L,
            name = "Test Decision",
            criteria = listOf(criterion),
            options = options
        )
        val scores = listOf(
            UserScore(1L, 1L, 3L, 1L, "user1", 3),
            UserScore(2L, 1L, 1L, 1L, "user1", 1),
            UserScore(3L, 1L, 2L, 1L, "user1", 2)
        )

        val result = decision.calculateOptionScores(scores)

        result.totalScores.keys.toList() shouldBe options
    }
}
