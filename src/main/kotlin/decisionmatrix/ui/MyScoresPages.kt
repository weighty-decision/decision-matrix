package decisionmatrix.ui

import decisionmatrix.DecisionAggregate
import decisionmatrix.UserScore
import decisionmatrix.auth.AuthenticatedUser
import kotlinx.html.ButtonType
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.li
import kotlinx.html.numberInput
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.ul

object MyScoresPages {

    fun myScoresPage(decisionAggregate: DecisionAggregate, user: AuthenticatedUser, scores: List<UserScore>): String =
        PageLayout.page("${decisionAggregate.name} · My scores", user = user) {
            section(classes = "card") {
                h1 { +"My scores for '${decisionAggregate.name}'" }

                if (decisionAggregate.locked) {
                    p {
                        +"This decision is locked and cannot be scored."
                    }
                } else if (decisionAggregate.options.isEmpty() || decisionAggregate.criteria.isEmpty()) {
                    p {
                        +"Add options and criteria first on the edit page."
                    }
                } else {
                    p {
                        +"Leave a field blank if you can't evaluate an option for that criterion. Omitted scores won't penalize the option."
                    }

                    p {
                        +"Score each option from ${decisionAggregate.minScore} (lowest) to "
                        +"${decisionAggregate.maxScore} (highest) based on how well it meets each criterion."
                    }

                    // Build a map for quick lookup of existing scores by (optionId, criteriaId)
                    val scoreMap = scores.associateBy { it.optionId to it.criteriaId }

                    form {
                        attributes["method"] = "post"
                        attributes["action"] = "/decisions/${decisionAggregate.id}/my-scores"
                        classes = setOf("stack")

                        table {
                            thead {
                                tr {
                                    th { }
                                    decisionAggregate.options.forEach { opt ->
                                        th {
                                            +opt.name
                                        }
                                    }
                                }
                            }
                            tbody {
                                decisionAggregate.criteria.forEach { c ->
                                    tr {
                                        th { +c.name }
                                        decisionAggregate.options.forEach { opt ->
                                            val existing = scoreMap[opt.id to c.id]
                                            td {
                                                numberInput {
                                                    name = "score_${opt.id}_${c.id}"
                                                    min = decisionAggregate.minScore.toString()
                                                    max = decisionAggregate.maxScore.toString()
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
                            href = "/decisions/${decisionAggregate.id}/results"
                            +"View results"
                        }
                    }
                }
            }
        }

}
