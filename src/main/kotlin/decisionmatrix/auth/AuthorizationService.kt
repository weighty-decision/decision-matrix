package decisionmatrix.auth

import decisionmatrix.db.DecisionRepository

/**
 * Service for handling entity authorization logic.
 * Encapsulates the rules for who can modify what entities.
 */
class AuthorizationService(
    private val decisionRepository: DecisionRepository
) {

    /**
     * Checks if a user can modify a decision.
     * Currently: user must be the creator of the decision.
     * Future: will also allow modification with a secret code.
     */
    fun canModifyDecision(decisionId: Long, userId: String): Boolean {
        val decision = decisionRepository.getDecision(decisionId) ?: return false
        return decision.canBeModifiedBy(userId)
    }
    
    /**
     * Checks if a user can modify an option.
     * Rule: user can modify if they can modify the decision that contains the option.
     */
    fun canModifyOption(decisionId: Long, userId: String): Boolean {
        return canModifyDecision(decisionId, userId)
    }
    
    /**
     * Checks if a user can modify criteria.
     * Rule: user can modify if they can modify the decision that contains the criteria.
     */
    fun canModifyCriteria(decisionId: Long, userId: String): Boolean {
        return canModifyDecision(decisionId, userId)
    }
    
}
