package decisionmatrix.ui

import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import decisionmatrix.auth.withMockAuth
import decisionmatrix.auth.AuthorizationService
import decisionmatrix.db.CriteriaRepositoryImpl
import decisionmatrix.db.DecisionRepositoryImpl
import decisionmatrix.db.OptionRepositoryImpl
import decisionmatrix.db.UserScoreRepositoryImpl
import decisionmatrix.db.TagRepositoryImpl
import decisionmatrix.db.cleanTestDatabase
import decisionmatrix.db.getTestJdbi
import decisionmatrix.routes.DecisionRoutes
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.body.form
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OptionNotesEndToEndTest {

    private val jdbi = getTestJdbi()
    private val decisionRepository = DecisionRepositoryImpl(jdbi)
    private val optionRepository = OptionRepositoryImpl(jdbi)
    private val criteriaRepository = CriteriaRepositoryImpl(jdbi)
    private val userScoreRepository = UserScoreRepositoryImpl(jdbi)
    private val tagRepository = TagRepositoryImpl(jdbi)
    private val authorizationService = AuthorizationService(decisionRepository)

    private val decisionRoutes = DecisionRoutes(
        decisionRepository = decisionRepository,
        optionRepository = optionRepository,
        criteriaRepository = criteriaRepository,
        userScoreRepository = userScoreRepository,
        tagRepository = tagRepository,
        authorizationService = authorizationService
    )

    private val testApp = decisionRoutes.routes.withMockAuth(userId = "test-user", userEmail = "test@example.com")

    @BeforeEach
    fun setUp() {
        cleanTestDatabase()
    }

    @Test fun `create option with notes and view them`() {
        // Create decision and option
        val decision = decisionRepository.insert(DecisionInput(name = "Test Decision"), createdBy = "test-user")
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        // Update option with markdown notes
        val markdown = """
            # Option A Details

            This is a **great** option because:
            - It's fast
            - It's reliable
            - It's cost-effective
        """.trimIndent()

        val updateRequest = Request(Method.POST, "/decisions/${decision.id}/options/${option.id}/update")
            .form("name", "Option A")
            .form("notes", markdown)

        val updateResponse = testApp(updateRequest)
        updateResponse.status shouldBe Status.SEE_OTHER

        // View the notes page
        val viewRequest = Request(Method.GET, "/decisions/${decision.id}/options/${option.id}/notes")
        val viewResponse = testApp(viewRequest)

        viewResponse.status shouldBe Status.OK
        val html = viewResponse.bodyString()

        // Check that markdown is rendered
        html shouldContain "<h1>Option A Details</h1>"
        html shouldContain "<strong>great</strong>"
        html shouldContain "<li>It's fast</li>"
        html shouldContain "<li>It's reliable</li>"
        html shouldContain "<li>It's cost-effective</li>"
    }

    @Test fun `view notes link appears on my-scores page when notes exist`() {
        // Create decision with two options
        val decision = decisionRepository.insert(DecisionInput(name = "Test Decision"), createdBy = "test-user")
        val option1 = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))
        val option2 = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option B"))

        // Add at least one criteria so the scores table appears
        criteriaRepository.insert(decisionId = decision.id, decisionmatrix.CriteriaInput(name = "Test Criteria", weight = 1))

        // Add notes only to option1
        optionRepository.update(id = option1.id, name = "Option A", notes = "Some notes for Option A")

        // View my-scores page
        val request = Request(Method.GET, "/decisions/${decision.id}/my-scores")
        val response = testApp(request)

        response.status shouldBe Status.OK
        val html = response.bodyString()

        // Check that view notes link appears for option1 (modal-based)
        html shouldContain "view notes"
        html shouldContain "data-option-id=\"${option1.id}\""
        html shouldContain "data-decision-id=\"${decision.id}\""
        html shouldContain "data-option-name=\"Option A\""
        html shouldContain "showNotesModal(${option1.id}, ${decision.id}, 'Option A')"

        // Check that view notes link does NOT appear for option2 (no notes)
        html shouldNotContain "data-option-id=\"${option2.id}\""
        html shouldNotContain "showNotesModal(${option2.id}"
    }

    @Test fun `notes textarea appears in edit decision page`() {
        // Create decision and option
        val decision = decisionRepository.insert(DecisionInput(name = "Test Decision"), createdBy = "test-user")
        optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        // View edit page
        val request = Request(Method.GET, "/decisions/${decision.id}/edit")
        val response = testApp(request)

        response.status shouldBe Status.OK
        val html = response.bodyString()

        // Check that notes textarea is present
        html shouldContain "textarea"
        html shouldContain "name=\"notes\""
        html shouldContain "Markdown notes"
    }

    @Test fun `view notes page shows message when no notes exist`() {
        // Create decision and option without notes
        val decision = decisionRepository.insert(DecisionInput(name = "Test Decision"), createdBy = "test-user")
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))

        // View the notes page
        val viewRequest = Request(Method.GET, "/decisions/${decision.id}/options/${option.id}/notes")
        val viewResponse = testApp(viewRequest)

        viewResponse.status shouldBe Status.OK
        val html = viewResponse.bodyString()

        // Check that message appears
        html shouldContain "No notes available for this option"
    }

    @Test fun `clearing notes removes them from database`() {
        // Create decision and option with notes
        val decision = decisionRepository.insert(DecisionInput(name = "Test Decision"), createdBy = "test-user")
        val option = optionRepository.insert(decisionId = decision.id, OptionInput(name = "Option A"))
        optionRepository.update(id = option.id, name = "Option A", notes = "Initial notes")

        // Verify notes exist
        val withNotes = requireNotNull(optionRepository.findById(option.id))
        withNotes.notes shouldBe "Initial notes"

        // Clear notes by sending empty string
        val updateRequest = Request(Method.POST, "/decisions/${decision.id}/options/${option.id}/update")
            .form("name", "Option A")
            .form("notes", "")

        val updateResponse = testApp(updateRequest)
        updateResponse.status shouldBe Status.SEE_OTHER

        // Verify notes are cleared
        val withoutNotes = requireNotNull(optionRepository.findById(option.id))
        withoutNotes.notes shouldBe null
    }
}
