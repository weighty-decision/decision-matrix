package decisionmatrix

import kotlinx.serialization.Serializable

@Serializable
data class Decision(
    val id: Long? = null,
    val name: String,
    val criteria: List<Criteria>,
    val options: List<Option>,
)

@Serializable
data class Criteria(
    val id: Long? = null,
    val decisionId: Long,
    val name: String,
    val weight: Int,
)

@Serializable
data class Option(
    val id: Long? = null,
    val decisionId: Long,
    val name: String,
)

@Serializable
data class OptionScore(
    val id: Long? = null,
    val optionId: Long,
    val score: Int,
)