package decisionmatrix.routes

import decisionmatrix.Option
import decisionmatrix.json
import decisionmatrix.db.OptionRepository
import kotlinx.serialization.Serializable
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

class OptionRoutes(private val optionRepository: OptionRepository) {

    val routes: RoutingHttpHandler = routes(
        "/decisions/{decisionId}/options/" bind Method.POST to ::createOption,
        "/decisions/{decisionId}/options/{optionId}" bind Method.PUT to ::updateOption,
        "/decisions/{decisionId}/options/{optionId}" bind Method.DELETE to ::deleteOption
    )

    fun createOption(request: Request): Response {
        return try {
            val decisionId = request.path("decisionId")?.toLong()
                ?: return Response(Status.BAD_REQUEST).body("Missing decisionId")
            val body = json.decodeFromString<CreateOptionRequest>(request.bodyString())
            val created = optionRepository.insert(
                Option(decisionId = decisionId, name = body.name)
            )
            val responseBody = json.encodeToString(created)
            Response(Status.CREATED).body(responseBody)
                .header("Content-Type", "application/json")
        } catch (e: Exception) {
            Response(Status.BAD_REQUEST).body("Invalid request: ${e.message}")
        }
    }

    fun updateOption(request: Request): Response {
        return try {
            val decisionId = request.path("decisionId")?.toLong()
                ?: return Response(Status.BAD_REQUEST).body("Missing decisionId")
            val optionId = request.path("optionId")?.toLong()
                ?: return Response(Status.BAD_REQUEST).body("Missing optionId")

            val body = json.decodeFromString<UpdateOptionRequest>(request.bodyString())
            val updated = optionRepository.update(optionId, decisionId, body.name)
            if (updated != null) {
                val responseBody = json.encodeToString(updated)
                Response(Status.OK).body(responseBody)
                    .header("Content-Type", "application/json")
            } else {
                Response(Status.NOT_FOUND).body("Option not found")
            }
        } catch (e: Exception) {
            Response(Status.BAD_REQUEST).body("Invalid request: ${e.message}")
        }
    }

    fun deleteOption(request: Request): Response {
        return try {
            val decisionId = request.path("decisionId")?.toLong()
                ?: return Response(Status.BAD_REQUEST).body("Missing decisionId")
            val optionId = request.path("optionId")?.toLong()
                ?: return Response(Status.BAD_REQUEST).body("Missing optionId")

            val deleted = optionRepository.delete(optionId, decisionId)
            if (deleted) {
                Response(Status.NO_CONTENT)
            } else {
                Response(Status.NOT_FOUND).body("Option not found")
            }
        } catch (e: Exception) {
            Response(Status.BAD_REQUEST).body("Invalid request: ${e.message}")
        }
    }
}

@Serializable
data class CreateOptionRequest(val name: String)

@Serializable
data class UpdateOptionRequest(val name: String)
