package decisionmatrix.auth

import decisionmatrix.auth.providers.MockOAuthProvider
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.ServerFilters

object TestAuthSetup {
    
    fun createMockAuthFilter(userId: String = "test-user", userEmail: String = "test@example.com", userName: String? = "Test User"): Filter =
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
    
    fun createMockOAuthConfig(): OAuthConfig = OAuthConfig(
        clientId = "test-client-id",
        clientSecret = "test-client-secret",
        redirectUri = "http://localhost:9000/auth/callback"
    )
    
    fun createMockAuthRoutes(): AuthRoutes = AuthRoutes(
        oauthProvider = MockOAuthProvider(),
        oauthConfig = createMockOAuthConfig(),
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