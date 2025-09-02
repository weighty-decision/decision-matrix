package decisionmatrix.routes

import decisionmatrix.CriteriaInput
import decisionmatrix.DEFAULT_MAX_SCORE
import decisionmatrix.DEFAULT_MIN_SCORE
import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import decisionmatrix.UserScoreInput
import decisionmatrix.db.CriteriaRepository
import decisionmatrix.db.DecisionRepository
import decisionmatrix.db.OptionRepository
import decisionmatrix.db.UserScoreRepository
import decisionmatrix.ui.DecisionPages
import decisionmatrix.ui.MyScoresPages
import decisionmatrix.ui.CalculateScoresPages
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
    private val userScoreRepository: UserScoreRepository
) {

    val routes: RoutingHttpHandler = routes(
        "/" bind Method.GET to ::home,
        "/decisions/new" bind Method.GET to ::newDecisionForm,
        "/decisions" bind Method.POST to ::createDecision,
        "/decisions/{id}/edit" bind Method.GET to ::editDecision,

        // htmx-backed POST endpoints (using POST for simplicity)
        "/decisions/{id}/name" bind Method.POST to ::updateDecisionName,

        "/decisions/{id}/options" bind Method.POST to ::createOption,
        "/decisions/{id}/options/{optionId}/update" bind Method.POST to ::updateOption,
        "/decisions/{id}/options/{optionId}/delete" bind Method.POST to ::deleteOption,

        "/decisions/{id}/criteria" bind Method.POST to ::createCriteria,
        "/decisions/{id}/criteria/{criteriaId}/update" bind Method.POST to ::updateCriteria,
        "/decisions/{id}/criteria/{criteriaId}/delete" bind Method.POST to ::deleteCriteria,

        "/decisions/{id}/my-scores" bind Method.GET to ::viewMyScores,
        "/decisions/{id}/my-scores" bind Method.POST to ::submitMyScores,
        "/decisions/{id}/calculate-scores" bind Method.GET to ::calculateScores
    )

    private fun home(@Suppress("UNUSED_PARAMETER") request: Request): Response =
        Response(Status.SEE_OTHER).header("Location", "/decisions/new")

    private fun newDecisionForm(@Suppress("UNUSED_PARAMETER") request: Request): Response =
        htmlResponse(DecisionPages.createPage())

    private fun createDecision(request: Request): Response {
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Name is required")
        
        val minScore = form["minScore"]?.toIntOrNull() ?: DEFAULT_MIN_SCORE
        val maxScore = form["maxScore"]?.toIntOrNull() ?: DEFAULT_MAX_SCORE
        
        if (minScore >= maxScore) {
            return Response(Status.BAD_REQUEST).body("Min score must be less than max score")
        }
        
        val created = decisionRepository.insert(DecisionInput(name = name, minScore = minScore, maxScore = maxScore))
        return Response(Status.SEE_OTHER).header("Location", "/decisions/${created.id}/edit")
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
        
        val minScore = form["minScore"]?.toIntOrNull() ?: DEFAULT_MIN_SCORE
        val maxScore = form["maxScore"]?.toIntOrNull() ?: DEFAULT_MAX_SCORE
        
        if (minScore >= maxScore) {
            return Response(Status.BAD_REQUEST).body("Min score must be less than max score")
        }
        
        val updated = decisionRepository.update(id, name, minScore, maxScore) ?: return Response(Status.NOT_FOUND).body("Decision not found")

        return if (isHx(request)) {
            htmlResponse(DecisionPages.nameFragment(updated))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/decisions/${updated.id}/edit")
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
            Response(Status.SEE_OTHER).header("Location", "/decisions/$decisionId/edit")
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
            Response(Status.SEE_OTHER).header("Location", "/decisions/$decisionId/edit")
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
            Response(Status.SEE_OTHER).header("Location", "/decisions/$decisionId/edit")
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
            Response(Status.SEE_OTHER).header("Location", "/decisions/$decisionId/edit")
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
            Response(Status.SEE_OTHER).header("Location", "/decisions/$decisionId/edit")
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
            Response(Status.SEE_OTHER).header("Location", "/decisions/$decisionId/edit")
        }
    }

    private fun viewMyScores(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val userId = request.query("userid")?.trim().orEmpty()
        if (userId.isBlank()) return Response(Status.BAD_REQUEST).body("Missing required query param 'userid'")

        val decision = decisionRepository.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
        val userScores = userScoreRepository.findAllByDecisionId(decisionId)
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

        // Save action: insert/update any provided numeric scores; delete existing scores if a blank was submitted.
        val existingForUser = userScoreRepository.findAllByDecisionId(decisionId)
            .filter { it.scoredBy == userId }
        val existingMap = existingForUser.associateBy { it.optionId to it.criteriaId }

        for (opt in decision.options) {
            for (c in decision.criteria) {
                val key = "score_${opt.id}_${c.id}"
                if (!form.containsKey(key)) continue
                val raw = form[key]?.trim()

                val existing = existingMap[opt.id to c.id]
                if (raw.isNullOrBlank()) {
                    if (existing != null) {
                        userScoreRepository.delete(existing.id)
                    }
                    continue
                }

                val value = raw.toIntOrNull() ?: continue
                
                if (value < decision.minScore || value > decision.maxScore) {
                    return Response(Status.BAD_REQUEST).body("Score $value is outside the allowed range of ${decision.minScore}-${decision.maxScore}")
                }
                
                if (existing == null) {
                    userScoreRepository.insert(
                        decisionId = decisionId,
                        optionId = opt.id,
                        criteriaId = c.id,
                        scoredBy = userId,
                        score = UserScoreInput(score = value)
                    )
                } else if (existing.score != value) {
                    userScoreRepository.update(existing.id, value)
                }
            }
        }

        return Response(Status.SEE_OTHER)
            .header("Location", "/decisions/$decisionId/my-scores?userid=$userId")
    }

    private fun calculateScores(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val decision = decisionRepository.findById(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")

        val scores = userScoreRepository.findAllByDecisionId(decisionId)

        return htmlResponse(CalculateScoresPages.calculateScoresPage(decision, scores))
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
