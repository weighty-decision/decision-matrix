package decisionmatrix.ui

import decisionmatrix.DEFAULT_MAX_SCORE
import decisionmatrix.DEFAULT_MIN_SCORE
import decisionmatrix.Decision
import decisionmatrix.DecisionAggregate
import decisionmatrix.auth.AuthenticatedUser
import kotlinx.html.ButtonType
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.li
import kotlinx.html.numberInput
import kotlinx.html.section
import kotlinx.html.small
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import kotlinx.html.textInput
import kotlinx.html.ul
import kotlinx.html.unsafe

object DecisionPages {

    fun createPage(user: AuthenticatedUser): String = PageLayout.page("Create decision", user = user) {
        section(classes = "card") {
            h1 { +"Create a decision" }
            form {
                attributes["method"] = "post"
                attributes["action"] = "/decisions"
                classes = setOf("stack")

                label {
                    span { +"Decision name" }
                    textInput {
                        name = "name"
                        placeholder = "e.g., Choose a laptop"
                        required = true
                        autoFocus = true
                    }
                }

                div(classes = "row") {
                    label {
                        span { +"Min score" }
                        numberInput {
                            name = "minScore"
                            value = "$DEFAULT_MIN_SCORE"
                            min = "1"
                            max = "100"
                            required = true
                        }
                    }
                    label {
                        span { +"Max score" }
                        numberInput {
                            name = "maxScore"
                            value = "$DEFAULT_MAX_SCORE"
                            min = "1"
                            max = "100"
                            required = true
                        }
                    }
                }
                div(classes = "actions") {
                    button(classes = "btn primary") {
                        type = ButtonType.submit
                        +"Create"
                    }
                }
            }
        }
    }

    fun editPage(decisionAggregate: DecisionAggregate, user: AuthenticatedUser): String =
        PageLayout.page("${decisionAggregate.name} · edit", user = user, extraTopLevelScript = {
            unsafe {
                +"""
            document.addEventListener('DOMContentLoaded', function() {
                document.body.addEventListener('htmx:afterSwap', function(evt) {
                    // Focus on new criteria input after criteria fragment swap
                    if (evt.target.id === 'criteria-fragment') {
                        setTimeout(function() {
                            var input = document.getElementById('new-criteria-input');
                            if (input) input.focus();
                        }, 50);
                    }
                    // Focus on new option input after options fragment swap
                    if (evt.target.id === 'options-fragment') {
                        setTimeout(function() {
                            var input = document.getElementById('new-option-input');
                            if (input) input.focus();
                        }, 50);
                    }
                });
            });
            """.trimIndent()
            }
        }) {
            unsafe {
                // Name form fragment
                +decisionFragment(decisionAggregate.decision)
            }
            div(classes = "grid") {
                unsafe { +criteriaFragment(decisionAggregate) }
                unsafe { +optionsFragment(decisionAggregate) }
            }
            section(classes = "card") {
                h2 { +"Next steps" }
                ul(classes = "list") {
                    li {
                        a(classes = "btn") {
                            href = "/decisions/${decisionAggregate.id}/my-scores"
                            +"Enter my scores"
                        }
                    }
                    li {
                        a(classes = "btn") {
                            href = "/decisions/${decisionAggregate.id}/results"
                            +"View results"
                        }
                    }
                }
            }
        }

    fun decisionFragment(decision: Decision): String = buildString {
        appendHTML().section(classes = "card") {
            id = "decision-name-fragment"
            h1 { +"Edit decision" }
            form {
                // htmx: submit updates and swap the entire fragment
                attributes["hx-post"] = "/decisions/${decision.id}/name"
                attributes["hx-target"] = "#decision-name-fragment"
                attributes["hx-swap"] = "outerHTML"
                classes = setOf("stack")

                label {
                    span { +"Name" }
                    textInput {
                        name = "name"
                        required = true
                        value = decision.name
                    }
                }

                div(classes = "row") {
                    label {
                        span { +"Min score" }
                        numberInput {
                            name = "minScore"
                            value = decision.minScore.toString()
                            min = "1"
                            max = "100"
                            required = true
                        }
                    }
                    div(classes = "row") {
                        label {
                            span { +"Max score" }
                            numberInput {
                                name = "maxScore"
                                value = decision.maxScore.toString()
                                min = "1"
                                max = "100"
                                required = true
                            }
                        }
                        small(classes = "muted") {
                            style = "margin-left: 0.5rem; align-self: end; margin-bottom: 0.25rem;"
                            +"Constrains the range of scores users can give each option"
                        }
                    }
                }
                div(classes = "actions") {
                    button(classes = "btn") {
                        type = ButtonType.submit
                        +"Save"
                    }
                    form {
                        attributes["method"] = "post"
                        attributes["action"] = "/decisions/${decision.id}/delete"
                        attributes["hx-confirm"] = "Are you sure you want to delete the decision '${decision.name}'? " +
                                "This will permanently delete all criteria, options, and scores. This action cannot be undone."
                        button(classes = "btn danger") {
                            type = ButtonType.submit
                            +"Delete"
                        }
                    }
                }
            }
        }
    }

    fun optionsFragment(decisionAggregate: DecisionAggregate): String = buildString {
        appendHTML().section(classes = "card") {
            id = "options-fragment"
            h2 { +"Options" }
            ul(classes = "list") {
                decisionAggregate.options.forEach { opt ->
                    li(classes = "row") {
                        form(classes = "row grow") {
                            // Update option inline
                            attributes["hx-post"] = "/decisions/${decisionAggregate.id}/options/${opt.id}/update"
                            attributes["hx-target"] = "#options-fragment"
                            attributes["hx-swap"] = "outerHTML"
                            textInput(classes = "grow") {
                                name = "name"
                                required = true
                                value = opt.name
                            }
                            button(classes = "btn small") {
                                type = ButtonType.submit
                                +"Save"
                            }
                        }
                        form {
                            // Delete option inline
                            attributes["hx-post"] = "/decisions/${decisionAggregate.id}/options/${opt.id}/delete"
                            attributes["hx-target"] = "#options-fragment"
                            attributes["hx-swap"] = "outerHTML"
                            attributes["hx-confirm"] = "Are you sure you want to delete the option '${opt.name}'? This action cannot be undone."
                            button(classes = "btn danger small") {
                                type = ButtonType.submit
                                +"Delete"
                            }
                        }
                    }
                }
            }
            form(classes = "row") {
                // Create option inline
                attributes["hx-post"] = "/decisions/${decisionAggregate.id}/options"
                attributes["hx-target"] = "#options-fragment"
                attributes["hx-swap"] = "outerHTML"
                textInput {
                    id = "new-option-input"
                    name = "name"
                    placeholder = "New option"
                    required = true
                }
                button(classes = "btn") {
                    type = ButtonType.submit
                    +"Add"
                }
            }
        }
    }

    fun criteriaFragment(decisionAggregate: DecisionAggregate): String = buildString {
        appendHTML().section(classes = "card") {
            id = "criteria-fragment"
            h2 { +"Criteria" }
            ul(classes = "list") {
                decisionAggregate.criteria.forEach { c ->
                    li(classes = "row") {
                        form(classes = "row grow") {
                            // Update criteria inline
                            attributes["hx-post"] = "/decisions/${decisionAggregate.id}/criteria/${c.id}/update"
                            attributes["hx-target"] = "#criteria-fragment"
                            attributes["hx-swap"] = "outerHTML"
                            textInput(classes = "grow") {
                                name = "name"
                                required = true
                                value = c.name
                            }
                            numberInput {
                                name = "weight"
                                value = c.weight.toString()
                                min = "1"
                            }
                            button(classes = "btn small") {
                                type = ButtonType.submit
                                +"Save"
                            }
                        }
                        form {
                            // Delete criteria inline
                            attributes["hx-post"] = "/decisions/${decisionAggregate.id}/criteria/${c.id}/delete"
                            attributes["hx-target"] = "#criteria-fragment"
                            attributes["hx-swap"] = "outerHTML"
                            attributes["hx-confirm"] = "Are you sure you want to delete the criteria '${c.name}'? This action cannot be undone."
                            button(classes = "btn danger small") {
                                type = ButtonType.submit
                                +"Delete"
                            }
                        }
                    }
                }
            }
            form(classes = "row") {
                // Create criteria inline
                attributes["hx-post"] = "/decisions/${decisionAggregate.id}/criteria"
                attributes["hx-target"] = "#criteria-fragment"
                attributes["hx-swap"] = "outerHTML"
                textInput {
                    id = "new-criteria-input"
                    name = "name"
                    placeholder = "New criteria"
                    required = true
                }
                numberInput {
                    name = "weight"
                    placeholder = "Weight"
                    min = "1"
                }
                button(classes = "btn") {
                    type = ButtonType.submit
                    +"Add"
                }
            }
        }
    }

}
