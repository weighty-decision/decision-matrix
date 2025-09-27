package decisionmatrix

import decisionmatrix.auth.AuthConfiguration
import decisionmatrix.auth.AuthRoutes
import decisionmatrix.auth.OAuthConfiguration
import decisionmatrix.auth.OAuthServiceInterface
import decisionmatrix.auth.SessionManager
import decisionmatrix.auth.StandardsBasedOAuthService
import decisionmatrix.auth.UserContext
import decisionmatrix.auth.requireAuth
import decisionmatrix.auth.AuthorizationService
import decisionmatrix.db.CriteriaRepositoryImpl
import decisionmatrix.db.DecisionRepositoryImpl
import decisionmatrix.db.OptionRepositoryImpl
import decisionmatrix.db.UserScoreRepositoryImpl
import decisionmatrix.db.loadJdbi
import decisionmatrix.oauth.MockOAuthServer
import decisionmatrix.routes.DecisionRoutes
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ResponseFilters
import org.http4k.filter.ServerFilters
import org.http4k.routing.ResourceLoader
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("DecisionMatrix")

val jdbi = loadJdbi()

val decisionRepository = DecisionRepositoryImpl(jdbi)
val optionRepository = OptionRepositoryImpl(jdbi)
val criteriaRepository = CriteriaRepositoryImpl(jdbi)
val userScoreRepository = UserScoreRepositoryImpl(jdbi)

// Authentication setup
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

val app: RoutingHttpHandler = routes(
    "/ping" bind GET to {
        Response(OK).body("pong")
    }, "/assets" bind static(ResourceLoader.Classpath("public")), authRoutes.routes, decisionRoutes.routes
)

private const val SERVER_PORT = 9000

fun main() {
    val app: HttpHandler = ResponseFilters.ReportHttpTransaction { tx ->
        log.atDebug().log { "uri=${tx.request.uri} status=${tx.response.status} elapsed_ms=${tx.duration.toMillis()}" }
    }.then(ServerFilters.CatchAll { throwable ->
        log.error("Uncaught exception in request processing", throwable)
        Response(INTERNAL_SERVER_ERROR).header("content-type", "text/html").body("Internal server error")
    }).then(ServerFilters.CatchLensFailure { lensFailure ->
        log.warn("Request validation failed: ${lensFailure.message}")
        Response(BAD_REQUEST).header("content-type", "text/html").body("Invalid request: ${lensFailure.message}")
    }).then(ServerFilters.InitialiseRequestContext(UserContext.contexts))
        .then(requireAuth(sessionManager, authConfig.devMode, authConfig.devUserId)).then(app)

    val server = app.asServer(Undertow(SERVER_PORT)).start()

    log.info("Server started. UI available at http://localhost:${server.port()}")
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
