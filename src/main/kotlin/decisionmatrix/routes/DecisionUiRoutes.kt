package decisionmatrix.routes

import decisionmatrix.CriteriaInput
import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import decisionmatrix.OptionCriteriaScoreInput
import decisionmatrix.db.CriteriaRepository
import decisionmatrix.db.DecisionRepository
import decisionmatrix.db.OptionRepository
import decisionmatrix.db.OptionCriteriaScoreRepository
import decisionmatrix.ui.DecisionPages
import decisionmatrix.ui.MyScoresPages
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
    private val decisionRepository: DecisionRepository,
    private val optionRepository: OptionRepository,
    private val criteriaRepository: CriteriaRepository,
    private val optionCriteriaScoreRepository: OptionCriteriaScoreRepository
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
        "/ui/decisions/{id}/criteria/{criteriaId}/delete" bind Method.POST to ::deleteCriteria,

        "/ui/decisions/{id}/my-scores" bind Method.GET to ::viewMyScores,
        "/ui/decisions/{id}/my-scores" bind Method.POST to ::submitMyScores
    )

    private fun home(@Suppress("UNUSED_PARAMETER") request: Request): Response =
        Response(Status.SEE_OTHER).header("Location", "/ui/decisions/new")

    private fun newDecisionForm(@Suppress("UNUSED_PARAMETER") request: Request): Response =
        htmlResponse(DecisionPages.createPage())

    private fun createDecision(request: Request): Response {
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Name is required")
        val created = decisionRepository.insert(DecisionInput(name = name))
        return Response(Status.SEE_OTHER).header("Location", "/ui/decisions/${created.id}/edit")
    }

    private fun editDecision(request: Request): Response {
        val id = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val decision = decisionRepository.findById(id) ?: return Response(Status.NOT_FOUND).body("Decision not found")
        return htmlResponse(DecisionPages.editPage(decision))
    }

    private fun updateDecisionName(request: Request): Response {
        val id = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Name is required")
        val updated = decisionRepository.update(id, name) ?: return Response(Status.NOT_FOUND).body("Decision not found")

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
        optionRepository.insert(decisionId, OptionInput(name))

        return if (isHx(request)) {
            val decision = decisionRepository.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
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
        val updated = optionRepository.update(optionId, name) ?: return Response(Status.NOT_FOUND).body("Option not found")

        return if (isHx(request)) {
            val decision = decisionRepository.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.optionsFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/ui/decisions/$decisionId/edit")
        }
    }

    private fun deleteOption(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val optionId = request.path("optionId")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing optionId")
        optionRepository.delete(optionId)

        return if (isHx(request)) {
            val decision = decisionRepository.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
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
        criteriaRepository.insert(decisionId, CriteriaInput(name = name, weight = weight))

        return if (isHx(request)) {
            val decision = decisionRepository.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
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
        val updated = criteriaRepository.update(criteriaId, name, weight) ?: return Response(Status.NOT_FOUND).body("Criteria not found")

        return if (isHx(request)) {
            val decision = decisionRepository.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.criteriaFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/ui/decisions/$decisionId/edit")
        }
    }

    private fun deleteCriteria(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val criteriaId = request.path("criteriaId")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing criteriaId")
        criteriaRepository.delete(criteriaId)

        return if (isHx(request)) {
            val decision = decisionRepository.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.criteriaFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/ui/decisions/$decisionId/edit")
        }
    }

    private fun viewMyScores(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val userId = request.query("userid")?.trim().orEmpty()
        if (userId.isBlank()) return Response(Status.BAD_REQUEST).body("Missing required query param 'userid'")

        val decision = decisionRepository.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
        val userScores = optionCriteriaScoreRepository.findAllByDecisionId(decisionId)
            .filter { it.scoredBy == userId }

        return htmlResponse(MyScoresPages.myScoresPage(decision, userId, userScores))
    }

    private fun submitMyScores(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val decision = decisionRepository.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")

        val form = parseForm(request)
        val userId = form["userid"]?.trim().takeUnless { it.isNullOrBlank() }
            ?: request.query("userid")?.trim()
            ?: return Response(Status.BAD_REQUEST).body("Missing userid")

        // If a delete submit button was clicked, it will be the only delete_* key present. Handle it explicitly.
        val deleteKey = form.keys.firstOrNull { it.startsWith("delete_") }
        if (deleteKey != null) {
            val parts = deleteKey.split("_")
            if (parts.size == 3) {
                val optId = parts[1].toLongOrNull()
                val critId = parts[2].toLongOrNull()
                if (optId != null && critId != null) {
                    val existingForUser = optionCriteriaScoreRepository.findAllByDecisionId(decisionId)
                        .firstOrNull { it.scoredBy == userId && it.optionId == optId && it.criteriaId == critId }
                    if (existingForUser != null) {
                        optionCriteriaScoreRepository.delete(existingForUser.id)
                    }
                }
            }
            return Response(Status.SEE_OTHER)
                .header("Location", "/ui/decisions/$decisionId/my-scores?userid=$userId")
        }

        // Otherwise, treat as a Save action: insert/update any provided numeric scores. Do NOT delete on blanks.
        val existingForUser = optionCriteriaScoreRepository.findAllByDecisionId(decisionId)
            .filter { it.scoredBy == userId }
        val existingMap = existingForUser.associateBy { it.optionId to it.criteriaId }

        for (opt in decision.options) {
            for (c in decision.criteria) {
                val key = "score_${opt.id}_${c.id}"
                if (!form.containsKey(key)) continue
                val raw = form[key]?.trim()
                if (raw.isNullOrBlank()) continue

                val value = raw.toIntOrNull() ?: continue
                val existing = existingMap[opt.id to c.id]

                if (existing == null) {
                    optionCriteriaScoreRepository.insert(
                        decisionId = decisionId,
                        optionId = opt.id,
                        criteriaId = c.id,
                        scoredBy = userId,
                        score = OptionCriteriaScoreInput(score = value)
                    )
                } else if (existing.score != value) {
                    optionCriteriaScoreRepository.update(existing.id, value)
                }
            }
        }

        return Response(Status.SEE_OTHER)
            .header("Location", "/ui/decisions/$decisionId/my-scores?userid=$userId")
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
