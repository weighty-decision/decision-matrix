package decisionmatrix.ui

import decisionmatrix.Decision
import decisionmatrix.UserScore
import decisionmatrix.calculateOptionScores
import kotlinx.html.*
import java.math.RoundingMode

object CalculateScoresPages {

    fun calculateScoresPage(decision: Decision, scores: List<UserScore>): String = PageLayout.page("${decision.name} · Calculated scores") {
        section(classes = "card") {
            h1 { +"Calculated scores for '${decision.name}'" }

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

                        // Add total row
                        tr(classes = "total-row") {
                            td { +"Total" }
                            decision.options.forEach { option ->
                                val totalScore = scoreReport.totalScores[option] ?: java.math.BigDecimal.ZERO
                                td { +totalScore.setScale(2, RoundingMode.HALF_UP).toPlainString() }
                            }
                        }
                    }
                }
            }

            div(classes = "actions") {
                a(classes = "btn") {
                    href = "/decisions/${decision.id}/edit"
                    +"Back to edit"
                }
            }
        }
    }

}
