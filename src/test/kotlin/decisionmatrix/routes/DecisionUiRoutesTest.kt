package decisionmatrix.routes

import decisionmatrix.DecisionInput
import decisionmatrix.auth.withMockAuth
import decisionmatrix.db.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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

    private val routes = DecisionRoutes(
        decisionRepository, optionRepository, criteriaRepository, userScoreRepository
    ).routes.withMockAuth()

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
            DecisionInput(name = "Test Decision", minScore = 1, maxScore = 5),
            createdBy = "test-user"
        )
        val option = optionRepository.insert(decision.id, decisionmatrix.OptionInput("Option A"))
        val criteria = criteriaRepository.insert(decision.id, decisionmatrix.CriteriaInput("Criteria A", 1))

        // Test valid score
        val validRequest = Request(Method.POST, "/decisions/${decision.id}/my-scores")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("score_${option.id}_${criteria.id}=3")

        val validResponse = routes(validRequest)
        validResponse.status shouldBe Status.SEE_OTHER

        // Test invalid score (too high)
        val invalidHighRequest = Request(Method.POST, "/decisions/${decision.id}/my-scores")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("score_${option.id}_${criteria.id}=7")

        val invalidHighResponse = routes(invalidHighRequest)
        invalidHighResponse.status shouldBe Status.BAD_REQUEST
        invalidHighResponse.bodyString() shouldBe "Score 7 is outside the allowed range of 1-5"

        // Test invalid score (too low)
        val invalidLowRequest = Request(Method.POST, "/decisions/${decision.id}/my-scores")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("score_${option.id}_${criteria.id}=0")

        val invalidLowResponse = routes(invalidLowRequest)
        invalidLowResponse.status shouldBe Status.BAD_REQUEST
        invalidLowResponse.bodyString() shouldBe "Score 0 is outside the allowed range of 1-5"
    }

    @Test fun `edit page includes focus behavior for new criteria and options inputs`() {
        val decision = decisionRepository.insert(
            DecisionInput(name = "Test Decision", minScore = 1, maxScore = 10),
            createdBy = "test-user"
        )

        val request = Request(Method.GET, "/decisions/${decision.id}/edit")
        val response = routes(request)

        response.status shouldBe Status.OK
        val htmlContent = response.bodyString()
        
        // Verify the new criteria input has focus behavior
        htmlContent shouldContain "id=\"new-criteria-input\""
        htmlContent shouldContain "htmx:afterSwap"
        htmlContent shouldContain "new-criteria-input"
        
        // Verify the new option input has focus behavior
        htmlContent shouldContain "id=\"new-option-input\""
        htmlContent shouldContain "new-option-input"
    }

    @Test fun `updateDecisionName with score range updates all fields`() {
        val decision = decisionRepository.insert(
            DecisionInput(name = "Original Decision", minScore = 1, maxScore = 10),
            createdBy = "test-user"
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
            DecisionInput(name = "Original Decision", minScore = 1, maxScore = 10),
            createdBy = "test-user"
        )

        val request = Request(Method.POST, "/decisions/${decision.id}/name")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("HX-Request", "true")
            .body("name=Updated+Decision&minScore=8&maxScore=2")

        val response = routes(request)

        response.status shouldBe Status.BAD_REQUEST
        response.bodyString() shouldBe "Min score must be less than max score"
    }

    @Test fun `home page shows decisions user is involved with sorted by created_at desc`() {
        // Create multiple decisions with different users and timestamps
        val decision1 = decisionRepository.insert(
            DecisionInput(name = "First Decision"),
            createdBy = "test-user"
        )
        val decision2 = decisionRepository.insert(
            DecisionInput(name = "Second Decision"),
            createdBy = "other-user"
        )
        val decision3 = decisionRepository.insert(
            DecisionInput(name = "Third Decision"),
            createdBy = "test-user"
        )

        // Add scores for test-user to decision2 so they're involved
        val option = optionRepository.insert(decision2.id, decisionmatrix.OptionInput("Option A"))
        val criteria = criteriaRepository.insert(decision2.id, decisionmatrix.CriteriaInput("Criteria A", 1))
        userScoreRepository.insert(decision2.id, option.id, criteria.id, "test-user", decisionmatrix.UserScoreInput(5))

        val request = Request(Method.GET, "/")
        val response = routes(request)

        response.status shouldBe Status.OK
        val htmlContent = response.bodyString()
        
        // Should show decisions user created or participated in
        htmlContent shouldContain "First Decision"
        htmlContent shouldContain "Second Decision"
        htmlContent shouldContain "Third Decision"
        htmlContent shouldContain "Create New Decision"
    }

    @Test fun `home page shows decisions user is involved in without role column`() {
        val decision1 = decisionRepository.insert(
            DecisionInput(name = "Created by me"),
            createdBy = "test-user"
        )
        val decision2 = decisionRepository.insert(
            DecisionInput(name = "Participated in"),
            createdBy = "other-user"
        )

        // Add scores for test-user to decision2 so they're a participant
        val option = optionRepository.insert(decision2.id, decisionmatrix.OptionInput("Option A"))
        val criteria = criteriaRepository.insert(decision2.id, decisionmatrix.CriteriaInput("Criteria A", 1))
        userScoreRepository.insert(decision2.id, option.id, criteria.id, "test-user", decisionmatrix.UserScoreInput(5))

        val request = Request(Method.GET, "/")
        val response = routes(request)

        response.status shouldBe Status.OK
        val htmlContent = response.bodyString()
        
        // Should show both decisions user is involved in
        htmlContent shouldContain "Created by me"
        htmlContent shouldContain "Participated in"
    }

    @Test fun `home page shows edit link only for decisions created by current user`() {
        val decision1 = decisionRepository.insert(
            DecisionInput(name = "Created by me"),
            createdBy = "test-user"
        )
        val decision2 = decisionRepository.insert(
            DecisionInput(name = "Created by other"),
            createdBy = "other-user"
        )

        // Add scores for test-user to decision2 so they're involved
        val option = optionRepository.insert(decision2.id, decisionmatrix.OptionInput("Option A"))
        val criteria = criteriaRepository.insert(decision2.id, decisionmatrix.CriteriaInput("Criteria A", 1))
        userScoreRepository.insert(decision2.id, option.id, criteria.id, "test-user", decisionmatrix.UserScoreInput(5))

        val request = Request(Method.GET, "/")
        val response = routes(request)

        response.status shouldBe Status.OK
        val htmlContent = response.bodyString()
        
        // Should show edit link only for decision created by test-user
        htmlContent shouldContain "/decisions/${decision1.id}/edit"
        htmlContent shouldContain "Edit"
        // Should not show edit link for decision created by other-user
        // Count occurrences to verify only one edit link
        val editLinkCount = Regex("/decisions/\\d+/edit").findAll(htmlContent).count()
        editLinkCount shouldBe 1
    }

    @Test fun `home page shows empty state when no decisions`() {
        val request = Request(Method.GET, "/")
        val response = routes(request)

        response.status shouldBe Status.OK
        val htmlContent = response.bodyString()
        
        htmlContent shouldContain "No decisions yet"
        htmlContent shouldContain "Create your first decision"
        htmlContent shouldContain "Create New Decision"
    }

    @Test fun `home page defaults to involved view mode`() {
        val request = Request(Method.GET, "/")
        val response = routes(request)

        response.status shouldBe Status.OK
        val htmlContent = response.bodyString()
        
        htmlContent shouldContain "view-mode-select"
        htmlContent shouldContain "Decisions you're involved in"
        htmlContent shouldContain "Recent decisions"
        htmlContent shouldContain "selected=\"selected\">Decisions you're involved in"
    }

    @Test fun `home page with recent view mode parameter shows recent decisions`() {
        val decision = decisionRepository.insert(
            DecisionInput(name = "Recent Decision"),
            createdBy = "other-user"
        )

        val request = Request(Method.GET, "/?view=recent")
        val response = routes(request)

        response.status shouldBe Status.OK
        val htmlContent = response.bodyString()
        
        htmlContent shouldContain "Recent Decision"
        htmlContent shouldContain "selected=\"selected\">Recent decisions"
    }

    @Test fun `home page with invalid view mode parameter defaults to involved`() {
        val request = Request(Method.GET, "/?view=invalid")
        val response = routes(request)

        response.status shouldBe Status.OK
        val htmlContent = response.bodyString()
        
        htmlContent shouldContain "selected=\"selected\">Decisions you're involved in"
    }

    @Test fun `home page does not show role column anymore`() {
        val decision = decisionRepository.insert(
            DecisionInput(name = "Test Decision"),
            createdBy = "test-user"
        )

        val request = Request(Method.GET, "/")
        val response = routes(request)

        response.status shouldBe Status.OK
        val htmlContent = response.bodyString()
        
        // Should not contain the Role column header
        htmlContent.contains("<th>Role</th>") shouldBe false
        htmlContent.contains("Creator") shouldBe false
        htmlContent.contains("Participant") shouldBe false
    }

    @Test fun `home page shows Score instead of My Scores in action links`() {
        val decision = decisionRepository.insert(
            DecisionInput(name = "Test Decision"),
            createdBy = "test-user"
        )

        val request = Request(Method.GET, "/")
        val response = routes(request)

        response.status shouldBe Status.OK
        val htmlContent = response.bodyString()
        
        htmlContent shouldContain ">Score<"
        htmlContent.contains(">My Scores<") shouldBe false
    }

    @Test fun `recent view shows empty state when no recent decisions`() {
        val request = Request(Method.GET, "/?view=recent")
        val response = routes(request)

        response.status shouldBe Status.OK
        val htmlContent = response.bodyString()
        
        htmlContent shouldContain "No recent decisions found"
        htmlContent shouldContain "selected=\"selected\">Recent decisions"
    }
}
