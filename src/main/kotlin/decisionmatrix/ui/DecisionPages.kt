package decisionmatrix.ui

import decisionmatrix.Decision
import kotlinx.html.*
import kotlinx.html.stream.appendHTML

object DecisionPages {

    fun createPage(): String = page("Create decision") {
        section(classes = "card") {
            h1 { +"Create a decision" }
            form {
                attributes["method"] = "post"
                attributes["action"] = "/ui/decisions"
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
                div(classes = "actions") {
                    button(classes = "btn primary") {
                        type = ButtonType.submit
                        +"Create"
                    }
                }
            }
        }
    }

    fun editPage(decision: Decision): String = page("${decision.name} · edit") {
        unsafe {
            // Name form fragment
            +nameFragment(decision)
        }
        div(classes = "grid") {
            unsafe { +optionsFragment(decision) }
            unsafe { +criteriaFragment(decision) }
        }
        section(classes = "card") {
            h2 { +"Next steps" }
            ul {
                li {
                    a(classes = "btn") {
                        href = "/ui/decisions/${decision.id}/my-scores?userid=fakeuser"
                        +"Enter my scores"
                    }
                }
                li {
                    a(classes = "btn") {
                        href = "/ui/decisions/${decision.id}/calculate-scores"
                        +"View calculated scores"
                    }
                }
            }
        }
    }

    fun nameFragment(decision: Decision): String = buildString {
        appendHTML().section(classes = "card") {
            id = "decision-name-fragment"
            h1 { +"Edit decision" }
            form {
                // htmx: submit updates and swap the entire fragment
                attributes["hx-post"] = "/ui/decisions/${decision.id}/name"
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
                div(classes = "actions") {
                    button(classes = "btn") {
                        type = ButtonType.submit
                        +"Save name"
                    }
                }
            }
        }
    }

    fun optionsFragment(decision: Decision): String = buildString {
        appendHTML().section(classes = "card") {
            id = "options-fragment"
            h2 { +"Options" }
            ul(classes = "list") {
                decision.options.forEach { opt ->
                    li(classes = "row") {
                        form(classes = "row grow") {
                            // Update option inline
                            attributes["hx-post"] = "/ui/decisions/${decision.id}/options/${opt.id}/update"
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
                            attributes["hx-post"] = "/ui/decisions/${decision.id}/options/${opt.id}/delete"
                            attributes["hx-target"] = "#options-fragment"
                            attributes["hx-swap"] = "outerHTML"
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
                attributes["hx-post"] = "/ui/decisions/${decision.id}/options"
                attributes["hx-target"] = "#options-fragment"
                attributes["hx-swap"] = "outerHTML"
                textInput {
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

    fun criteriaFragment(decision: Decision): String = buildString {
        appendHTML().section(classes = "card") {
            id = "criteria-fragment"
            h2 { +"Criteria" }
            ul(classes = "list") {
                decision.criteria.forEach { c ->
                    li(classes = "row") {
                        form(classes = "row grow") {
                            // Update criteria inline
                            attributes["hx-post"] = "/ui/decisions/${decision.id}/criteria/${c.id}/update"
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
                                max = "10"
                            }
                            button(classes = "btn small") {
                                type = ButtonType.submit
                                +"Save"
                            }
                        }
                        form {
                            // Delete criteria inline
                            attributes["hx-post"] = "/ui/decisions/${decision.id}/criteria/${c.id}/delete"
                            attributes["hx-target"] = "#criteria-fragment"
                            attributes["hx-swap"] = "outerHTML"
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
                attributes["hx-post"] = "/ui/decisions/${decision.id}/criteria"
                attributes["hx-target"] = "#criteria-fragment"
                attributes["hx-swap"] = "outerHTML"
                textInput {
                    name = "name"
                    placeholder = "New criteria"
                    required = true
                }
                numberInput {
                    name = "weight"
                    placeholder = "Weight"
                    min = "1"
                    max = "10"
                }
                button(classes = "btn") {
                    type = ButtonType.submit
                    +"Add"
                }
            }
        }
    }

    // ---- base page layout
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
