package decisionmatrix.ui

import decisionmatrix.DecisionAggregate
import decisionmatrix.UserScore
import decisionmatrix.auth.AuthenticatedUser
import decisionmatrix.score.calculateOptionScores
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.em
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import java.math.RoundingMode

object ResultsPage {

    fun resultsPage(decisionAggregate: DecisionAggregate, scores: List<UserScore>, user: AuthenticatedUser): String =
        PageLayout.page("${decisionAggregate.name} · results", user = user) {
            section(classes = "card") {
                h1 { +"Results for '${decisionAggregate.name}'" }

                if (decisionAggregate.options.isEmpty() || decisionAggregate.criteria.isEmpty()) {
                    p { +"Add options and criteria first on the edit page." }
                } else if (scores.isEmpty()) {
                    p { +"No scores yet. Ask participants to submit their scores." }
                } else {
                    val scoreReport = decisionAggregate.calculateOptionScores(scores)

                    table(classes = "full-width") {
                        thead {
                            tr {
                                th { +"Criteria" }
                                th {
                                    style = "text-align: right;"
                                    +"Possible Points"
                                    span(classes = "muted") {
                                        attributes["title"] = "The decision's maximum score * the criteria weight"
                                        style = "cursor: help;"
                                        +"\u24d8"
                                    }
                                }
                                decisionAggregate.options.sortedBy { it.id }.forEach { option ->
                                    th {
                                        style = "text-align: right;"
                                        +option.name
                                    }
                                }
                            }
                        }
                        tbody {
                            // Create rows for each criterion
                            decisionAggregate.criteria.sortedBy { it.id }.forEach { criterion ->
                                tr {
                                    td { +criterion.name }
                                    td {
                                        style = "text-align: right;"
                                        +(criterion.weight * decisionAggregate.maxScore).toString()
                                    }
                                    decisionAggregate.options.forEach { option ->
                                        val score = scoreReport.optionScores.find {
                                            it.criteriaName == criterion.name && it.optionName == option.name
                                        }?.optionScore ?: java.math.BigDecimal.ZERO
                                        td {
                                            style = "text-align: right;"
                                            +score.setScale(2, RoundingMode.HALF_UP).toPlainString()
                                        }
                                    }
                                }
                            }

                            tr(classes = "total-row") {
                                val totalPossiblePoints = decisionAggregate.criteria.sumOf { it.weight * decisionAggregate.maxScore }
                                td { +"Total" }
                                td {
                                    style = "text-align: right;"
                                    +totalPossiblePoints.toString()
                                }
                                decisionAggregate.options.sortedBy { it.id }.forEach { option ->
                                    val totalScore = scoreReport.totalScores[option] ?: java.math.BigDecimal.ZERO
                                    td {
                                        style = "text-align: right;"
                                        +totalScore.setScale(2, RoundingMode.HALF_UP).toPlainString()
                                    }
                                }
                            }
                        }
                    }

                    div(classes = "score-count") {
                        val uniqueUsers = scores.map { it.scoredBy }.distinct().size
                        p { +"Scores from $uniqueUsers participant${if (uniqueUsers == 1) "" else "s"}" }
                        p { em { +"Each criteria's score is the average score of all participants for that criteria." } }
                    }
                }

                div(classes = "actions") {
                    a(classes = "btn btn-secondary") {
                        href = "/decisions/${decisionAggregate.id}/user-scores.csv"
                        +"Download User Scores (CSV)"
                    }
                    if (decisionAggregate.canBeModifiedBy(user.id)) {
                        a(classes = "btn") {
                            href = "/decisions/${decisionAggregate.id}/edit"
                            +"Back to edit"
                        }
                    }
                }
            }
        }

}
