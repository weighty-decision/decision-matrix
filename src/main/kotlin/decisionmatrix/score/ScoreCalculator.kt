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

    for (option in options) {
        var optionTotal = BigDecimal.ZERO

        for (criterion in criteria) {
            val scores = userScores.filter { it.optionId == option.id && it.criteriaId == criterion.id }

            val weightedScore = if (scores.isNotEmpty()) {
                val sum = scores.fold(BigDecimal.ZERO) { acc, s -> acc + BigDecimal(s.score) }
                val average = sum.divide(BigDecimal(scores.size), 2, RoundingMode.HALF_UP)
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

            optionTotal = optionTotal.add(weightedScore)
        }

        totalScores[option] = optionTotal
    }

    return ScoreReport(optionScores = optionScores, totalScores = totalScores)
}
