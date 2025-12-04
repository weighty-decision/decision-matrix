package decisionmatrix.ui

import decisionmatrix.DEFAULT_MAX_SCORE
import decisionmatrix.DEFAULT_MIN_SCORE
import decisionmatrix.Decision
import decisionmatrix.DecisionAggregate
import decisionmatrix.auth.AuthenticatedUser
import decisionmatrix.score.calculateWeightPercentage
import kotlinx.html.ButtonType
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.checkBoxInput
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
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.textArea
import kotlinx.html.textInput
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
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
                    small(classes = "muted") {
                        style = "align-self: end; margin-bottom: 0.25rem;"
                        +"Constrains the minimum and maximum score allowed for each option"
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
            unsafe { +tagsFragment(decisionAggregate) }
            unsafe { +criteriaFragment(decisionAggregate) }
            unsafe { +optionsFragment(decisionAggregate) }
            section(classes = "card") {
                h2 { +"Next steps" }
                ul(classes = "list") {
                    li {
                        if (decisionAggregate.locked) {
                            span(classes = "btn disabled") {
                                +"Decision is locked"
                            }
                        } else {
                            a(classes = "btn") {
                                href = "/decisions/${decisionAggregate.id}/my-scores"
                                +"Enter my scores"
                            }
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
                            style = "align-self: end; margin-bottom: 0.25rem;"
                            +"Constrains the minimum and maximum score allowed for each option"
                        }
                    }
                }

                label(classes = "row") {
                    checkBoxInput {
                        name = "locked"
                        checked = decision.locked
                    }
                    span {
                        +"Lock decision to prevent users from creating or modifying scores"
                    }
                }
            }
            div(classes = "actions") {
                form {
                    attributes["hx-post"] = "/decisions/${decision.id}/name"
                    attributes["hx-target"] = "#decision-name-fragment"
                    attributes["hx-swap"] = "outerHTML"
                    attributes["hx-include"] = "#decision-name-fragment input"
                    attributes["style"] = "display: inline;"
                    button(classes = "btn") {
                        type = ButtonType.submit
                        +"Save"
                    }
                }
                form {
                    attributes["method"] = "post"
                    attributes["action"] = "/decisions/${decision.id}/delete"
                    attributes["onsubmit"] = "return confirm('Are you sure you want to delete the decision? " +
                            "This will permanently delete all criteria, options, and scores. This action cannot be undone.');"
                    attributes["style"] = "display: inline;"
                    button(classes = "btn danger") {
                        type = ButtonType.submit
                        +"Delete"
                    }
                }
            }
        }
    }

    fun tagsFragment(decisionAggregate: DecisionAggregate): String = buildString {
        appendHTML().section(classes = "card") {
            id = "tags-fragment"
            h2 { +"Tags" }
            form {
                attributes["hx-post"] = "/decisions/${decisionAggregate.id}/tags"
                attributes["hx-target"] = "#tags-fragment"
                attributes["hx-swap"] = "outerHTML"
                classes = setOf("stack")

                label {
                    span {
                        +"Categorize this decision with space-separated tags"
                    }
                    textInput {
                        name = "tags"
                        placeholder = "tag1 tag2"
                        value = decisionAggregate.tags.joinToString(" ") { it.name }
                        attributes["maxlength"] = "150"
                    }
                    small(classes = "muted") {
                        +"Use lowercase letters, numbers, and hyphens. Maximum 5 tags, 25 characters each."
                    }
                }

                if (decisionAggregate.tags.isNotEmpty()) {
                    div {
                        style = "display: flex; flex-wrap: wrap; gap: 0.5rem; margin-top: 0.5rem;"
                        decisionAggregate.tags.forEach { tag ->
                            span(classes = "badge") {
                                +tag.name
                            }
                        }
                    }
                }

                button(classes = "btn") {
                    type = ButtonType.submit
                    +"Save"
                }
            }
        }
    }

    fun criteriaFragment(decisionAggregate: DecisionAggregate): String = buildString {
        appendHTML().section(classes = "card") {
            id = "criteria-fragment"
            h2 { +"Criteria" }
            table {
                thead {
                    tr {
                        th { +"Name" }
                        th {
                            +"Weight "
                            span(classes = "muted") {
                                attributes["title"] = "The relative importance of this criteria. Higher values mean " +
                                        "this criteria has more influence on the final decision. For example a weight of 2 " +
                                        "will be twice as important as a criteria with a weight of 1."
                                style = "cursor: help;"
                                +"\u24d8"
                            }
                        }
                        th {
                            +"Weight % "
                            span(classes = "muted") {
                                attributes["title"] = "The percentage this criteria contributes to the total weight across all criteria."
                                style = "cursor: help;"
                                +"\u24d8"
                            }
                        }
                        th { }
                    }
                }
                tbody {
                    decisionAggregate.criteria.sortedBy { it.id }.forEach { criteria ->
                        tr {
                            td {
                                textInput {
                                    name = "name"
                                    required = true
                                    value = criteria.name
                                }
                            }
                            td {
                                numberInput {
                                    name = "weight"
                                    value = criteria.weight.toString()
                                    min = "1"
                                }
                            }
                            td {
                                val percentage = criteria.calculateWeightPercentage(decisionAggregate.criteria)
                                if (percentage != null) {
                                    small(classes = "muted") {
                                        +"$percentage%"
                                    }
                                }
                            }
                            td {
                                div(classes = "row") {
                                    form {
                                        attributes["hx-post"] = "/decisions/${decisionAggregate.id}/criteria/${criteria.id}/update"
                                        attributes["hx-target"] = "#criteria-fragment"
                                        attributes["hx-swap"] = "outerHTML"
                                        attributes["hx-include"] = "closest tr"
                                        button(classes = "btn small") {
                                            type = ButtonType.submit
                                            +"Save"
                                        }
                                    }
                                    form {
                                        attributes["hx-post"] = "/decisions/${decisionAggregate.id}/criteria/${criteria.id}/delete"
                                        attributes["hx-target"] = "#criteria-fragment"
                                        attributes["hx-swap"] = "outerHTML"
                                        attributes["hx-confirm"] =
                                            "Are you sure you want to delete the criteria '${criteria.name}'? " +
                                                    "This action cannot be undone."
                                        button(classes = "btn danger small") {
                                            type = ButtonType.submit
                                            +"Delete"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Create new criteria row
                    tr {
                        td {
                            textInput {
                                id = "new-criteria-input"
                                name = "name"
                                placeholder = "New criteria"
                                required = true
                            }
                        }
                        td {
                            numberInput {
                                name = "weight"
                                placeholder = "Weight"
                                min = "1"
                            }
                        }
                        td { }
                        td {
                            form {
                                attributes["hx-post"] = "/decisions/${decisionAggregate.id}/criteria"
                                attributes["hx-target"] = "#criteria-fragment"
                                attributes["hx-swap"] = "outerHTML"
                                attributes["hx-include"] = "closest tr"
                                button(classes = "btn") {
                                    type = ButtonType.submit
                                    +"Add"
                                }
                            }
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
            table {
                thead {
                    tr {
                        th { +"Name" }
                        th { +"Notes" }
                        th { }
                    }
                }
                tbody {
                    decisionAggregate.options.sortedBy { it.id }.forEach { opt ->
                        tr {
                            td {
                                textInput {
                                    name = "name"
                                    required = true
                                    value = opt.name
                                }
                            }
                            td {
                                textArea {
                                    name = "notes"
                                    placeholder = "Markdown notes (optional)"
                                    rows = "3"
                                    opt.notes?.let { +it }
                                }
                            }
                            td {
                                div(classes = "row") {
                                    form {
                                        // Update option inline
                                        attributes["hx-post"] = "/decisions/${decisionAggregate.id}/options/${opt.id}/update"
                                        attributes["hx-target"] = "#options-fragment"
                                        attributes["hx-swap"] = "outerHTML"
                                        attributes["hx-include"] = "closest tr"
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
                    }
                    // Create new option row
                    tr {
                        td {
                            textInput {
                                id = "new-option-input"
                                name = "name"
                                placeholder = "New option"
                                required = true
                            }
                        }
                        td { }
                        td {
                            form {
                                // Create option inline
                                attributes["hx-post"] = "/decisions/${decisionAggregate.id}/options"
                                attributes["hx-target"] = "#options-fragment"
                                attributes["hx-swap"] = "outerHTML"
                                attributes["hx-include"] = "closest tr"
                                button(classes = "btn") {
                                    type = ButtonType.submit
                                    +"Add"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
