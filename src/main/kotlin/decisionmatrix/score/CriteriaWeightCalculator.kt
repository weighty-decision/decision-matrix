package decisionmatrix.score

import decisionmatrix.Criteria
import kotlin.math.roundToInt

/**
 * Calculates the percentage of total weight that this criterion represents.
 * Returns null if the total weight is zero or if the criteria list is empty.
 * Returns the rounded percentage (using half-up rounding) otherwise.
 */
fun Criteria.calculateWeightPercentage(allCriteria: List<Criteria>): Int? {
    if (allCriteria.isEmpty()) return null

    val totalWeight = allCriteria.sumOf { it.weight }
    if (totalWeight == 0) return null

    val percentage = (this.weight.toDouble() / totalWeight.toDouble()) * 100
    return percentage.roundToInt()
}
