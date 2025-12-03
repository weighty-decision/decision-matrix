package decisionmatrix.routes

import decisionmatrix.Tag
import decisionmatrix.db.TagRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

@Serializable
data class TagAutocompleteResponse(
    val tags: List<Tag>
)

class TagRoutes(
    private val tagRepository: TagRepository
) {

    val routes: RoutingHttpHandler = routes(
        "/api/tags/autocomplete" bind Method.GET to ::autocomplete
    )

    private fun autocomplete(request: Request): Response {
        val prefix = request.query("q") ?: ""

        if (prefix.isBlank()) {
            return Response(Status.OK)
                .header("Content-Type", "application/json")
                .body(Json.encodeToString(TagAutocompleteResponse(tags = emptyList())))
        }

        val tags = tagRepository.findByPrefix(prefix = prefix, limit = 10)

        return Response(Status.OK)
            .header("Content-Type", "application/json")
            .body(Json.encodeToString(TagAutocompleteResponse(tags = tags)))
    }
}
