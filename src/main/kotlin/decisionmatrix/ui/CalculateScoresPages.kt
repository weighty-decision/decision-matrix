package decisionmatrix.ui

import decisionmatrix.Decision
import decisionmatrix.UserScore
import decisionmatrix.calculateOptionScores
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.math.RoundingMode

object CalculateScoresPages {

    fun calculateScoresPage(decision: Decision, scores: List<UserScore>): String = page("${decision.name} · Calculated scores") {
        section(classes = "card") {
            h1 { +"Calculated scores for '${decision.name}'" }

            if (decision.options.isEmpty() || decision.criteria.isEmpty()) {
                p { +"Add options and criteria first on the edit page." }
            } else if (scores.isEmpty()) {
                p { +"No scores yet. Ask participants to submit their scores." }
            } else {
                val results = decision.calculateOptionScores(scores)
                    .totalScores.entries.sortedByDescending { it.value }

                table {
                    thead {
                        tr {
                            th { +"Option" }
                            th { +"Score" }
                        }
                    }
                    tbody {
                        results.forEach { (opt, score) ->
                            tr {
                                td { +opt.name }
                                td { +score.setScale(2, RoundingMode.HALF_UP).toPlainString() }
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

    // ---- base page layout (duplicated for isolation)
    private fun page(titleText: String, mainContent: MAIN.() -> Unit): String = buildString {
        appendHTML().html {
            lang = "en"
            head {
                meta { charset = "utf-8" }
                meta {
                    name = "viewport"
                    content = "width=device-width, initial-scale=1"
                }
                title { +titleText }
                link(rel = "stylesheet", href = "/assets/style.css")
                script {
                    src = "https://unpkg.com/htmx.org@2.0.2"
                }
            }
            body {
                header(classes = "container") {
                    a(classes = "logo") {
                        href = "/"
                        +"Decision Matrix"
                    }
                }
                main(classes = "container") {
                    mainContent()
                }
                footer(classes = "container muted") {
                }
            }
        }
    }
}
