package decisionmatrix.auth

import kotlinx.serialization.json.Json
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.slf4j.LoggerFactory

class AuthRoutes(
    private val oauthProvider: OAuthProvider,
    private val oauthConfig: OAuthConfig,
    private val sessionManager: SessionManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    val routes: RoutingHttpHandler = routes(
        "/auth/login" bind Method.GET to ::loginPage,
        "/auth/callback" bind Method.GET to ::oauthCallback,
        "/auth/logout" bind Method.POST to ::logout
    )

    private fun loginPage(request: Request): Response {
        val redirectAfterLogin = request.query("redirect") ?: "/"
        val state = redirectAfterLogin // Use redirect URL as state for simplicity

        val authUrl = oauthProvider.authorizationUrl(oauthConfig, state)

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Login - Decision Matrix</title>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; margin: 40px; }
                    .login-container { max-width: 400px; margin: 100px auto; text-align: center; }
                    .login-btn { 
                        display: inline-block; 
                        padding: 12px 24px; 
                        background: #4285f4; 
                        color: white; 
                        text-decoration: none; 
                        border-radius: 4px; 
                        font-size: 16px;
                    }
                    .login-btn:hover { background: #3367d6; }
                </style>
            </head>
            <body>
                <div class="login-container">
                    <h1>Decision Matrix</h1>
                    <p>Please sign in to continue</p>
                    <a href="$authUrl" class="login-btn">Sign in with ${oauthProvider.name}</a>
                </div>
            </body>
            </html>
        """.trimIndent()

        return Response(Status.OK)
            .header("Content-Type", "text/html; charset=utf-8")
            .body(html)
    }

    private fun oauthCallback(request: Request): Response {
        val code = request.query("code")
        val state = request.query("state")
        val error = request.query("error")

        if (error != null) {
            log.warn("OAuth error: {}", error)
            return Response(Status.BAD_REQUEST).body("Authentication failed: $error")
        }

        if (code == null) {
            log.warn("No authorization code received")
            return Response(Status.BAD_REQUEST).body("No authorization code received")
        }

        try {
            // Exchange code for access token (blocking call - in production consider async)
            val accessToken = runCatching {
                oauthProvider.exchangeCodeForToken(oauthConfig, code)
            }.getOrElse { e ->
                log.error("Failed to exchange code for token", e)
                return Response(Status.INTERNAL_SERVER_ERROR).body("Failed to exchange code for token")
            }

            // Get user info (blocking call - in production consider async)
            val userInfo = runCatching {
                oauthProvider.getUserInfo(accessToken)
            }.getOrElse { e ->
                log.error("Failed to get user info", e)
                return Response(Status.INTERNAL_SERVER_ERROR).body("Failed to get user info")
            }

            // Create session
            val user = AuthenticatedUser(
                id = userInfo.id,
                email = userInfo.email,
                name = userInfo.name
            )

            val sessionId = sessionManager.createSession(user)

            log.info("User logged in: {}", user.email)

            // Redirect to original destination or home
            val redirectUrl = state?.takeIf { it.isNotBlank() } ?: "/"

            return Response(Status.SEE_OTHER)
                .header("Location", redirectUrl)
                .let { sessionManager.addSessionCookie(it, sessionId) }

        } catch (e: Exception) {
            log.error("OAuth callback error", e)
            return Response(Status.INTERNAL_SERVER_ERROR).body("Authentication failed")
        }
    }

    private fun logout(request: Request): Response {
        val sessionId = sessionManager.getSessionIdFromRequest(request)
        if (sessionId != null) {
            sessionManager.removeSession(sessionId)
        }

        return Response(Status.SEE_OTHER)
            .header("Location", "/auth/login")
            .let { sessionManager.removeSessionCookie(it) }
    }
}
