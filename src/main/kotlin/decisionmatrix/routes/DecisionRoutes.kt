package decisionmatrix.routes

import decisionmatrix.DecisionInput
import decisionmatrix.db.DecisionRepository
import decisionmatrix.json
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

class DecisionRoutes(private val decisionRepository: DecisionRepository) {

    val routes: RoutingHttpHandler = routes(
        "/decisions" bind Method.POST to ::createDecision,
        "/decisions/{id}" bind Method.GET to ::getDecision
    )

    fun createDecision(request: Request): Response {
        return try {
            val decision = json.decodeFromString<DecisionInput>(request.bodyString())
            val createdDecision = decisionRepository.insert(decision)
            val responseBody = json.encodeToString(createdDecision)
            Response(Status.CREATED).body(responseBody)
                .header("Content-Type", "application/json")
        } catch (e: Exception) {
            Response(Status.BAD_REQUEST).body("Invalid request: ${e.message}")
        }
    }

    fun getDecision(request: Request): Response {
        return try {
            val id = request.path("id")?.toLong()
                ?: return Response(Status.BAD_REQUEST).body("Missing ID")

            val decision = decisionRepository.findById(id)
            if (decision != null) {
                val responseBody = json.encodeToString(decision)
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


