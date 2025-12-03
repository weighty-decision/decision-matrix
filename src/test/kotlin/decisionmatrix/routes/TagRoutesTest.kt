package decisionmatrix.routes

import decisionmatrix.db.TagRepositoryImpl
import decisionmatrix.db.cleanTestDatabase
import decisionmatrix.db.getTestJdbi
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TagRoutesTest {

    val jdbi = getTestJdbi()
    val tagRepository = TagRepositoryImpl(jdbi)
    val tagRoutes = TagRoutes(tagRepository)

    @BeforeEach
    fun setup() {
        cleanTestDatabase()
    }

    @Test fun `autocomplete returns matching tags`() {
        tagRepository.findOrCreate("vacations")
        tagRepository.findOrCreate("vacation-planning")
        tagRepository.findOrCreate("frontend-working-group")

        val request = Request(Method.GET, "/api/tags/autocomplete?q=vaca")
        val response = tagRoutes.routes(request)

        response.status shouldBe Status.OK
        response.header("Content-Type") shouldBe "application/json"

        val responseBody = Json.decodeFromString<TagAutocompleteResponse>(response.bodyString())
        responseBody.tags.size shouldBe 2
        responseBody.tags.map { it.name }.toSet() shouldBe setOf("vacations", "vacation-planning")
    }

    @Test fun `autocomplete returns empty list when no matches`() {
        tagRepository.findOrCreate("vacations")

        val request = Request(Method.GET, "/api/tags/autocomplete?q=frontend")
        val response = tagRoutes.routes(request)

        response.status shouldBe Status.OK

        val responseBody = Json.decodeFromString<TagAutocompleteResponse>(response.bodyString())
        responseBody.tags.size shouldBe 0
    }

    @Test fun `autocomplete returns empty list when query is blank`() {
        tagRepository.findOrCreate("vacations")

        val request = Request(Method.GET, "/api/tags/autocomplete?q=")
        val response = tagRoutes.routes(request)

        response.status shouldBe Status.OK

        val responseBody = Json.decodeFromString<TagAutocompleteResponse>(response.bodyString())
        responseBody.tags.size shouldBe 0
    }

    @Test fun `autocomplete returns empty list when query parameter is missing`() {
        tagRepository.findOrCreate("vacations")

        val request = Request(Method.GET, "/api/tags/autocomplete")
        val response = tagRoutes.routes(request)

        response.status shouldBe Status.OK

        val responseBody = Json.decodeFromString<TagAutocompleteResponse>(response.bodyString())
        responseBody.tags.size shouldBe 0
    }

    @Test fun `autocomplete limits results to 10`() {
        // Create 15 tags
        repeat(15) { i ->
            tagRepository.findOrCreate("tag-$i")
        }

        val request = Request(Method.GET, "/api/tags/autocomplete?q=tag")
        val response = tagRoutes.routes(request)

        response.status shouldBe Status.OK

        val responseBody = Json.decodeFromString<TagAutocompleteResponse>(response.bodyString())
        responseBody.tags.size shouldBe 10
    }
}
