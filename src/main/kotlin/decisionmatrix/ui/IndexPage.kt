package decisionmatrix.ui

import decisionmatrix.Decision
import decisionmatrix.auth.AuthenticatedUser
import decisionmatrix.db.TimeRange
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.section
import kotlinx.html.select
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.strong
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.unsafe
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object IndexPage {

    fun indexPage(
        decisions: List<Decision>,
        currentUser: AuthenticatedUser,
        searchTerm: String? = null,
        timeRange: TimeRange = TimeRange.LAST_90_DAYS,
        involvedFilter: Boolean = false
    ): String =
        PageLayout.page("Decision Matrix", user = currentUser) {
            section(classes = "card") {
                div(classes = "row") {
                    div(classes = "search-container") {
                        attributes["style"] = "position: relative; flex: 1; max-width: 400px;"
                        input(type = InputType.search) {
                            id = "search-input"
                            name = "search"
                            placeholder = "Search decisions..."
                            value = searchTerm ?: ""
                            attributes["hx-get"] = "/search"
                            attributes["hx-trigger"] = "keyup changed delay:500ms[target.value.length == 0 || target.value.length >= 3]"
                            attributes["hx-target"] = "#decisions-table"
                            attributes["hx-include"] = "#search-form"
                            attributes["hx-push-url"] = "true"
                            attributes["style"] = "width: 100%; padding: 8px 12px;"
                        }
                    }

                    form {
                        id = "search-form"
                        attributes["style"] = "display: flex; align-items: center; gap: 8px;"

                        label {
                            attributes["for"] = "time-range-select"
                            attributes["style"] = "margin-left: 12px; white-space: nowrap;"
                            +"Show:"
                        }
                        select {
                            id = "time-range-select"
                            name = "timeRange"
                            attributes["hx-get"] = "/search"
                            attributes["hx-trigger"] = "change"
                            attributes["hx-target"] = "#decisions-table"
                            attributes["hx-include"] = "#search-form"
                            attributes["hx-push-url"] = "true"

                            option {
                                value = ""
                                selected = (timeRange == TimeRange.ALL)
                                +"All decisions"
                            }
                            option {
                                value = "7"
                                selected = (timeRange == TimeRange.LAST_7_DAYS)
                                +"Last 7 days"
                            }
                            option {
                                value = "30"
                                selected = (timeRange == TimeRange.LAST_30_DAYS)
                                +"Last 30 days"
                            }
                            option {
                                value = "90"
                                selected = (timeRange == TimeRange.LAST_90_DAYS)
                                +"Last 90 days"
                            }
                            option {
                                value = "180"
                                selected = (timeRange == TimeRange.LAST_6_MONTHS)
                                +"Last 6 months"
                            }
                        }

                        button(classes = if (involvedFilter) "btn filter-btn active" else "btn filter-btn") {
                            type = ButtonType.button
                            id = "involved-toggle"
                            attributes["hx-get"] = "/search"
                            attributes["hx-trigger"] = "click"
                            attributes["hx-target"] = "#decisions-table"
                            attributes["hx-vals"] =
                                "js:{involved: document.querySelector('input[name=\"involved\"]').value === 'true' ? 'false' : 'true', " +
                                        "timeRange: document.getElementById('time-range-select').value, " +
                                        "search: document.getElementById('search-input').value}"
                            attributes["hx-push-url"] = "true"
                            +"I'm involved in"
                        }

                        // Hidden input to track involved filter state
                        input(type = InputType.hidden) {
                            id = "involved-input"
                            name = "involved"
                            value = if (involvedFilter) "true" else "false"
                        }
                    }
                }

                div {
                    id = "decisions-table"
                    unsafe { +decisionsTableFragment(decisions, currentUser) }
                }
            }

            script {
                unsafe {
                    +"""
                    // Toggle button behavior - update local hidden inputs and button appearance
                    document.addEventListener('DOMContentLoaded', function() {
                        document.getElementById('involved-toggle').addEventListener('click', function() {
                            const button = this;
                            const hiddenInput = document.querySelector('input[name="involved"]');
                            const currentValue = hiddenInput.value === 'true';
                            const newValue = !currentValue;

                            // Update local state
                            hiddenInput.value = newValue.toString();

                            // Update button appearance immediately
                            if (newValue) {
                                button.classList.add('active');
                            } else {
                                button.classList.remove('active');
                            }
                        });
                    });
                    """.trimIndent()
                }
            }
        }

    fun decisionsTableFragment(decisions: List<Decision>, currentUser: AuthenticatedUser): String = buildString {
        if (decisions.isEmpty()) {
            appendHTML().p(classes = "muted") {
                +"No decisions found. Try adjusting your search or filters, or create your first decision to get started."
            }
        } else {
            appendHTML().table(classes = "full-width") {
                thead {
                    tr {
                        th { +"Decision" }
                        th { +"Created" }
                        th(classes = "actions") { +"Actions" }
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
                                if (decision.locked) {
                                    span(classes = "btn small disabled") {
                                        +"Locked"
                                    }
                                } else {
                                    a(classes = "btn small") {
                                        href = "/decisions/${decision.id}/my-scores"
                                        +"Score"
                                    }
                                }
                                a(classes = "btn small") {
                                    href = "/decisions/${decision.id}/results"
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
