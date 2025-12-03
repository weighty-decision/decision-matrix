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
import decisionmatrix.db.TagRepositoryImpl
import decisionmatrix.db.SampleDataPopulator
import decisionmatrix.db.loadJdbi
import decisionmatrix.http4k.HttpConfig
import decisionmatrix.oauth.MockOAuthServer
import decisionmatrix.routes.DecisionRoutes
import decisionmatrix.routes.TagRoutes
import decisionmatrix.http4k.HttpServer
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("DecisionMatrix")

val jdbi = loadJdbi()

val decisionRepository = DecisionRepositoryImpl(jdbi)
val optionRepository = OptionRepositoryImpl(jdbi)
val criteriaRepository = CriteriaRepositoryImpl(jdbi)
val userScoreRepository = UserScoreRepositoryImpl(jdbi)
val tagRepository = TagRepositoryImpl(jdbi)

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
    tagRepository = tagRepository,
    authorizationService = authorizationService
)

val tagRoutes = TagRoutes(
    tagRepository = tagRepository
)

fun main() {
    // Populate sample data if requested
    if (System.getenv("DM_INCLUDE_SAMPLE_DATA")?.toBoolean() == true) {
        SampleDataPopulator(
            jdbi = jdbi,
            decisionRepository = decisionRepository,
            optionRepository = optionRepository,
            criteriaRepository = criteriaRepository,
            userScoreRepository = userScoreRepository,
            tagRepository = tagRepository
        ).populateIfEmpty()
    }

    HttpServer(
        authRoutes = authRoutes,
        decisionRoutes = decisionRoutes,
        tagRoutes = tagRoutes,
        sessionManager = sessionManager,
        devMode = authConfig.devMode,
        devUserId = authConfig.devUserId
    ).start(port = httpConfig.port)

    if (authConfig.devMode) {
        log.info("Running in DEV MODE - authentication bypassed")
        log.info("Default dev user: {}", authConfig.devUserId ?: "dev-user")
    } else {
        log.info("OAuth authentication enabled - using standards-based OAuth 2.0")
    }

    if (mockOAuthServer != null) {
        log.info("Mock OAuth server running at ${mockOAuthServer.getIssuerUrl()}")
        log.info("Set DM_OAUTH_ISSUER_URL=${mockOAuthServer.getIssuerUrl()} to use it")
    }
}
