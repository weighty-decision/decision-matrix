package decisionmatrix

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable
data class DecisionInput(
    val name: String
)

@Serializable
data class Decision(
    val id: Long,
    val name: String,
    val criteria: List<Criteria> = emptyList(),
    val options: List<Option> = emptyList(),
) {
    fun calculateOptionScores(optionCriteriaScores: List<OptionCriteriaScore>): Map<Option, BigDecimal> {
        require(options.isNotEmpty()) { "Missing required options" }
        require(criteria.isNotEmpty()) { "Missing required criteria" }
        require(optionCriteriaScores.isNotEmpty()) { "Missing required scores" }

        val result = LinkedHashMap<Option, BigDecimal>()

        for (option in options) {
            var optionTotal = BigDecimal.ZERO

            for (criterion in criteria) {
                val scores = optionCriteriaScores.filter { it.optionId == option.id && it.criteriaId == criterion.id }

                if (scores.isNotEmpty()) {
                    val sum = scores.fold(BigDecimal.ZERO) { acc, s -> acc + BigDecimal(s.score) }
                    val average = sum.divide(BigDecimal(scores.size), 2, RoundingMode.HALF_UP)
                    val weighted = average.multiply(BigDecimal(criterion.weight))
                    optionTotal = optionTotal.add(weighted)
                }
            }

            result[option] = optionTotal
        }

        return result
    }
}

@Serializable
data class CriteriaInput(
    val name: String,
    val weight: Int
)

@Serializable
data class Criteria(
    val id: Long,
    val decisionId: Long,
    val name: String,
    val weight: Int,
)

@Serializable
data class OptionInput(
    val name: String
)

@Serializable
data class Option(
    val id: Long,
    val decisionId: Long,
    val name: String,
)

@Serializable
data class OptionCriteriaScoreInput(
    val score: Int,
)

@Serializable
data class OptionCriteriaScore(
    val id: Long,
    val decisionId: Long,
    val optionId: Long,
    val criteriaId: Long,
    val scoredBy: String,
    val score: Int,
)