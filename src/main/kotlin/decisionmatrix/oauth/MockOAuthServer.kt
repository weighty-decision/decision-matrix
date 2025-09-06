package decisionmatrix.oauth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Undertow
import org.http4k.server.asServer
import java.time.Instant
import java.util.*

@Serializable
data class OpenIdConfiguration(
    val issuer: String,
    val authorization_endpoint: String,
    val token_endpoint: String,
    val jwks_uri: String,
    val response_types_supported: List<String> = listOf("code"),
    val subject_types_supported: List<String> = listOf("public"),
    val id_token_signing_alg_values_supported: List<String> = listOf("RS256")
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String = "Bearer",
    val expires_in: Int = 3600,
    val id_token: String
)

data class TestUser(
    val id: String,
    val email: String,
    val name: String
)

private const val MOCK_OAUTH_SERVER_PORT = 8081

class MockOAuthServer(private val port: Int = MOCK_OAUTH_SERVER_PORT) {
    private val baseUrl = "http://localhost:$port"
    private val rsaKey: RSAKey = RSAKeyGenerator(2048)
        .keyID("test-key")
        .generate()
    private val jwkSet = JWKSet(rsaKey.toPublicJWK())
    private val signer = RSASSASigner(rsaKey)

    private val testUsers = listOf(
        TestUser("user1", "alice@example.com", "Alice Test"),
        TestUser("user2", "bob@example.com", "Bob Test"),
        TestUser("admin", "admin@example.com", "Admin User")
    )

    private val authCodes = mutableMapOf<String, String>() // code -> userId

    private fun extractClientId(request: Request, params: Map<String, String>): String? {
        // Try Basic auth first (Authorization header)
        val authHeader = request.header("Authorization")
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            val encoded = authHeader.substring("Basic ".length)
            val decoded = String(Base64.getDecoder().decode(encoded))
            val (clientId, _) = decoded.split(":", limit = 2)
            return clientId
        }

        // Fall back to form parameters
        return params["client_id"]
    }

    val routes = routes(
        "/.well-known/openid-configuration" bind GET to { _ ->
            val config = OpenIdConfiguration(
                issuer = baseUrl,
                authorization_endpoint = "$baseUrl/auth",
                token_endpoint = "$baseUrl/token",
                jwks_uri = "$baseUrl/.well-known/jwks.json",
                response_types_supported = listOf("code"),
                subject_types_supported = listOf("public"),
                id_token_signing_alg_values_supported = listOf("RS256")
            )
            Response(OK).body(Json { encodeDefaults = true }.encodeToString(config)).header("Content-Type", "application/json")
        },

        "/.well-known/jwks.json" bind GET to { _ ->
            Response(OK).body(jwkSet.toString()).header("Content-Type", "application/json")
        },

        "/auth" bind GET to { request ->
            val clientId = request.query("client_id")
            val redirectUri = request.query("redirect_uri")
            val state = request.query("state")

            val loginPage = """
                <!DOCTYPE html>
                <html>
                <head><title>Mock OAuth Login</title></head>
                <body>
                    <h2>Mock OAuth Server - Select Test User</h2>
                    <form action="/auth/login" method="post">
                        <input type="hidden" name="client_id" value="$clientId">
                        <input type="hidden" name="redirect_uri" value="$redirectUri">
                        <input type="hidden" name="state" value="$state">
                        ${
                testUsers.joinToString("") { user ->
                    """
                            <div style="margin: 10px;">
                                <button type="submit" name="user_id" value="${user.id}" 
                                        style="padding: 10px; margin: 5px; display: block; width: 200px;">
                                    ${user.name} (${user.email})
                                </button>
                            </div>
                            """
                }
            }
                    </form>
                </body>
                </html>
            """.trimIndent()

            Response(OK).body(loginPage).header("Content-Type", "text/html")
        },

        "/auth/login" bind POST to { request ->
            val bodyString = request.bodyString()
            val params = bodyString.split("&").associate {
                val (key, value) = it.split("=", limit = 2)
                key to java.net.URLDecoder.decode(value, "UTF-8")
            }
            val userId = params["user_id"]
            val clientId = params["client_id"]
            val redirectUri = params["redirect_uri"]
            val state = params["state"]

            if (userId != null && redirectUri != null) {
                val authCode = UUID.randomUUID().toString()
                authCodes[authCode] = userId

                val redirectUrl = buildString {
                    append(redirectUri)
                    append("?code=$authCode")
                    if (state != null) append("&state=$state")
                }

                Response(FOUND).header("Location", redirectUrl)
            } else {
                Response(Status.BAD_REQUEST).body("Missing required parameters")
            }
        },

        "/token" bind POST to { request ->
            val bodyString = request.bodyString()
            val params = bodyString.split("&").associate {
                val (key, value) = it.split("=", limit = 2)
                key to java.net.URLDecoder.decode(value, "UTF-8")
            }
            val code = params["code"]

            // Handle client authentication - either from Authorization header (Basic auth) or form params
            val clientId = extractClientId(request, params)

            if (code != null && clientId != null && authCodes.containsKey(code)) {
                val userId = authCodes.remove(code)!!
                val user = testUsers.first { it.id == userId }

                val now = Instant.now()
                val accessToken = UUID.randomUUID().toString()

                val idTokenClaims = JWTClaimsSet.Builder()
                    .issuer(baseUrl)
                    .subject(user.id)
                    .audience(listOf(clientId!!))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .issueTime(Date.from(now))
                    .claim("email", user.email)
                    .claim("name", user.name)
                    .build()

                val signedJWT = SignedJWT(
                    JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).build(),
                    idTokenClaims
                )
                signedJWT.sign(signer)

                val tokenResponse = TokenResponse(
                    access_token = accessToken,
                    id_token = signedJWT.serialize()
                )

                Response(OK).body(Json { encodeDefaults = true }.encodeToString(tokenResponse))
                    .header("Content-Type", "application/json")
            } else {
                Response(Status.BAD_REQUEST).body("Invalid authorization code")
            }
        }
    )

    private var server: Http4kServer? = null

    fun start(): MockOAuthServer {
        server = routes.asServer(Undertow(port)).start()
        println("Mock OAuth server started on $baseUrl")
        println("Test users available:")
        testUsers.forEach { user ->
            println("  - ${user.name} (${user.email}) [ID: ${user.id}]")
        }
        return this
    }

    fun stop() {
        server?.stop()
        println("Mock OAuth server stopped")
    }

    fun getIssuerUrl(): String = baseUrl
}
