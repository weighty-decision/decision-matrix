package decisionmatrix.http4k

data class HttpConfig(
    val port: Int
) {
    companion object {
        fun fromEnvironment(): HttpConfig {
            return HttpConfig(
                port = System.getenv("DM_HTTP_SERVER_PORT")?.toInt() ?: 8080,
            )
        }
    }
}
