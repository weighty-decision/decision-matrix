package decisionmatrix.routes

import decisionmatrix.CriteriaInput
import decisionmatrix.DEFAULT_MAX_SCORE
import decisionmatrix.DEFAULT_MIN_SCORE
import decisionmatrix.DecisionInput
import decisionmatrix.OptionInput
import decisionmatrix.UserScoreInput
import decisionmatrix.auth.UserContext
import decisionmatrix.db.CriteriaRepository
import decisionmatrix.db.DecisionRepository
import decisionmatrix.db.DecisionSearchFilters
import decisionmatrix.db.OptionRepository
import decisionmatrix.db.UserScoreRepository
import decisionmatrix.ui.DecisionPages
import decisionmatrix.ui.IndexPage
import decisionmatrix.ui.MyScoresPages
import decisionmatrix.ui.ResultsPage
import decisionmatrix.auth.AuthorizationService
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

class DecisionRoutes(
    private val decisionRepository: DecisionRepository,
    private val optionRepository: OptionRepository,
    private val criteriaRepository: CriteriaRepository,
    private val userScoreRepository: UserScoreRepository,
    private val authorizationService: AuthorizationService
) {

    val routes: RoutingHttpHandler = routes(
        "/" bind Method.GET to ::home,
        "/search" bind Method.GET to ::searchDecisions,
        "/decisions/new" bind Method.GET to ::newDecisionForm,
        "/decisions" bind Method.POST to ::createDecision,
        "/decisions/{id}/edit" bind Method.GET to ::editDecision,

        "/decisions/{id}/name" bind Method.POST to ::updateDecisionName,
        "/decisions/{id}/delete" bind Method.POST to ::deleteDecision,

        "/decisions/{id}/options" bind Method.POST to ::createOption,
        "/decisions/{id}/options/{optionId}/update" bind Method.POST to ::updateOption,
        "/decisions/{id}/options/{optionId}/delete" bind Method.POST to ::deleteOption,

        "/decisions/{id}/criteria" bind Method.POST to ::createCriteria,
        "/decisions/{id}/criteria/{criteriaId}/update" bind Method.POST to ::updateCriteria,
        "/decisions/{id}/criteria/{criteriaId}/delete" bind Method.POST to ::deleteCriteria,

        "/decisions/{id}/my-scores" bind Method.GET to ::viewMyScores,
        "/decisions/{id}/my-scores" bind Method.POST to ::submitMyScores,
        "/decisions/{id}/results" bind Method.GET to ::calculateScores,
        "/decisions/{id}/user-scores.csv" bind Method.GET to ::downloadUserScoresCsv
    )

    private fun home(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)

        // Parse query parameters for search and filters
        val search = request.query("search")?.takeIf { it.isNotBlank() }
        val recent = request.query("recent")?.let { it == "true" } ?: true
        val involved = request.query("involved") == "true"

        val filters = DecisionSearchFilters(
            search = search,
            recentOnly = recent,
            involvedOnly = involved,
            userId = currentUser.id
        )

        val decisions = decisionRepository.findDecisions(filters)

        return htmlResponse(IndexPage.indexPage(decisions, currentUser, search, recent, involved))
    }

    private fun searchDecisions(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)

        // Parse query parameters for search and filters
        val search = request.query("search")?.takeIf { it.isNotBlank() }
        val recent = request.query("recent")?.let { it == "true" } ?: true
        val involved = request.query("involved") == "true"

        val filters = DecisionSearchFilters(
            search = search,
            recentOnly = recent,
            involvedOnly = involved,
            userId = currentUser.id
        )

        val decisions = decisionRepository.findDecisions(filters)

        return if (isHx(request)) {
            htmlResponse(IndexPage.decisionsTableFragment(decisions, currentUser))
        } else {
            htmlResponse(IndexPage.indexPage(decisions, currentUser, search, recent, involved))
        }
    }

    private fun newDecisionForm(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)
        return htmlResponse(DecisionPages.createPage(currentUser))
    }

    private fun createDecision(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Name is required")

        val minScore = form["minScore"]?.toIntOrNull() ?: DEFAULT_MIN_SCORE
        val maxScore = form["maxScore"]?.toIntOrNull() ?: DEFAULT_MAX_SCORE

        if (minScore >= maxScore) {
            return Response(Status.BAD_REQUEST).body("Min score must be less than max score")
        }

        val created = decisionRepository.insert(
            DecisionInput(name = name, minScore = minScore, maxScore = maxScore),
            currentUser.id
        )
        return Response(Status.SEE_OTHER).header("Location", "/decisions/${created.id}/edit")
    }

    private fun editDecision(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)
        val id = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val decision = decisionRepository.getDecisionAggregate(id) ?: return Response(Status.NOT_FOUND).body("Decision not found")

        if (!decision.canBeModifiedBy(currentUser.id)) {
            return Response(Status.FORBIDDEN).body("You don't have permission to modify this decision")
        }

        return htmlResponse(DecisionPages.editPage(decision, currentUser))
    }

    private fun updateDecisionName(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)
        val id = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        
        if (!authorizationService.canModifyDecision(id, currentUser.id)) {
            return Response(Status.FORBIDDEN).body("You don't have permission to modify this decision")
        }
        
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Name is required")

        val minScore = form["minScore"]?.toIntOrNull() ?: DEFAULT_MIN_SCORE
        val maxScore = form["maxScore"]?.toIntOrNull() ?: DEFAULT_MAX_SCORE
        val locked = form["locked"] == "on"

        if (minScore >= maxScore) {
            return Response(Status.BAD_REQUEST).body("Min score must be less than max score")
        }

        val updated = decisionRepository.update(id, name, minScore, maxScore, locked) ?: return Response(Status.NOT_FOUND).body("Decision not found")

        return if (isHx(request)) {
            htmlResponse(DecisionPages.decisionFragment(updated))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/decisions/${updated.id}/edit")
        }
    }

    private fun createOption(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        
        if (!authorizationService.canModifyOption(decisionId, currentUser.id)) {
            return Response(Status.FORBIDDEN).body("You don't have permission to modify options for this decision")
        }
        
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Option name is required")
        optionRepository.insert(decisionId, OptionInput(name))

        return if (isHx(request)) {
            val decision = decisionRepository.getDecisionAggregate(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.optionsFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/decisions/$decisionId/edit")
        }
    }

    private fun updateOption(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val optionId = request.path("optionId")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing optionId")
        
        if (!authorizationService.canModifyOption(decisionId, currentUser.id)) {
            return Response(Status.FORBIDDEN).body("You don't have permission to modify options for this decision")
        }
        
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Option name is required")
        optionRepository.update(optionId, name) ?: return Response(Status.NOT_FOUND).body("Option not found")

        return if (isHx(request)) {
            val decision = decisionRepository.getDecisionAggregate(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.optionsFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/decisions/$decisionId/edit")
        }
    }

    private fun deleteOption(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val optionId = request.path("optionId")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing optionId")
        
        if (!authorizationService.canModifyOption(decisionId, currentUser.id)) {
            return Response(Status.FORBIDDEN).body("You don't have permission to modify options for this decision")
        }
        
        optionRepository.delete(optionId)

        return if (isHx(request)) {
            val decision = decisionRepository.getDecisionAggregate(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.optionsFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/decisions/$decisionId/edit")
        }
    }

    private fun createCriteria(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        
        if (!authorizationService.canModifyCriteria(decisionId, currentUser.id)) {
            return Response(Status.FORBIDDEN).body("You don't have permission to modify criteria for this decision")
        }
        
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        val weight = form["weight"]?.toIntOrNull() ?: 1
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Criteria name is required")
        criteriaRepository.insert(decisionId, CriteriaInput(name = name, weight = weight))

        return if (isHx(request)) {
            val decision = decisionRepository.getDecisionAggregate(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.criteriaFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/decisions/$decisionId/edit")
        }
    }

    private fun updateCriteria(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val criteriaId = request.path("criteriaId")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing criteriaId")
        
        if (!authorizationService.canModifyCriteria(decisionId, currentUser.id)) {
            return Response(Status.FORBIDDEN).body("You don't have permission to modify criteria for this decision")
        }
        
        val form = parseForm(request)
        val name = form["name"]?.trim().orEmpty()
        val weight = form["weight"]?.toIntOrNull() ?: 1
        if (name.isBlank()) return Response(Status.BAD_REQUEST).body("Criteria name is required")
        criteriaRepository.update(criteriaId, name, weight) ?: return Response(Status.NOT_FOUND).body("Criteria not found")

        return if (isHx(request)) {
            val decision = decisionRepository.getDecisionAggregate(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.criteriaFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/decisions/$decisionId/edit")
        }
    }

    private fun deleteCriteria(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val criteriaId = request.path("criteriaId")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing criteriaId")
        
        if (!authorizationService.canModifyCriteria(decisionId, currentUser.id)) {
            return Response(Status.FORBIDDEN).body("You don't have permission to modify criteria for this decision")
        }
        
        criteriaRepository.delete(criteriaId)

        return if (isHx(request)) {
            val decision = decisionRepository.getDecisionAggregate(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
            htmlResponse(DecisionPages.criteriaFragment(decision))
        } else {
            Response(Status.SEE_OTHER).header("Location", "/decisions/$decisionId/edit")
        }
    }

    private fun deleteDecision(request: Request): Response {
        val currentUser = UserContext.requireCurrent(request)
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        
        if (!authorizationService.canModifyDecision(decisionId, currentUser.id)) {
            return Response(Status.FORBIDDEN).body("You don't have permission to delete this decision")
        }
        
        decisionRepository.delete(decisionId)

        return Response(Status.SEE_OTHER).header("Location", "/")
    }

    private fun viewMyScores(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val currentUser = UserContext.requireCurrent(request)

        val decision = decisionRepository.getDecisionAggregate(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
        val userScores = userScoreRepository.findAllByDecisionId(decisionId)
            .filter { it.scoredBy == currentUser.id }

        return htmlResponse(MyScoresPages.myScoresPage(decision, currentUser, userScores))
    }

    private fun submitMyScores(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val decision = decisionRepository.getDecisionAggregate(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")
        val currentUser = UserContext.requireCurrent(request)

        if (decision.locked) {
            return Response(Status.FORBIDDEN).body("This decision is locked and cannot be scored")
        }

        val form = parseForm(request)
        val userId = currentUser.id

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
            .header("Location", "/decisions/$decisionId/my-scores")
    }

    private fun calculateScores(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val decision = decisionRepository.getDecisionAggregate(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")

        val scores = userScoreRepository.findAllByDecisionId(decisionId)

        val currentUser = UserContext.requireCurrent(request)
        return htmlResponse(ResultsPage.resultsPage(decision, scores, currentUser))
    }

    private fun downloadUserScoresCsv(request: Request): Response {
        val decisionId = request.path("id")?.toLongOrNull() ?: return Response(Status.BAD_REQUEST).body("Missing id")
        val decision = decisionRepository.getDecisionAggregate(decisionId) ?: return Response(Status.NOT_FOUND).body("Decision not found")

        val scores = userScoreRepository.findAllByDecisionId(decisionId)

        val optionMap = decision.options.associateBy { it.id }
        val criteriaMap = decision.criteria.associateBy { it.id }

        val csvContent = buildString {
            appendLine("scoredby,criteria,option,score")

            for (score in scores) {
                val optionName = optionMap[score.optionId]?.name ?: "Unknown Option"
                val criteriaName = criteriaMap[score.criteriaId]?.name ?: "Unknown Criteria"

                appendLine("${score.scoredBy},${escapeCsv(criteriaName)},${escapeCsv(optionName)},${score.score}")
            }
        }

        return Response(Status.OK)
            .header("Content-Type", "text/csv; charset=utf-8")
            .header("Content-Disposition", "attachment; filename=\"decision-${decision.id}-user-scores.csv\"")
            .body(csvContent)
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

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
