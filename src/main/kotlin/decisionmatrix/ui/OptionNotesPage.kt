package decisionmatrix.ui

import decisionmatrix.Option
import decisionmatrix.auth.AuthenticatedUser
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.section
import kotlinx.html.unsafe
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

object OptionNotesPage {

    fun viewNotesPage(option: Option, decisionName: String, user: AuthenticatedUser): String {
        val renderedNotes = if (!option.notes.isNullOrBlank()) {
            val parser = Parser.builder().build()
            val document = parser.parse(option.notes)
            val renderer = HtmlRenderer.builder().build()
            renderer.render(document)
        } else {
            "<p>No notes available for this option.</p>"
        }

        return PageLayout.page("${option.name} Notes", user = user) {
            section(classes = "card") {
                h1 { +option.name }
                div {
                    attributes["style"] = "margin-bottom: 1rem; color: #666;"
                    +"Decision: $decisionName"
                }
                div(classes = "markdown-content") {
                    unsafe {
                        +renderedNotes
                    }
                }
            }
        }
    }
}
