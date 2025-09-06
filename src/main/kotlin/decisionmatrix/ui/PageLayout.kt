package decisionmatrix.ui

import decisionmatrix.auth.AuthenticatedUser
import kotlinx.html.ButtonType
import kotlinx.html.FormMethod
import kotlinx.html.MAIN
import kotlinx.html.SCRIPT
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.footer
import kotlinx.html.form
import kotlinx.html.head
import kotlinx.html.header
import kotlinx.html.html
import kotlinx.html.id
import kotlinx.html.lang
import kotlinx.html.link
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.title
import kotlinx.html.unsafe

object PageLayout {

    fun page(
        titleText: String,
        user: AuthenticatedUser? = null,
        extraTopLevelScript: (SCRIPT.() -> Unit)? = null,
        mainContent: MAIN.() -> Unit
    ): String = buildString {
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
                script {
                    unsafe {
                        +"""
                        document.addEventListener('DOMContentLoaded', function() {
                            const accountButton = document.getElementById('account-button');
                            const accountMenu = document.getElementById('account-menu');
                            
                            if (accountButton && accountMenu) {
                                accountButton.addEventListener('click', function(e) {
                                    e.stopPropagation();
                                    const isVisible = accountMenu.style.display !== 'none';
                                    
                                    if (isVisible) {
                                        accountMenu.style.display = 'none';
                                        accountButton.setAttribute('aria-expanded', 'false');
                                    } else {
                                        accountMenu.style.display = 'block';
                                        accountButton.setAttribute('aria-expanded', 'true');
                                    }
                                });
                                
                                document.addEventListener('click', function() {
                                    accountMenu.style.display = 'none';
                                    accountButton.setAttribute('aria-expanded', 'false');
                                });
                                
                                accountMenu.addEventListener('click', function(e) {
                                    e.stopPropagation();
                                });
                            }
                        });
                        """.trimIndent()
                    }
                }
                extraTopLevelScript?.let { scriptContent ->
                    script {
                        scriptContent()
                    }
                }
            }
            body {
                header(classes = "container") {
                    a(classes = "logo") {
                        href = "/"
                        +"Decision Matrix"
                    }

                    user?.let { authenticatedUser ->
                        div(classes = "account-dropdown") {
                            button(classes = "account-button") {
                                id = "account-button"
                                type = ButtonType.button
                                attributes["aria-expanded"] = "false"
                                attributes["aria-haspopup"] = "true"

                                span(classes = "account-name") { +(authenticatedUser.name ?: authenticatedUser.email) }
                                span(classes = "dropdown-arrow") { +"▾" }
                            }

                            div(classes = "account-menu") {
                                id = "account-menu"
                                attributes["role"] = "menu"
                                attributes["style"] = "display: none;"

                                form {
                                    method = FormMethod.post
                                    action = "/auth/logout"
                                    button(classes = "logout-button") {
                                        type = ButtonType.submit
                                        +"Logout"
                                    }
                                }
                            }
                        }
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
