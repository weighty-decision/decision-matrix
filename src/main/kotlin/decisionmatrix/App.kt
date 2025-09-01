package decisionmatrix

import decisionmatrix.db.*
import decisionmatrix.routes.DecisionRoutes
import decisionmatrix.routes.CriteriaRoutes
import decisionmatrix.routes.OptionRoutes
import decisionmatrix.routes.DecisionUiRoutes
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.routing.ResourceLoader
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.server.Undertow
import org.http4k.server.asServer

val jdbi = loadDatabase()

val decisionRepository = DecisionRepositoryImpl(jdbi)
val optionRepository = OptionRepositoryImpl(jdbi)
val criteriaRepository = CriteriaRepositoryImpl(jdbi)
val optionCriteriaScoreRepository = OptionCriteriaScoreRepositoryImpl(jdbi)

val decisionRoutes = DecisionRoutes(decisionRepository)
val criteriaRoutes = CriteriaRoutes(criteriaRepository)
val optionRoutes = OptionRoutes(optionRepository)
val decisionUiRoutes = DecisionUiRoutes(
    decisionRepository = decisionRepository,
    optionRepository = optionRepository,
    criteriaRepository = criteriaRepository
)

val app: RoutingHttpHandler = routes(
    "/ping" bind GET to {
        Response(OK).body("pong")
    },
    // static assets
    "/assets" bind static(ResourceLoader.Classpath("public")),
    // API routes
    decisionRoutes.routes,
    criteriaRoutes.routes,
    optionRoutes.routes,
    // UI routes
    decisionUiRoutes.routes
)

fun main() {
    val app: HttpHandler = PrintRequest()
//        .then(ServerFilters.OpenTelemetryTracing())
//        .then(ServerFilters.OpenTelemetryMetrics.RequestCounter())
//        .then(ServerFilters.OpenTelemetryMetrics.RequestTimer())
        .then(app)

    val server = app.asServer(Undertow(9000)).start()

    println("Server started on " + server.port())
}
