package decisionmatrix.routes

import decisionmatrix.Decision
import decisionmatrix.db.DecisionRepository
import kotlinx.serialization.json.Json
import org.http4k.core.*
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

class DecisionRoutes(private val decisionRepository: DecisionRepository) {

    private val json = Json { ignoreUnknownKeys = true }

    val routes: RoutingHttpHandler = routes(
        "/decisions" bind Method.POST to ::createDecision,
        "/decisions/{id}" bind Method.GET to ::getDecision
    )

    private fun createDecision(request: Request): Response {
        return try {
            val decision = json.decodeFromString<Decision>(request.bodyString())
            val createdDecision = decisionRepository.insert(decision)
            val responseBody = json.encodeToString(Decision.serializer(), createdDecision)
            Response(Status.CREATED).body(responseBody)
                .header("Content-Type", "application/json")
        } catch (e: Exception) {
            Response(Status.BAD_REQUEST).body("Invalid request: ${e.message}")
        }
    }

    private fun getDecision(request: Request): Response {
        return try {
            val id = request.path("id")?.toLong() 
                ?: return Response(Status.BAD_REQUEST).body("Invalid ID")

            val decision = decisionRepository.findById(id)
            if (decision != null) {
                val responseBody = json.encodeToString(Decision.serializer(), decision)
                Response(Status.OK).body(responseBody)
                    .header("Content-Type", "application/json")
            } else {
                Response(Status.NOT_FOUND).body("Decision not found")
            }
        } catch (e: Exception) {
            Response(Status.BAD_REQUEST).body("Invalid request: ${e.message}")
        }
    }
}
