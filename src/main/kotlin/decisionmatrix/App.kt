package decisionmatrix

import decisionmatrix.auth.AuthConfiguration
import decisionmatrix.auth.AuthRoutes
import decisionmatrix.auth.OAuthConfiguration
import decisionmatrix.auth.OAuthServiceInterface
import decisionmatrix.auth.SessionManager
import decisionmatrix.auth.StandardsBasedOAuthService
import decisionmatrix.auth.AuthorizationService
import decisionmatrix.db.CriteriaRepositoryImpl
import decisionmatrix.db.DecisionRepositoryImpl
import decisionmatrix.db.OptionRepositoryImpl
import decisionmatrix.db.UserScoreRepositoryImpl
import decisionmatrix.db.loadJdbi
import decisionmatrix.http4k.HttpConfig
import decisionmatrix.oauth.MockOAuthServer
import decisionmatrix.routes.DecisionRoutes
import decisionmatrix.http4k.HttpServer
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("DecisionMatrix")

val jdbi = loadJdbi()

val decisionRepository = DecisionRepositoryImpl(jdbi)
val optionRepository = OptionRepositoryImpl(jdbi)
val criteriaRepository = CriteriaRepositoryImpl(jdbi)
val userScoreRepository = UserScoreRepositoryImpl(jdbi)

val httpConfig = HttpConfig.fromEnvironment()
val authConfig = AuthConfiguration.fromEnvironment()
val sessionManager = SessionManager()

// Mock OAuth server setup for testing
val mockOAuthServer = if (System.getenv("DM_MOCK_OAUTH_SERVER")?.toBoolean() == true) {
    MockOAuthServer().start()
} else null

// Create a simple dev OAuth service for dev mode
class DevOAuthService : OAuthServiceInterface {
    override fun createAuthorizationUrl(redirectAfterLogin: String): String {
        return "/auth/login"
    }

    override fun handleCallback(code: String?, state: String?, error: String?): StandardsBasedOAuthService.CallbackResult {
        return StandardsBasedOAuthService.CallbackResult.Error("Not supported in dev mode")
    }
}

val oauthService = if (!authConfig.devMode) {
    val oauthConfiguration = OAuthConfiguration.fromEnvironment()
    StandardsBasedOAuthService(oauthConfiguration)
} else {
    DevOAuthService()
}

val authRoutes = AuthRoutes(oauthService, sessionManager)

val authorizationService = AuthorizationService(decisionRepository)

val decisionRoutes = DecisionRoutes(
    decisionRepository = decisionRepository,
    optionRepository = optionRepository,
    criteriaRepository = criteriaRepository,
    userScoreRepository = userScoreRepository,
    authorizationService = authorizationService
)


fun main() {
    HttpServer(
        authRoutes = authRoutes,
        decisionRoutes = decisionRoutes,
        sessionManager = sessionManager,
        devMode = authConfig.devMode,
        devUserId = authConfig.devUserId
    ).start(port = httpConfig.port)

    if (authConfig.devMode) {
        log.info("Running in DEV MODE - authentication bypassed")
        log.info("Default dev user: {}", authConfig.devUserId ?: "dev-user")
        log.info("Override with ?dev_user=<user_id> query parameter")
    } else {
        log.info("OAuth authentication enabled - using standards-based OAuth 2.0")
    }

    if (mockOAuthServer != null) {
        log.info("Mock OAuth server running at ${mockOAuthServer.getIssuerUrl()}")
        log.info("Set DM_OAUTH_ISSUER_URL=${mockOAuthServer.getIssuerUrl()} to use it")
    }
}
