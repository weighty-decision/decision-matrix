package decisionmatrix.http4k

import decisionmatrix.auth.AuthRoutes
import decisionmatrix.auth.SessionManager
import decisionmatrix.auth.UserContext
import decisionmatrix.auth.requireAuth
import decisionmatrix.routes.DecisionRoutes
import decisionmatrix.routes.TagRoutes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ResponseFilters
import org.http4k.filter.ServerFilters
import org.http4k.hotreload.HotReloadable
import org.http4k.routing.ResourceLoader
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.slf4j.LoggerFactory

@Serializable
data class HealthResponse(
    val status: String
)

class HttpServer(
    private val authRoutes: AuthRoutes,
    private val decisionRoutes: DecisionRoutes,
    private val tagRoutes: TagRoutes,
    private val sessionManager: SessionManager,
    private val devMode: Boolean,
    private val devUserId: String?
) {
    private val log = LoggerFactory.getLogger("HttpServer")

    private fun appRoutes(): RoutingHttpHandler = routes(
        "/ping" bind GET to {
            Response(OK).body("pong")
        },
        "/health" bind GET to {
            Response(OK)
                .header("Content-Type", "application/json")
                .body(Json.encodeToString(HealthResponse(status = "healthy")))
        },
        "/assets" bind static(ResourceLoader.Classpath("public")),
        authRoutes.routes,
        decisionRoutes.routes,
        tagRoutes.routes
    )

    fun createHttpHandler(): HttpHandler = ResponseFilters.ReportHttpTransaction { tx ->
        log.atDebug().log { "uri=${tx.request.uri} status=${tx.response.status} elapsed_ms=${tx.duration.toMillis()}" }
    }.then(ServerFilters.CatchAll { throwable ->
        log.error("Uncaught exception in request processing", throwable)
        Response(INTERNAL_SERVER_ERROR).header("content-type", "text/html").body("Internal server error")
    }).then(ServerFilters.CatchLensFailure { lensFailure ->
        log.warn("Request validation failed: ${lensFailure.message}")
        Response(BAD_REQUEST).header("content-type", "text/html").body("Invalid request: ${lensFailure.message}")
    }).then(ServerFilters.InitialiseRequestContext(UserContext.contexts))
        .then(requireAuth(sessionManager, devMode, devUserId))
        .then(appRoutes())

    fun start(port: Int) {
//        if (devMode) {
//            log.info("Running HTTP server in hot reload mode")
//            HotReloadServer.http<ReloadableHttpApp>(serverConfig = SunHttp(port)).start()
//        } else {
        log.info("Running HTTP server in production mode")
        createHttpHandler().asServer(Undertow(port)).start()
//        }
        log.info("Server started. UI available at http://localhost:$port")
    }
}

class ReloadableHttpApp : HotReloadable<HttpHandler> {
    override fun create(): HttpHandler {
        val authConfig = decisionmatrix.authConfig
        val sessionManager = decisionmatrix.sessionManager
        val authRoutes = decisionmatrix.authRoutes
        val decisionRoutes = decisionmatrix.decisionRoutes
        val tagRoutes = decisionmatrix.tagRoutes

        return HttpServer(
            authRoutes = authRoutes,
            decisionRoutes = decisionRoutes,
            tagRoutes = tagRoutes,
            sessionManager = sessionManager,
            devMode = authConfig.devMode,
            devUserId = authConfig.devUserId
        ).createHttpHandler()
    }
}
