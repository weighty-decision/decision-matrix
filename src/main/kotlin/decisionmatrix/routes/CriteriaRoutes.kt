package decisionmatrix.routes

import decisionmatrix.CriteriaInput
import decisionmatrix.db.CriteriaRepository
import decisionmatrix.json
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

class CriteriaRoutes(private val criteriaRepository: CriteriaRepository) {

    val routes: RoutingHttpHandler = routes(
        "/decisions/{decisionId}/criteria/" bind Method.POST to ::createCriteria,
        "/decisions/{decisionId}/criteria/{criteriaId}" bind Method.PUT to ::updateCriteria,
        "/decisions/{decisionId}/criteria/{criteriaId}" bind Method.DELETE to ::deleteCriteria
    )

    fun createCriteria(request: Request): Response {
        return try {
            val decisionId = request.path("decisionId")?.toLong()
                ?: return Response(Status.BAD_REQUEST).body("Missing decisionId")
            val body = json.decodeFromString<CriteriaInput>(request.bodyString())
            val created = criteriaRepository.insert(
                CriteriaInput(decisionId = decisionId, name = body.name, weight = body.weight)
            )
            val responseBody = json.encodeToString(created)
            Response(Status.CREATED).body(responseBody)
                .header("Content-Type", "application/json")
        } catch (e: Exception) {
            Response(Status.BAD_REQUEST).body("Invalid request: ${e.message}")
        }
    }

    fun updateCriteria(request: Request): Response {
        return try {
            val decisionId = request.path("decisionId")?.toLong()
                ?: return Response(Status.BAD_REQUEST).body("Missing decisionId")
            val criteriaId = request.path("criteriaId")?.toLong()
                ?: return Response(Status.BAD_REQUEST).body("Missing criteriaId")

            val body = json.decodeFromString<CriteriaInput>(request.bodyString())
            val updated = criteriaRepository.update(criteriaId, decisionId, body.name, body.weight)
            if (updated != null) {
                val responseBody = json.encodeToString(updated)
                Response(Status.OK).body(responseBody)
                    .header("Content-Type", "application/json")
            } else {
                Response(Status.NOT_FOUND).body("Criteria not found")
            }
        } catch (e: Exception) {
            Response(Status.BAD_REQUEST).body("Invalid request: ${e.message}")
        }
    }

    fun deleteCriteria(request: Request): Response {
        return try {
            val decisionId = request.path("decisionId")?.toLong()
                ?: return Response(Status.BAD_REQUEST).body("Missing decisionId")
            val criteriaId = request.path("criteriaId")?.toLong()
                ?: return Response(Status.BAD_REQUEST).body("Missing criteriaId")

            val deleted = criteriaRepository.delete(criteriaId, decisionId)
            if (deleted) {
                Response(Status.NO_CONTENT)
            } else {
                Response(Status.NOT_FOUND).body("Criteria not found")
            }
        } catch (e: Exception) {
            Response(Status.BAD_REQUEST).body("Invalid request: ${e.message}")
        }
    }
}

