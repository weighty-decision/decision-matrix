package decisionmatrix.ui

import decisionmatrix.Decision
import decisionmatrix.Tag
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
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.html.unsafe

object IndexPage {

    fun indexPage(
        decisions: List<Decision>,
        decisionTags: Map<Long, List<Tag>>,
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
                            title = "Search by title or @tagname"
                            value = searchTerm ?: ""
                            attributes["hx-get"] = "/search"
                            attributes["hx-trigger"] = "keyup changed delay:500ms[target.value.length == 0 || target.value.length >= 3]"
                            attributes["hx-target"] = "#decisions-table"
                            attributes["hx-include"] = "#search-form"
                            attributes["hx-push-url"] = "true"
                            attributes["style"] = "width: 100%; padding: 8px 12px;"
                        }
                        div {
                            id = "tag-autocomplete"
                            attributes["style"] = """
                                display: none;
                                position: absolute;
                                top: 100%;
                                left: 0;
                                right: 0;
                                background: white;
                                border: 1px solid #ddd;
                                border-top: none;
                                border-radius: 0 0 4px 4px;
                                box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                                max-height: 200px;
                                overflow-y: auto;
                                z-index: 1000;
                            """.trimIndent()
                        }
                    }

                    form {
                        id = "search-form"
                        attributes["style"] = "display: flex; align-items: center; gap: 8px;"

                        label {
                            attributes["for"] = "time-range-select"
                            attributes["style"] = "margin-left: 12px; white-space: nowrap;"
                            +"Filter:"
                        }
                        select {
                            id = "time-range-select"
                            name = "timeRange"
                            attributes["title"] = "Show decisions created or scored within the given time"
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
                            attributes["title"] = "Only show decisions I've created or scored"
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
                    unsafe { +decisionsTableFragment(decisions, decisionTags, currentUser) }
                }
            }

            script {
                unsafe {
                    +"""
                    // Format timestamps to user's local timezone as YYYY-MM-DD
                    function formatLocalDates() {
                        document.querySelectorAll('.local-date').forEach(function(element) {
                            const timestamp = element.getAttribute('data-timestamp');
                            if (timestamp) {
                                const date = new Date(timestamp);
                                const year = date.getFullYear();
                                const month = String(date.getMonth() + 1).padStart(2, '0');
                                const day = String(date.getDate()).padStart(2, '0');
                                element.textContent = year + '-' + month + '-' + day;
                            }
                        });
                    }

                    // Format on initial page load
                    document.addEventListener('DOMContentLoaded', formatLocalDates);

                    // Format after htmx swaps content
                    document.body.addEventListener('htmx:afterSwap', formatLocalDates);

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

                        // Tag autocomplete functionality
                        const searchInput = document.getElementById('search-input');
                        const autocompleteDiv = document.getElementById('tag-autocomplete');

                        let autocompleteTimeout = null;
                        let currentTags = [];
                        let selectedIndex = -1;
                        let currentAtIndex = -1;

                        searchInput.addEventListener('input', function() {
                            const value = this.value;
                            const atIndex = value.lastIndexOf('@');

                            // Clear any existing timeout
                            if (autocompleteTimeout) {
                                clearTimeout(autocompleteTimeout);
                            }

                            // Check if we have an @ symbol and extract the prefix after it
                            if (atIndex !== -1) {
                                const prefix = value.substring(atIndex + 1);

                                // Only show autocomplete if there's at least one character after @
                                if (prefix.length > 0) {
                                    // Debounce the autocomplete request
                                    autocompleteTimeout = setTimeout(function() {
                                        fetch('/api/tags/autocomplete?q=' + encodeURIComponent(prefix))
                                            .then(response => response.json())
                                            .then(data => {
                                                if (data.tags && data.tags.length > 0) {
                                                    currentAtIndex = atIndex;
                                                    showAutocomplete(data.tags);
                                                } else {
                                                    hideAutocomplete();
                                                }
                                            })
                                            .catch(() => hideAutocomplete());
                                    }, 200);
                                } else {
                                    hideAutocomplete();
                                }
                            } else {
                                hideAutocomplete();
                            }
                        });

                        // Keyboard navigation
                        searchInput.addEventListener('keydown', function(e) {
                            if (autocompleteDiv.style.display !== 'block') return;

                            if (e.key === 'ArrowDown') {
                                e.preventDefault();
                                selectedIndex = Math.min(selectedIndex + 1, currentTags.length - 1);
                                updateSelection();
                            } else if (e.key === 'ArrowUp') {
                                e.preventDefault();
                                selectedIndex = Math.max(selectedIndex - 1, 0);
                                updateSelection();
                            } else if (e.key === 'Enter' && selectedIndex >= 0) {
                                e.preventDefault();
                                selectTag(currentTags[selectedIndex]);
                            } else if (e.key === 'Escape') {
                                e.preventDefault();
                                hideAutocomplete();
                            }
                        });

                        function showAutocomplete(tags) {
                            currentTags = tags;
                            selectedIndex = 0; // Select first item by default
                            autocompleteDiv.innerHTML = '';

                            tags.forEach(function(tag, index) {
                                const item = document.createElement('div');
                                item.className = 'autocomplete-item';
                                item.textContent = tag.name;
                                item.dataset.index = index;
                                item.style.cssText = 'padding: 8px 12px; cursor: pointer; border-bottom: 1px solid #eee; color: #333;';

                                // Mouse hover effect
                                item.addEventListener('mouseenter', function() {
                                    selectedIndex = parseInt(this.dataset.index);
                                    updateSelection();
                                });

                                // Click to select
                                item.addEventListener('click', function() {
                                    selectTag(tag);
                                });

                                autocompleteDiv.appendChild(item);
                            });

                            autocompleteDiv.style.display = 'block';
                            updateSelection();
                        }

                        function updateSelection() {
                            const items = autocompleteDiv.querySelectorAll('.autocomplete-item');
                            items.forEach(function(item, index) {
                                if (index === selectedIndex) {
                                    item.style.backgroundColor = '#007bff';
                                    item.style.color = 'white';
                                } else {
                                    item.style.backgroundColor = 'white';
                                    item.style.color = '#333';
                                }
                            });
                        }

                        function selectTag(tag) {
                            const beforeAt = searchInput.value.substring(0, currentAtIndex);
                            searchInput.value = beforeAt + '@' + tag.name;
                            hideAutocomplete();

                            // Trigger the search by dispatching htmx trigger
                            htmx.trigger(searchInput, 'keyup');
                        }

                        function hideAutocomplete() {
                            autocompleteDiv.style.display = 'none';
                            autocompleteDiv.innerHTML = '';
                            currentTags = [];
                            selectedIndex = -1;
                            currentAtIndex = -1;
                        }

                        // Close autocomplete when clicking outside
                        document.addEventListener('click', function(e) {
                            if (!searchInput.contains(e.target) && !autocompleteDiv.contains(e.target)) {
                                hideAutocomplete();
                            }
                        });
                    });
                    """.trimIndent()
                }
            }
        }

    fun decisionsTableFragment(decisions: List<Decision>, decisionTags: Map<Long, List<Tag>>, currentUser: AuthenticatedUser): String = buildString {
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
                        th { +"Author" }
                        th { +"Actions" }
                    }
                }
                tbody {
                    decisions.forEach { decision ->
                        tr {
                            td {
                                strong { +decision.name }
                                val tags = decisionTags[decision.id] ?: emptyList()
                                if (tags.isNotEmpty()) {
                                    div {
                                        attributes["style"] = "display: flex; flex-wrap: wrap; gap: 0.25rem; margin-top: 0.25rem;"
                                        tags.forEach { tag ->
                                            a(classes = "badge") {
                                                href = "/?search=@${tag.name}"
                                                attributes["title"] = "Filter by tag: ${tag.name}"
                                                +tag.name
                                            }
                                        }
                                    }
                                }
                            }
                            td(classes = "local-date") {
                                attributes["data-timestamp"] = decision.createdAt.toString()
                                +decision.createdAt.toString() // Fallback text
                            }
                            td {
                                +decision.createdBy
                            }
                            td {
                                div(classes = "actions") {
                                    if (decision.createdBy == currentUser.id) {
                                        a(classes = "btn small") {
                                            href = "/decisions/${decision.id}/edit"
                                            +"Edit"
                                        }
                                    }
                                    if (decision.locked) {
                                        span(classes = "btn small disabled") {
                                            attributes["title"] = "Scoring for this decision has been locked by the creator"
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

}
