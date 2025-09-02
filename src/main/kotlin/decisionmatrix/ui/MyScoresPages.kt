package decisionmatrix.ui

import decisionmatrix.Decision
import decisionmatrix.UserScore
import kotlinx.html.*
import kotlinx.html.stream.appendHTML

object MyScoresPages {

    fun myScoresPage(decision: Decision, userId: String, scores: List<UserScore>): String = page("${decision.name} · My scores") {
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
                    // Post back to same endpoint and preserve userid in query as well
                    attributes["method"] = "post"
                    attributes["action"] = "/decisions/${decision.id}/my-scores?userid=$userId"
                    classes = setOf("stack")

                    // Ensure userid is posted in the body to be used server-side
                    hiddenInput {
                        name = "userid"
                        value = userId
                    }

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
