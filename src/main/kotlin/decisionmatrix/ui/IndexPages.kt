package decisionmatrix.ui

import decisionmatrix.Decision
import kotlinx.html.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object IndexPages {

    fun indexPage(decisions: List<Decision>, currentUserId: String): String = PageLayout.page("Decision Matrix") {
        section(classes = "card") {
            div(classes = "row") {
                h1 { +"Decisions you're involved in" }
                a(classes = "btn primary") {
                    href = "/decisions/new"
                    +"Create New Decision"
                }
            }

            if (decisions.isEmpty()) {
                p(classes = "muted") {
                    +"No decisions yet. Create your first decision to get started."
                }
            } else {
                table {
                    thead {
                        tr {
                            th { +"Decision" }
                            th { +"Created" }
                            th { +"Role" }
                            th { +"Actions" }
                        }
                    }
                    tbody {
                        decisions.forEach { decision ->
                            tr {
                                td {
                                    strong { +decision.name }
                                }
                                td {
                                    decision.createdAt?.let { createdAt ->
                                        +createdAt.atZone(java.time.ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                                    } ?: span(classes = "muted") { +"Unknown" }
                                }
                                td {
                                    if (decision.createdBy == currentUserId) {
                                        span(classes = "badge primary") { +"Creator" }
                                    } else {
                                        span(classes = "badge") { +"Participant" }
                                    }
                                }
                                td(classes = "actions") {
                                    if (decision.createdBy == currentUserId) {
                                        a(classes = "btn small") {
                                            href = "/decisions/${decision.id}/edit"
                                            +"Edit"
                                        }
                                    }
                                    a(classes = "btn small") {
                                        href = "/decisions/${decision.id}/my-scores"
                                        +"My Scores"
                                    }
                                    a(classes = "btn small") {
                                        href = "/decisions/${decision.id}/calculate-scores"
                                        +"Results"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
