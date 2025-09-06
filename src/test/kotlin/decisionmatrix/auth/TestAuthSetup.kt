package decisionmatrix.auth

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.ServerFilters

object TestAuthSetup {

    fun createMockAuthFilter(
        userId: String = "test-user",
        userEmail: String = "test@example.com",
        userName: String? = "Test User"
    ): Filter =
        Filter { next: HttpHandler ->
            { request: Request ->
                val mockUser = AuthenticatedUser(
                    id = userId,
                    email = userEmail,
                    name = userName
                )
                next(UserContext.authenticated(mockUser)(request))
            }
        }

    fun createMockSessionManager(): SessionManager = SessionManager()

    fun createMockOAuthService(): MockOAuthService = MockOAuthService()

    fun createMockAuthRoutes(): AuthRoutes = AuthRoutes(
        oauthService = createMockOAuthService(),
        sessionManager = createMockSessionManager()
    )
}

/**
 * Extension function to add authentication context to any HttpHandler for testing
 */
fun HttpHandler.withMockAuth(
    userId: String = "test-user",
    userEmail: String = "test@example.com",
    userName: String? = "Test User"
): HttpHandler = ServerFilters.InitialiseRequestContext(UserContext.contexts)
    .then(TestAuthSetup.createMockAuthFilter(userId, userEmail, userName))
    .then(this)
