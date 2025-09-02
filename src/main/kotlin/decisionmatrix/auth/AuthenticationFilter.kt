package decisionmatrix.auth

import org.http4k.core.*
import org.slf4j.LoggerFactory

class AuthenticationFilter(
    private val sessionManager: SessionManager,
    private val devMode: Boolean = false,
    private val devUserId: String? = null
) : Filter {

    private val log = LoggerFactory.getLogger(AuthenticationFilter::class.java)

    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        if (shouldSkipAuth(request)) {
            next(request)
        } else if (devMode) {
            handleDevMode(request, next)
        } else {
            handleProdMode(request, next)
        }
    }

    private fun shouldSkipAuth(request: Request): Boolean {
        val path = request.uri.path
        return path.startsWith("/auth/") ||
                path.startsWith("/assets/") ||
                path == "/ping"
    }

    private fun handleDevMode(request: Request, next: HttpHandler): Response {
        // In dev mode, check for dev_user query param, then devUserId, then default
        val userId = request.query("dev_user") ?: devUserId ?: "dev-user"
        val devUser = AuthenticatedUser(
            id = userId,
            email = "$userId@example.com",
            name = "Dev User ($userId)"
        )

        log.debug("Dev mode: Using dev user {}", userId)
        return next(UserContext.authenticated(devUser)(request))
    }

    private fun handleProdMode(request: Request, next: HttpHandler): Response {
        val sessionId = sessionManager.getSessionIdFromRequest(request)

        if (sessionId != null) {
            val user = sessionManager.getSession(sessionId)
            if (user != null) {
                log.debug("Authenticated user: {}", user.email)
                return next(UserContext.authenticated(user)(request))
            } else {
                log.debug("Invalid or expired session: {}", sessionId)
            }
        }

        // No valid session, redirect to login
        val loginUrl = "/auth/login?redirect=${Uri.of(request.uri.toString()).toString()}"
        return Response(Status.SEE_OTHER)
            .header("Location", loginUrl)
    }
}

fun requireAuth(
    sessionManager: SessionManager,
    devMode: Boolean = false,
    devUserId: String? = null
): Filter = AuthenticationFilter(sessionManager, devMode, devUserId)
