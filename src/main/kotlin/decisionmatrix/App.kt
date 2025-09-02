package decisionmatrix

import decisionmatrix.db.*
import decisionmatrix.routes.DecisionUiRoutes
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.routing.*
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("DecisionMatrix")

val jdbi = loadDatabase()

val decisionRepository = DecisionRepositoryImpl(jdbi)
val optionRepository = OptionRepositoryImpl(jdbi)
val criteriaRepository = CriteriaRepositoryImpl(jdbi)
val userScoreRepository = UserScoreRepositoryImpl(jdbi)

val decisionUiRoutes = DecisionUiRoutes(
    decisionRepository = decisionRepository,
    optionRepository = optionRepository,
    criteriaRepository = criteriaRepository,
    userScoreRepository = userScoreRepository
)

val app: RoutingHttpHandler = routes(
    "/ping" bind GET to {
        Response(OK).body("pong")
    },
    "/assets" bind static(ResourceLoader.Classpath("public")),
    decisionUiRoutes.routes
)

fun main() {
    val app: HttpHandler = PrintRequest()
        .then(app)

    val server = app.asServer(Undertow(9000)).start()

    log.info("Server started. UI available at http://localhost:${server.port()}")
}
