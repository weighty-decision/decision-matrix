package decisionmatrix.ui

import decisionmatrix.Decision
import decisionmatrix.auth.AuthenticatedUser
import kotlinx.html.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class ViewMode(val displayName: String, val paramValue: String) {
    INVOLVED("Decisions you're involved in", "involved"),
    RECENT("Recent decisions", "recent")
}

object IndexPages {

    fun indexPage(decisions: List<Decision>, currentUser: AuthenticatedUser, viewMode: ViewMode = ViewMode.INVOLVED): String = 
        PageLayout.page("Decision Matrix", user = currentUser) {
            section(classes = "card") {
                div(classes = "row") {
                    div {
                        // View mode selector
                        select(classes = "form-control") {
                            id = "view-mode-select"
                            attributes["hx-get"] = "/"
                            attributes["hx-include"] = "#view-mode-select"
                            attributes["hx-trigger"] = "change"
                            attributes["hx-target"] = "body"
                            attributes["hx-push-url"] = "true"
                            name = "view"
                            
                            ViewMode.values().forEach { mode ->
                                option {
                                    value = mode.paramValue
                                    if (mode == viewMode) {
                                        selected = true
                                    }
                                    +mode.displayName
                                }
                            }
                        }
                    }
                    a(classes = "btn primary") {
                        href = "/decisions/new"
                        +"Create New Decision"
                    }
                }

                if (decisions.isEmpty()) {
                    p(classes = "muted") {
                        when (viewMode) {
                            ViewMode.INVOLVED -> +"No decisions yet. Create your first decision to get started."
                            ViewMode.RECENT -> +"No recent decisions found."
                        }
                    }
                } else {
                    table {
                        thead {
                            tr {
                                th { +"Decision" }
                                th { +"Created" }
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
                                    td(classes = "actions") {
                                        if (decision.createdBy == currentUser.id) {
                                            a(classes = "btn small") {
                                                href = "/decisions/${decision.id}/edit"
                                                +"Edit"
                                            }
                                        }
                                        a(classes = "btn small") {
                                            href = "/decisions/${decision.id}/my-scores"
                                            +"Score"
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
