package decisionmatrix.auth

import decisionmatrix.Decision
import decisionmatrix.UserScore
import decisionmatrix.db.DecisionRepository
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthorizationServiceTest {
    
    private val mockDecisionRepository = object : DecisionRepository {
        private val testDecision = Decision(
            id = 1L,
            name = "Test Decision",
            createdBy = "user1",
            createdAt = Instant.now()
        )
        
        override fun findById(id: Long): Decision? {
            return if (id == 1L) testDecision else null
        }
    }
    
    private val authorizationService = AuthorizationService(mockDecisionRepository)
    
    @Test fun `canModifyDecision returns true when user is creator`() {
        authorizationService.canModifyDecision(1L, "user1") shouldBe true
    }
    
    @Test fun `canModifyDecision returns false when user is not creator`() {
        authorizationService.canModifyDecision(1L, "user2") shouldBe false
    }
    
    @Test fun `canModifyDecision returns false when decision does not exist`() {
        authorizationService.canModifyDecision(999L, "user1") shouldBe false
    }
    
    @Test fun `canModifyOption delegates to canModifyDecision`() {
        authorizationService.canModifyOption(1L, "user1") shouldBe true
        authorizationService.canModifyOption(1L, "user2") shouldBe false
    }
    
    @Test fun `canModifyCriteria delegates to canModifyDecision`() {
        authorizationService.canModifyCriteria(1L, "user1") shouldBe true
        authorizationService.canModifyCriteria(1L, "user2") shouldBe false
    }
    

    @Test fun `canModifyUserScore returns true when user is scorer`() {
        val userScore = UserScore(
            id = 1L,
            decisionId = 1L,
            optionId = 1L,
            criteriaId = 1L,
            scoredBy = "user1",
            score = 5,
            createdAt = Instant.now()
        )
        
        userScore.canModifyUserScore("user1") shouldBe true
    }
    
    @Test fun `canModifyUserScore returns false when user is not scorer`() {
        val userScore = UserScore(
            id = 1L,
            decisionId = 1L,
            optionId = 1L,
            criteriaId = 1L,
            scoredBy = "user1",
            score = 5,
            createdAt = Instant.now()
        )
        
        userScore.canModifyUserScore("user2") shouldBe false
    }
}
