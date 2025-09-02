package decisionmatrix.routes

import decisionmatrix.DecisionInput
import decisionmatrix.db.*
import io.kotest.matchers.shouldBe
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class DecisionUiRoutesTest {

    private val jdbi = createTempDatabase()
    private val decisionRepository = DecisionRepositoryImpl(jdbi)
    private val optionRepository = OptionRepositoryImpl(jdbi)
    private val criteriaRepository = CriteriaRepositoryImpl(jdbi)
    private val userScoreRepository = UserScoreRepositoryImpl(jdbi)

    private val routes = DecisionUiRoutes(
        decisionRepository, optionRepository, criteriaRepository, userScoreRepository
    ).routes

    @Test fun `createDecision with custom score range creates decision with correct range`() {
        val request = Request(Method.POST, "/decisions")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=Test+Decision&minScore=2&maxScore=8")

        val response = routes(request)

        response.status shouldBe Status.SEE_OTHER
        val location = response.header("Location")
        val decisionId = location!!.split("/")[2].toLong()

        val decision = decisionRepository.findById(decisionId)
        decision!!.name shouldBe "Test Decision"
        decision.minScore shouldBe 2
        decision.maxScore shouldBe 8
    }

    @Test fun `createDecision with invalid score range returns error`() {
        val request = Request(Method.POST, "/decisions")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=Test+Decision&minScore=8&maxScore=2")

        val response = routes(request)

        response.status shouldBe Status.BAD_REQUEST
        response.bodyString() shouldBe "Min score must be less than max score"
    }

    @Test fun `submitMyScores validates score within range`() {
        // Create decision with custom score range
        val decision = decisionRepository.insert(
            DecisionInput(name = "Test Decision", minScore = 1, maxScore = 5)
        )
        val option = optionRepository.insert(decision.id, decisionmatrix.OptionInput("Option A"))
        val criteria = criteriaRepository.insert(decision.id, decisionmatrix.CriteriaInput("Criteria A", 1))

        // Test valid score
        val validRequest = Request(Method.POST, "/decisions/${decision.id}/my-scores?userid=testuser")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("userid=testuser&score_${option.id}_${criteria.id}=3")

        val validResponse = routes(validRequest)
        validResponse.status shouldBe Status.SEE_OTHER

        // Test invalid score (too high)
        val invalidHighRequest = Request(Method.POST, "/decisions/${decision.id}/my-scores?userid=testuser")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("userid=testuser&score_${option.id}_${criteria.id}=7")

        val invalidHighResponse = routes(invalidHighRequest)
        invalidHighResponse.status shouldBe Status.BAD_REQUEST
        invalidHighResponse.bodyString() shouldBe "Score 7 is outside the allowed range of 1-5"

        // Test invalid score (too low)
        val invalidLowRequest = Request(Method.POST, "/decisions/${decision.id}/my-scores?userid=testuser")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("userid=testuser&score_${option.id}_${criteria.id}=0")

        val invalidLowResponse = routes(invalidLowRequest)
        invalidLowResponse.status shouldBe Status.BAD_REQUEST
        invalidLowResponse.bodyString() shouldBe "Score 0 is outside the allowed range of 1-5"
    }

    @Test fun `updateDecisionName with score range updates all fields`() {
        val decision = decisionRepository.insert(
            DecisionInput(name = "Original Decision", minScore = 1, maxScore = 10)
        )

        val request = Request(Method.POST, "/decisions/${decision.id}/name")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("HX-Request", "true")
            .body("name=Updated+Decision&minScore=2&maxScore=8")

        val response = routes(request)

        response.status shouldBe Status.OK

        val updatedDecision = decisionRepository.findById(decision.id)
        updatedDecision!!.name shouldBe "Updated Decision"
        updatedDecision.minScore shouldBe 2
        updatedDecision.maxScore shouldBe 8
    }

    @Test fun `updateDecisionName with invalid score range returns error`() {
        val decision = decisionRepository.insert(
            DecisionInput(name = "Original Decision", minScore = 1, maxScore = 10)
        )

        val request = Request(Method.POST, "/decisions/${decision.id}/name")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("HX-Request", "true")
            .body("name=Updated+Decision&minScore=8&maxScore=2")

        val response = routes(request)

        response.status shouldBe Status.BAD_REQUEST
        response.bodyString() shouldBe "Min score must be less than max score"
    }
}
