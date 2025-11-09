package decisionmatrix.score

import decisionmatrix.DecisionAggregate
import decisionmatrix.Option
import decisionmatrix.UserScore
import java.math.BigDecimal
import java.math.RoundingMode


/**
 * Represents the calculated scores for a single decision.
 */
data class ScoreReport(
    val optionScores: List<CriteriaOptionScore>,
    val totalScores: Map<Option, BigDecimal>,
)

/**
 * Represents the calculated score for a single criteria and option combination.
 */
data class CriteriaOptionScore(
    val criteriaName: String,
    val criteriaWeight: Int,
    val optionName: String,
    val optionScore: BigDecimal,
)


fun DecisionAggregate.calculateOptionScores(userScores: List<UserScore>): ScoreReport {
    require(options.isNotEmpty()) { "Missing required options" }
    require(criteria.isNotEmpty()) { "Missing required criteria" }
    require(userScores.isNotEmpty()) { "Missing required scores" }

    val optionScores = mutableListOf<CriteriaOptionScore>()
    val totalScores = LinkedHashMap<Option, BigDecimal>()

    // Calculate total possible weight across all criteria
    val totalPossibleWeight = criteria.sumOf { it.weight }

    for (option in options) {
        var weightedSum = BigDecimal.ZERO
        var totalWeightScored = 0

        for (criterion in criteria) {
            val scores = userScores.filter { it.optionId == option.id && it.criteriaId == criterion.id }

            val weightedScore = if (scores.isNotEmpty()) {
                val sum = scores.fold(BigDecimal.ZERO) { acc, s -> acc + BigDecimal(s.score) }
                val average = sum.divide(BigDecimal(scores.size), 2, RoundingMode.HALF_UP)
                totalWeightScored += criterion.weight
                average.multiply(BigDecimal(criterion.weight))
            } else {
                BigDecimal.ZERO
            }

            optionScores.add(
                CriteriaOptionScore(
                    criteriaName = criterion.name,
                    criteriaWeight = criterion.weight,
                    optionName = option.name,
                    optionScore = weightedScore
                )
            )

            weightedSum = weightedSum.add(weightedScore)
        }

        // Normalize the score: (weightedSum / totalWeightScored) * totalPossibleWeight
        // This ensures that omitted scores don't penalize the option
        val normalizedScore = if (totalWeightScored > 0) {
            weightedSum
                .divide(BigDecimal(totalWeightScored), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal(totalPossibleWeight))
                .setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        totalScores[option] = normalizedScore
    }

    return ScoreReport(optionScores = optionScores, totalScores = totalScores)
}
