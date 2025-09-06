package decisionmatrix.ui

import decisionmatrix.Decision
import decisionmatrix.auth.AuthenticatedUser
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object IndexPages {

    fun indexPage(decisions: List<Decision>, currentUser: AuthenticatedUser, searchTerm: String? = null, recentFilter: Boolean = true, involvedFilter: Boolean = false): String = 
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
                        attributes["style"] = "display: contents;"
                        
                        button(classes = if (recentFilter) "btn filter-btn active" else "btn filter-btn") {
                            type = ButtonType.button
                            id = "recent-toggle"
                            attributes["style"] = "margin-left: 12px;"
                            attributes["hx-get"] = "/search"
                            attributes["hx-trigger"] = "click"
                            attributes["hx-target"] = "#decisions-table"
                            attributes["hx-vals"] = "js:{recent: document.querySelector('input[name=\"recent\"]').value === 'true' ? 'false' : 'true', involved: document.querySelector('input[name=\"involved\"]').value}"
                            attributes["hx-push-url"] = "true"
                            +"Recent"
                        }
                        
                        button(classes = if (involvedFilter) "btn filter-btn active" else "btn filter-btn") {
                            type = ButtonType.button
                            id = "involved-toggle"
                            attributes["style"] = "margin-left: 8px;"
                            attributes["hx-get"] = "/search"
                            attributes["hx-trigger"] = "click"
                            attributes["hx-target"] = "#decisions-table"
                            attributes["hx-vals"] = "js:{involved: document.querySelector('input[name=\"involved\"]').value === 'true' ? 'false' : 'true', recent: document.querySelector('input[name=\"recent\"]').value}"
                            attributes["hx-push-url"] = "true"
                            +"I'm involved in"
                        }
                        
                        // Hidden inputs to track filter state
                        input(type = InputType.hidden) {
                            id = "recent-input"
                            name = "recent"
                            value = if (recentFilter) "true" else "false"
                        }
                        input(type = InputType.hidden) {
                            id = "involved-input"
                            name = "involved" 
                            value = if (involvedFilter) "true" else "false"
                        }
                    }
                    
                    div(classes = "grow") {
                        // Empty div to push button to the right
                    }
                    a(classes = "btn primary") {
                        href = "/decisions/new"
                        attributes["style"] = "white-space: nowrap;"
                        +"Create New Decision"
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
                        // Update button appearance immediately on click
                        document.getElementById('recent-toggle').addEventListener('click', function() {
                            const button = this;
                            const hiddenInput = document.querySelector('input[name="recent"]');
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
            appendHTML().table {
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
                                a(classes = "btn small") {
                                    href = "/decisions/${decision.id}/my-scores"
                                    +"Score"
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
