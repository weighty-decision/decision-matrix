package decisionmatrix

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

const val DEFAULT_MIN_SCORE: Int = 1
const val DEFAULT_MAX_SCORE: Int = 10

@Serializable
data class DecisionInput(
    val name: String,
    val minScore: Int = DEFAULT_MIN_SCORE,
    val maxScore: Int = DEFAULT_MAX_SCORE,
)

@Serializable
data class Decision(
    val id: Long,
    val name: String,
    val minScore: Int = DEFAULT_MIN_SCORE,
    val maxScore: Int = DEFAULT_MAX_SCORE,
    val createdBy: String? = null,
    @Contextual val createdAt: Instant? = null,
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
data class UserScoreInput(
    val score: Int,
)

@Serializable
data class UserScore(
    val id: Long,
    val decisionId: Long,
    val optionId: Long,
    val criteriaId: Long,
    val scoredBy: String,
    val score: Int,
)
