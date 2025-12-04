package decisionmatrix.ui

import decisionmatrix.DecisionAggregate
import decisionmatrix.UserScore
import decisionmatrix.auth.AuthenticatedUser
import kotlinx.html.ButtonType
import kotlinx.html.a
import kotlinx.html.br
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.id
import kotlinx.html.li
import kotlinx.html.numberInput
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.span
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.ul
import kotlinx.html.unsafe

object MyScoresPages {

    fun myScoresPage(decisionAggregate: DecisionAggregate, user: AuthenticatedUser, scores: List<UserScore>): String =
        PageLayout.page(
            "${decisionAggregate.name} · My scores",
            user = user,
            extraTopLevelScript = {
                unsafe {
                    +"""
                        function showNotesModal(optionId, decisionId, optionName) {
                            const modal = document.getElementById('notes-modal');
                            const modalTitle = document.getElementById('notes-modal-title');
                            const modalBody = document.getElementById('notes-modal-body');

                            modalTitle.textContent = optionName + ' Notes';
                            modalBody.innerHTML = '<div class="loading">Loading...</div>';
                            modal.classList.add('show');

                            fetch('/decisions/' + decisionId + '/options/' + optionId + '/notes-content')
                                .then(response => response.text())
                                .then(html => {
                                    modalBody.innerHTML = html;
                                })
                                .catch(error => {
                                    modalBody.innerHTML = '<p style="color: var(--danger);">Failed to load notes.</p>';
                                });
                        }

                        function closeNotesModal() {
                            const modal = document.getElementById('notes-modal');
                            modal.classList.remove('show');
                        }

                        document.addEventListener('DOMContentLoaded', function() {
                            const modal = document.getElementById('notes-modal');
                            const overlay = modal;

                            overlay.addEventListener('click', function(e) {
                                if (e.target === overlay) {
                                    closeNotesModal();
                                }
                            });

                            document.addEventListener('keydown', function(e) {
                                if (e.key === 'Escape') {
                                    closeNotesModal();
                                }
                            });
                        });
                    """.trimIndent()
                }
            }
        ) {
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

                        table(classes = "full-width") {
                            attributes["id"] = "scores-table"
                            thead {
                                tr {
                                    th {
                                        a(classes = "btn-link") {
                                            href = "#"
                                            attributes["hx-on:click"] = """
                                                event.preventDefault();
                                                document.querySelectorAll('.criterion-weight').forEach(el => {
                                                    el.classList.toggle('hidden');
                                                });
                                                this.textContent = this.textContent.includes('Show') ? 'Hide weights' : 'Show weights';
                                            """.trimIndent()
                                            +"Show weights"
                                        }
                                    }
                                    decisionAggregate.options.forEach { opt ->
                                        th {
                                            +opt.name
                                            if (!opt.notes.isNullOrBlank()) {
                                                br { }
                                                a(classes = "view-notes-link") {
                                                    href = "#"
                                                    attributes["data-option-id"] = opt.id.toString()
                                                    attributes["data-decision-id"] = decisionAggregate.id.toString()
                                                    attributes["data-option-name"] = opt.name
                                                    attributes["onclick"] = "showNotesModal(${opt.id}, ${decisionAggregate.id}, '${opt.name}'); return false;"
                                                    +"view notes"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            tbody {
                                decisionAggregate.criteria.forEach { c ->
                                    tr {
                                        th {
                                            +c.name
                                            +" "
                                            span(classes = "criterion-weight hidden") {
                                                +"(weight: ${c.weight})"
                                            }
                                        }
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

            // Modal for viewing notes
            div(classes = "modal-overlay") {
                id = "notes-modal"
                div(classes = "modal-content") {
                    attributes["onclick"] = "event.stopPropagation();"
                    div(classes = "modal-header") {
                        h2 { id = "notes-modal-title"; +"Notes" }
                        button(classes = "modal-close") {
                            attributes["onclick"] = "closeNotesModal()"
                            attributes["aria-label"] = "Close"
                            +"×"
                        }
                    }
                    div(classes = "modal-body markdown-content") {
                        id = "notes-modal-body"
                    }
                }
            }
        }

}
