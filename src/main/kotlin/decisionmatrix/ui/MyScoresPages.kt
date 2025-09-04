package decisionmatrix.ui

import decisionmatrix.Decision
import decisionmatrix.UserScore
import decisionmatrix.auth.AuthenticatedUser
import kotlinx.html.*

object MyScoresPages {

    fun myScoresPage(decision: Decision, user: AuthenticatedUser, scores: List<UserScore>): String = PageLayout.page("${decision.name} · My scores", user = user) {
        section(classes = "card") {
            h1 { +"My scores for '${decision.name}'" }

            if (decision.options.isEmpty() || decision.criteria.isEmpty()) {
                p {
                    +"Add options and criteria first on the edit page."
                }
            } else {
                // Build a map for quick lookup of existing scores by (optionId, criteriaId)
                val scoreMap = scores.associateBy { it.optionId to it.criteriaId }

                form {
                    attributes["method"] = "post"
                    attributes["action"] = "/decisions/${decision.id}/my-scores"
                    classes = setOf("stack")

                    table {
                        thead {
                            tr {
                                th { }
                                decision.options.forEach { opt ->
                                    th {
                                        +opt.name
                                    }
                                }
                            }
                        }
                        tbody {
                            decision.criteria.forEach { c ->
                                tr {
                                    th { +c.name }
                                    decision.options.forEach { opt ->
                                        val existing = scoreMap[opt.id to c.id]
                                        td {
                                            numberInput {
                                                name = "score_${opt.id}_${c.id}"
                                                placeholder = "Score (${decision.minScore}-${decision.maxScore})"
                                                min = decision.minScore.toString()
                                                max = decision.maxScore.toString()
                                                if (existing != null) {
                                                    value = existing.score.toString()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    div(classes = "actions") {
                        button(classes = "btn primary") {
                            type = ButtonType.submit
                            +"Save scores"
                        }
                    }
                }
            }
        }
        section(classes = "card") {
            h2 { +"Next steps" }
            ul {
                li {
                    a(classes = "btn") {
                        href = "/decisions/${decision.id}/calculate-scores"
                        +"View calculated scores"
                    }
                }
            }
        }
    }

}
