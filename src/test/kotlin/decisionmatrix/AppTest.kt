package decisionmatrix

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.junit.jupiter.api.Test

class AppTest {

    @Test fun `Ping test`() {
        app(Request(GET, "/ping")) shouldBe Response(OK).body("pong")
    }

    @Test fun `Create decision with custom score range`() {
        // Test decision creation with custom score range
        val createDecisionRequest = Request(POST, "/decisions")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=Test+Decision&minScore=0&maxScore=5")

        val createResponse = app(createDecisionRequest)
        createResponse.status shouldBe SEE_OTHER

        val decisionId = createResponse.header("Location")!!.split("/")[2]

        // Verify the edit page shows the correct score range
        val editPageRequest = Request(GET, "/decisions/$decisionId/edit")
        val editPageResponse = app(editPageRequest)

        editPageResponse.status shouldBe OK
        val editPageBody = editPageResponse.bodyString()
        editPageBody shouldContain "value=\"0\""  // minScore
        editPageBody shouldContain "value=\"5\""  // maxScore
    }

    @Test fun `Update decision score range through UI`() {
        // Step 1: Create decision with default score range
        val createDecisionRequest = Request(POST, "/decisions")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("name=Default+Range+Decision")

        val createResponse = app(createDecisionRequest)
        createResponse.status shouldBe SEE_OTHER

        val decisionId = createResponse.header("Location")!!.split("/")[2]

        // Step 2: Update decision with new score range
        val updateRequest = Request(POST, "/decisions/$decisionId/name")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("HX-Request", "true")
            .body("name=Updated+Decision&minScore=2&maxScore=8")

        val updateResponse = app(updateRequest)
        updateResponse.status shouldBe OK

        // Step 3: Verify update was applied by checking edit page
        val editPageRequest = Request(GET, "/decisions/$decisionId/edit")
        val editPageResponse = app(editPageRequest)

        editPageResponse.status shouldBe OK
        val editPageBody = editPageResponse.bodyString()
        editPageBody shouldContain "value=\"Updated Decision\""
        editPageBody shouldContain "value=\"2\""  // Updated minScore
        editPageBody shouldContain "value=\"8\""  // Updated maxScore
    }

}
