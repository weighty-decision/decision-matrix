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
    val locked: Boolean = false,
)

@Serializable
data class Decision(
    val id: Long,
    val name: String,
    val minScore: Int = DEFAULT_MIN_SCORE,
    val maxScore: Int = DEFAULT_MAX_SCORE,
    val locked: Boolean = false,
    val createdBy: String? = null,
    @Contextual val createdAt: Instant? = null,
) {
    /**
     * Checks if a user can modify a decision when you already have a hydrated Decision object.
     */
    fun canBeModifiedBy(userId: String): Boolean {
        return createdBy == userId
    }
}

@Serializable
data class DecisionAggregate(
    val decision: Decision,
    val criteria: List<Criteria> = emptyList(),
    val options: List<Option> = emptyList(),
) {
    // Delegate properties to the composed Decision
    val id: Long get() = decision.id
    val name: String get() = decision.name
    val minScore: Int get() = decision.minScore
    val maxScore: Int get() = decision.maxScore
    val locked: Boolean get() = decision.locked
    val createdBy: String? get() = decision.createdBy
    val createdAt: Instant? get() = decision.createdAt

    fun canBeModifiedBy(userId: String): Boolean = decision.canBeModifiedBy(userId)
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
    @Contextual val createdAt: Instant? = null,
    val score: Int,
) {
    fun canModifyUserScore(userId: String): Boolean {
        return scoredBy == userId
    }
}
