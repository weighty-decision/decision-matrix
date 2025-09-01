package decisionmatrix

import kotlinx.serialization.Serializable

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
)

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