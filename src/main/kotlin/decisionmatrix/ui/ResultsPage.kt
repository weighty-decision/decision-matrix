package decisionmatrix.ui

import decisionmatrix.Decision
import decisionmatrix.UserScore
import decisionmatrix.auth.AuthenticatedUser
import decisionmatrix.calculateOptionScores
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import java.math.RoundingMode

object ResultsPage {

    fun resultsPage(decision: Decision, scores: List<UserScore>, user: AuthenticatedUser): String =
        PageLayout.page("${decision.name} · results", user = user) {
            section(classes = "card") {
                h1 { +"Results for '${decision.name}'" }

                if (decision.options.isEmpty() || decision.criteria.isEmpty()) {
                    p { +"Add options and criteria first on the edit page." }
                } else if (scores.isEmpty()) {
                    p { +"No scores yet. Ask participants to submit their scores." }
                } else {
                    val scoreReport = decision.calculateOptionScores(scores)

                    table {
                        thead {
                            tr {
                                th { +"Criteria" }
                                decision.options.forEach { option ->
                                    th { +option.name }
                                }
                            }
                        }
                        tbody {
                            // Create rows for each criterion
                            decision.criteria.forEach { criterion ->
                                tr {
                                    td { +"${criterion.name} (×${criterion.weight})" }
                                    decision.options.forEach { option ->
                                        val score = scoreReport.optionScores.find {
                                            it.criteriaName == criterion.name && it.optionName == option.name
                                        }?.optionScore ?: java.math.BigDecimal.ZERO
                                        td { +score.setScale(2, RoundingMode.HALF_UP).toPlainString() }
                                    }
                                }
                            }

                            tr(classes = "total-row") {
                                td { +"Total" }
                                decision.options.forEach { option ->
                                    val totalScore = scoreReport.totalScores[option] ?: java.math.BigDecimal.ZERO
                                    td { +totalScore.setScale(2, RoundingMode.HALF_UP).toPlainString() }
                                }
                            }
                        }
                    }

                    div(classes = "score-count") {
                        val uniqueUsers = scores.map { it.scoredBy }.distinct().size
                        p { +"Scores from $uniqueUsers participant${if (uniqueUsers == 1) "" else "s"}" }
                    }
                }

                div(classes = "actions") {
                    a(classes = "btn btn-secondary") {
                        href = "/decisions/${decision.id}/user-scores.csv"
                        +"Download User Scores (CSV)"
                    }
                    a(classes = "btn") {
                        href = "/decisions/${decision.id}/edit"
                        +"Back to edit"
                    }
                }
            }
        }

}
