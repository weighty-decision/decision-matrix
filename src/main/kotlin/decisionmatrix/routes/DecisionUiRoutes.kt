package decisionmatrix.routes

import decisionmatrix.CriteriaInput
import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import decisionmatrix.db.CriteriaRepository
import decisionmatrix.db.DecisionRepository
import decisionmatrix.db.OptionRepository
import decisionmatrix.ui.DecisionPages
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class DecisionUiRoutes(
    private val decisions: DecisionRepository,
    private val options: OptionRepository,
    private val criteria: CriteriaRepository
) {

    val routes: RoutingHttpHandler = routes(
        "/" bind Method.GET to ::home,
        "/ui/decisions/new" bind Method.GET to ::newDecisionForm,
        "/ui/decisions" bind Method.POST to ::createDecision,
        "/ui/decisions/{id}/edit" bind Method.GET to ::editDecision,

        // htmx-backed POST endpoints (using POST for simplicity)
        "/ui/decisions/{id}/name" bind Method.POST to ::updateDecisionName,

        "/ui/decisions/{id}/options" bind Method.POST to ::createOption,
        "/ui/decisions/{id}/options/{optionId}/update" bind Method.POST to ::updateOption,
        "/ui/decisions/{id}/options/{optionId}/delete" bind Method.POST to ::deleteOption,

        "/ui/decisions/{id}/criteria" bind Method.POST to ::createCriteria,
        "/ui/decisions/{id}/criteria/{criteriaId}/update" bind Method.POST to ::updateCriteria,
        "/ui/decisions/{id}/criteria/{criteriaId}/delete" bind Method.POST to ::deleteCriteria
    )

    private fun home(@Suppress("UNUSED_PARAMETER") request: Request): Response =
        Response(Status.SEE_OTHER).header("Location", "/ui/decisions/new")

    private fun newDecisionForm(@Suppress("UNUSED_PARAMETER") request: Request): Response =
        htmlResponse(DecisionPages.createPage())

    private fun createDecision(request: Request): Response {
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Name is required")
        val created = decisions.insert(DecisionInput(name = name))
        return Response(Status.SEE_OTHER).header("Location", "/ui/decisions/${created.id}/edit")
    }

    private fun editDecision(request: Request): Response {
        val id = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val decision = decisions.findById(id) ?: return Response(Status.NOT_FOUND).body("Decision not found")
        return htmlResponse(DecisionPages.editPage(decision))
    }

    private fun updateDecisionName(request: Request): Response {
        val id = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Name is required")
        val updated = decisions.update(id, name) ?: return Response(Status.NOT_FOUND).body("Decision not found")

        return if (isHx(request)) {
            htmlResponse(DecisionPages.nameFragment(updated))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/ui/decisions/${updated.id}/edit")
        }
    }

    private fun createOption(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Option name is required")
        options.insert(decisionId, OptionInput(name))

        return if (isHx(request)) {
            val decision = decisions.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.optionsFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/ui/decisions/$decisionId/edit")
        }
    }

    private fun updateOption(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val optionId = request.path("optionId")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing optionId")
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Option name is required")
        val updated = options.update(optionId, name) ?: return Response(Status.NOT_FOUND).body("Option not found")

        return if (isHx(request)) {
            val decision = decisions.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.optionsFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/ui/decisions/$decisionId/edit")
        }
    }

    private fun deleteOption(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val optionId = request.path("optionId")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing optionId")
        options.delete(optionId)

        return if (isHx(request)) {
            val decision = decisions.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.optionsFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/ui/decisions/$decisionId/edit")
        }
    }

    private fun createCriteria(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        val weight = form["weight"]?.toIntOrNull() ?: 1
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Criteria name is required")
        criteria.insert(decisionId, CriteriaInput(name = name, weight = weight))

        return if (isHx(request)) {
            val decision = decisions.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.criteriaFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/ui/decisions/$decisionId/edit")
        }
    }

    private fun updateCriteria(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val criteriaId = request.path("criteriaId")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing criteriaId")
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        val weight = form["weight"]?.toIntOrNull() ?: 1
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Criteria name is required")
        val updated = criteria.update(criteriaId, name, weight) ?: return Response(Status.NOT_FOUND).body("Criteria not found")

        return if (isHx(request)) {
            val decision = decisions.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.criteriaFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/ui/decisions/$decisionId/edit")
        }
    }

    private fun deleteCriteria(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val criteriaId = request.path("criteriaId")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing criteriaId")
        criteria.delete(criteriaId)

        return if (isHx(request)) {
            val decision = decisions.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.criteriaFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/ui/decisions/$decisionId/edit")
        }
    }

    // ---- helpers
    private fun htmlResponse(html: String): Response =
        Response(Status.OK).header("Content-Type", "text/html; charset=utf-8").body(html)

    private fun isHx(request: Request): Boolean =
        request.header("HX-Request")?.equals("true", ignoreCase = true) == true

    private fun parseForm(request: Request): Map<String, String> {
        val raw = request.bodyString()
        if (raw.isBlank()) return emptyMap()
        return raw.split("&").mapNotNull { pair ->
            val idx = pair.indexOf("=")
            if (idx < 0) return@mapNotNull null
            val k = pair.substring(0, idx)
            val v = pair.substring(idx + 1)
            urlDecode(k) to urlDecode(v)
        }.toMap()
    }

    private fun urlDecode(s: String) = URLDecoder.decode(s, StandardCharsets.UTF_8)
}
