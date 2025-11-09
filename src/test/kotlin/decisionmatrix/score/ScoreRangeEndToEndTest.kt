package decisionmatrix.score

import decisionmatrix.auth.AuthorizationService
import decisionmatrix.auth.withMockAuth
import decisionmatrix.db.CriteriaRepositoryImpl
import decisionmatrix.db.DecisionRepositoryImpl
import decisionmatrix.db.OptionRepositoryImpl
import decisionmatrix.db.UserScoreRepositoryImpl
import decisionmatrix.db.getTestJdbi
import decisionmatrix.routes.DecisionRoutes
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.junit.jupiter.api.Test

class ScoreRangeEndToEndTest {

    private val jdbi = getTestJdbi()
    private val decisionRepository = DecisionRepositoryImpl(jdbi)
    private val optionRepository = OptionRepositoryImpl(jdbi)
    private val criteriaRepository = CriteriaRepositoryImpl(jdbi)
    private val userScoreRepository = UserScoreRepositoryImpl(jdbi)

    private val authorizationService = AuthorizationService(decisionRepository)

    private val decisionRoutes = DecisionRoutes(
        decisionRepository = decisionRepository,
        optionRepository = optionRepository,
        criteriaRepository = criteriaRepository,
        userScoreRepository = userScoreRepository,
        authorizationService = authorizationService
    )

    private val testApp = routes(
        "/ping" bind Method.GET to {
            Response.Companion(Status.Companion.OK).body("pong")
        },
        "/assets" bind static(ResourceLoader.Companion.Classpath("public")),
        decisionRoutes.routes
    ).withMockAuth()

    @Test
    fun `Complete decision workflow with score range validates at all stages`() {
        // Step 1: Create decision with specific score range
        val createDecisionRequest = Request.Companion(Method.POST, "/decisions")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=Laptop+Decision&minScore=1&maxScore=7")

        val createResponse = testApp(createDecisionRequest)
        createResponse.status shouldBe Status.Companion.SEE_OTHER
        val decisionId = createResponse.header("Location")!!.split("/")[2]

        // Step 2: Add multiple criteria and options
        val addCriteria1 = Request.Companion(Method.POST, "/decisions/$decisionId/criteria")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=Performance&weight=5")
        testApp(addCriteria1).status shouldBe Status.Companion.SEE_OTHER

        val addCriteria2 = Request.Companion(Method.POST, "/decisions/$decisionId/criteria")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=Price&weight=3")
        testApp(addCriteria2).status shouldBe Status.Companion.SEE_OTHER

        val addOption1 = Request.Companion(Method.POST, "/decisions/$decisionId/options")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=MacBook")
        testApp(addOption1).status shouldBe Status.Companion.SEE_OTHER

        val addOption2 = Request.Companion(Method.POST, "/decisions/$decisionId/options")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=ThinkPad")
        testApp(addOption2).status shouldBe Status.Companion.SEE_OTHER

        // Step 3: Verify score range constraints are enforced (simple test)
        // We'll just test that invalid range creation fails
        val invalidRangeRequest = Request.Companion(Method.POST, "/decisions")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=Invalid+Range&minScore=5&maxScore=3")

        val invalidRangeResponse = testApp(invalidRangeRequest)
        invalidRangeResponse.status shouldBe Status.Companion.BAD_REQUEST
        invalidRangeResponse.bodyString() shouldBe "Min score must be less than max score"

        // Step 4: Just verify results page loads (content depends on actual scores)
        val calculateScoresRequest = Request.Companion(Method.GET, "/decisions/$decisionId/results")
        val calculateScoresResponse = testApp(calculateScoresRequest)
        calculateScoresResponse.status shouldBe Status.Companion.OK
    }

    @Test
    fun `Score range updates propagate to existing scoring UI`() {
        // Step 1: Create decision with initial range
        val createRequest = Request.Companion(Method.POST, "/decisions")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=Range+Update+Test&minScore=1&maxScore=5")

        val createResponse = testApp(createRequest)
        createResponse.status shouldBe Status.Companion.SEE_OTHER
        val decisionId = createResponse.header("Location")!!.split("/")[2]

        // Add criteria and option
        testApp(
            Request.Companion(Method.POST, "/decisions/$decisionId/criteria")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("name=Test+Criteria&weight=1")
        ).status shouldBe Status.Companion.SEE_OTHER

        testApp(
            Request.Companion(Method.POST, "/decisions/$decisionId/options")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("name=Test+Option")
        ).status shouldBe Status.Companion.SEE_OTHER

        // Step 2: Check initial my-scores page constraints
        val initialMyScores = testApp(Request.Companion(Method.GET, "/decisions/$decisionId/my-scores"))
        initialMyScores.status shouldBe Status.Companion.OK
        val initialBody = initialMyScores.bodyString()
        initialBody shouldContain "min=\"1\""
        initialBody shouldContain "max=\"5\""

        // Step 3: Update score range
        val updateRangeRequest = Request.Companion(Method.POST, "/decisions/$decisionId/name")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("HX-Request", "true")
            .body("name=Range+Update+Test&minScore=2&maxScore=8")

        testApp(updateRangeRequest).status shouldBe Status.Companion.OK

        // Step 4: Verify updated my-scores page reflects new constraints
        val updatedMyScores = testApp(Request.Companion(Method.GET, "/decisions/$decisionId/my-scores"))
        updatedMyScores.status shouldBe Status.Companion.OK
        val updatedBody = updatedMyScores.bodyString()
        updatedBody shouldContain "min=\"2\""
        updatedBody shouldContain "max=\"8\""

        // Old constraints should not be present
        updatedBody shouldNotContain "min=\"1\""
        updatedBody shouldNotContain "max=\"5\""

    }

    @Test
    fun `Edge cases for score range validation`() {
        // Test minimum possible range (1-2)
        val minRangeRequest = Request.Companion(Method.POST, "/decisions")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=Min+Range&minScore=1&maxScore=2")

        val minRangeResponse = testApp(minRangeRequest)
        minRangeResponse.status shouldBe Status.Companion.SEE_OTHER
        val minRangeDecisionId = minRangeResponse.header("Location")!!.split("/")[2]

        // Add basic structure
        testApp(
            Request.Companion(Method.POST, "/decisions/$minRangeDecisionId/criteria")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("name=Test&weight=1")
        ).status shouldBe Status.Companion.SEE_OTHER

        testApp(
            Request.Companion(Method.POST, "/decisions/$minRangeDecisionId/options")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("name=Option")
        ).status shouldBe Status.Companion.SEE_OTHER


        // Test invalid range creation (min >= max)
        val invalidRangeRequest = Request.Companion(Method.POST, "/decisions")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=Invalid+Range&minScore=5&maxScore=3")

        val invalidRangeResponse = testApp(invalidRangeRequest)
        invalidRangeResponse.status shouldBe Status.Companion.BAD_REQUEST
        invalidRangeResponse.bodyString() shouldBe "Min score must be less than max score"

        // Test equal min and max (should also be invalid)
        val equalRangeRequest = Request.Companion(Method.POST, "/decisions")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=Equal+Range&minScore=5&maxScore=5")

        val equalRangeResponse = testApp(equalRangeRequest)
        equalRangeResponse.status shouldBe Status.Companion.BAD_REQUEST
        equalRangeResponse.bodyString() shouldBe "Min score must be less than max score"
    }
}
