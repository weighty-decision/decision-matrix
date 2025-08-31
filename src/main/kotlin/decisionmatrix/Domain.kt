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
    val decisionId: Long,
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
    val decisionId: Long,
    val name: String
)

@Serializable
data class Option(
    val id: Long,
    val decisionId: Long,
    val name: String,
)

@Serializable
data class OptionScoreInput(
    val optionId: Long,
    val score: Int,
)

@Serializable
data class OptionScore(
    val id: Long,
    val optionId: Long,
    val score: Int,
)