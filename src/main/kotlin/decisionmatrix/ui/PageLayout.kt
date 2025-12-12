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
                link(rel = "icon", href = "/assets/favicon.svg", type = "image/svg+xml")
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
                        
                        document.addEventListener('DOMContentLoaded', function() {
                            document.body.addEventListener('htmx:afterRequest', function(evt) {
                                if (evt.detail.successful) {
                                    // Clear any existing error messages on success
                                    const errorContainer = document.getElementById('error-notifications');
                                    if (errorContainer) {
                                        errorContainer.innerHTML = '';
                                        errorContainer.style.display = 'none';
                                    }
                                } else if (evt.detail.failed && evt.detail.xhr) {
                                    // Handle server errors (4xx/5xx)
                                    const xhr = evt.detail.xhr;
                                    const path = evt.detail.requestConfig.path || '';
                                    
                                    let message = 'Operation failed. Please try again.';
                                    
                                    if (xhr.status >= 500) {
                                        message = 'Server error occurred. Please try again later.';
                                    } else if (xhr.status >= 400) {
                                        message = 'Request failed. Please check your input and try again.';
                                    }
                                    
                                    showErrorNotification(message);
                                } else {
                                    // Handle network errors
                                    showErrorNotification('Connection error. Please check your internet connection and try again.');
                                }
                            });
                        });
                        
                        function showErrorNotification(message) {
                            const errorContainer = document.getElementById('error-notifications');
                            if (errorContainer) {
                                errorContainer.innerHTML = message;
                                errorContainer.style.display = 'block';

                                // Auto-hide after 5 seconds
                                setTimeout(function() {
                                    errorContainer.style.display = 'none';
                                }, 5000);
                            }
                        }

                        function showSuccessNotification(message) {
                            const successContainer = document.getElementById('success-notifications');
                            if (successContainer) {
                                successContainer.innerHTML = message;
                                successContainer.style.display = 'block';

                                // Auto-hide after 3 seconds
                                setTimeout(function() {
                                    successContainer.style.display = 'none';
                                }, 3000);
                            }
                        }

                        document.addEventListener('DOMContentLoaded', function() {
                            document.body.addEventListener('showSuccess', function(evt) {
                                if (evt.detail && evt.detail.message) {
                                    showSuccessNotification(evt.detail.message);
                                }
                            });

                            // Check for success message in query parameter
                            const urlParams = new URLSearchParams(window.location.search);
                            const successMessage = urlParams.get('success');
                            if (successMessage) {
                                showSuccessNotification(successMessage);
                                // Remove the query parameter from URL without reloading
                                const url = new URL(window.location);
                                url.searchParams.delete('success');
                                window.history.replaceState({}, '', url);
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

                    user?.let {
                        a(classes = "header-create-btn") {
                            href = "/decisions/new"
                            +"New Decision"
                        }
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
                div {
                    id = "success-notifications"
                    attributes["class"] = "success-toast"
                    attributes["style"] = "display: none;"
                }
                div {
                    id = "error-notifications"
                    attributes["class"] = "error-toast"
                    attributes["style"] = "display: none;"
                }
                footer(classes = "container muted") {
                }
            }
        }
    }
}
