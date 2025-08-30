package decisionmatrix

data class Decision(
    val id: Long? = null,
    val name: String,
    val criteria: List<Criteria>,
    val options: List<Option>,
)

data class Criteria(
    val id: Long? = null,
    val decisionId: Long,
    val name: String,
    val weight: Int,
)

data class Option(
    val id: Long? = null,
    val decisionId: Long,
    val name: String,
)

data class OptionScore(
    val id: Long? = null,
    val optionId: Long,
    val score: Int,
)