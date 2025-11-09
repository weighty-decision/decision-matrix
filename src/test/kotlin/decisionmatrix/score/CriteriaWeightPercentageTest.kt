package decisionmatrix.score

import decisionmatrix.Criteria
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CriteriaWeightPercentageTest {

    @Test
    fun `calculateWeightPercentage should return 100 for single criterion`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 5)
        val allCriteria = listOf(criterion)

        val percentage = criterion.calculateWeightPercentage(allCriteria)

        percentage shouldBe 100
    }

    @Test
    fun `calculateWeightPercentage should calculate correct percentage for two criteria`() {
        val criterion1 = Criteria(id = 1L, decisionId = 1L, name = "Cost", weight = 3)
        val criterion2 = Criteria(id = 2L, decisionId = 1L, name = "Quality", weight = 7)
        val allCriteria = listOf(criterion1, criterion2)

        val percentage1 = criterion1.calculateWeightPercentage(allCriteria)
        val percentage2 = criterion2.calculateWeightPercentage(allCriteria)

        percentage1 shouldBe 30
        percentage2 shouldBe 70
    }

    @Test
    fun `calculateWeightPercentage should round half up`() {
        val criterion1 = Criteria(id = 1L, decisionId = 1L, name = "A", weight = 1)
        val criterion2 = Criteria(id = 2L, decisionId = 1L, name = "B", weight = 2)
        val allCriteria = listOf(criterion1, criterion2)

        val percentage1 = criterion1.calculateWeightPercentage(allCriteria)

        // 1/3 = 0.333... -> 33.333...% -> rounds to 33%
        percentage1 shouldBe 33
    }

    @Test
    fun `calculateWeightPercentage should round half up for 0_5 case`() {
        val criterion1 = Criteria(id = 1L, decisionId = 1L, name = "A", weight = 1)
        val criterion2 = Criteria(id = 2L, decisionId = 1L, name = "B", weight = 1)
        val criterion3 = Criteria(id = 3L, decisionId = 1L, name = "C", weight = 4)
        val allCriteria = listOf(criterion1, criterion2, criterion3)

        val percentage1 = criterion1.calculateWeightPercentage(allCriteria)

        // 1/6 = 0.1666... -> 16.666...% -> rounds to 17%
        percentage1 shouldBe 17
    }

    @Test
    fun `calculateWeightPercentage should return null when total weight is zero`() {
        val criterion1 = Criteria(id = 1L, decisionId = 1L, name = "A", weight = 0)
        val criterion2 = Criteria(id = 2L, decisionId = 1L, name = "B", weight = 0)
        val allCriteria = listOf(criterion1, criterion2)

        val percentage1 = criterion1.calculateWeightPercentage(allCriteria)

        percentage1 shouldBe null
    }

    @Test
    fun `calculateWeightPercentage should return null when criterion has zero weight`() {
        val criterion1 = Criteria(id = 1L, decisionId = 1L, name = "A", weight = 0)
        val criterion2 = Criteria(id = 2L, decisionId = 1L, name = "B", weight = 5)
        val allCriteria = listOf(criterion1, criterion2)

        val percentage1 = criterion1.calculateWeightPercentage(allCriteria)

        percentage1 shouldBe 0
    }

    @Test
    fun `calculateWeightPercentage should return null for empty criteria list`() {
        val criterion = Criteria(id = 1L, decisionId = 1L, name = "A", weight = 5)
        val allCriteria = emptyList<Criteria>()

        val percentage = criterion.calculateWeightPercentage(allCriteria)

        percentage shouldBe null
    }

    @Test
    fun `calculateWeightPercentage should handle three criteria with different weights`() {
        val criterion1 = Criteria(id = 1L, decisionId = 1L, name = "A", weight = 2)
        val criterion2 = Criteria(id = 2L, decisionId = 1L, name = "B", weight = 3)
        val criterion3 = Criteria(id = 3L, decisionId = 1L, name = "C", weight = 5)
        val allCriteria = listOf(criterion1, criterion2, criterion3)

        val percentage1 = criterion1.calculateWeightPercentage(allCriteria)
        val percentage2 = criterion2.calculateWeightPercentage(allCriteria)
        val percentage3 = criterion3.calculateWeightPercentage(allCriteria)

        // Total weight: 2 + 3 + 5 = 10
        percentage1 shouldBe 20  // 2/10 = 20%
        percentage2 shouldBe 30  // 3/10 = 30%
        percentage3 shouldBe 50  // 5/10 = 50%
    }
}
