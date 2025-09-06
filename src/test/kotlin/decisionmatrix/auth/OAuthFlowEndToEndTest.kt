package decisionmatrix.auth

import decisionmatrix.oauth.MockOAuthServer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OAuthFlowEndToEndTest {

    private lateinit var mockOAuthServer: MockOAuthServer
    private lateinit var sessionManager: SessionManager
    private lateinit var oauthService: StandardsBasedOAuthService
    private lateinit var authRoutes: AuthRoutes
    private lateinit var testApp: HttpHandler

    @BeforeEach
    fun setUp() {
        // Start mock OAuth server
        mockOAuthServer = MockOAuthServer(8081).start()

        // Set up OAuth configuration
        val oauthConfig = OAuthConfiguration(
            issuerUrl = "http://localhost:8081",
            clientId = "test-client",
            clientSecret = "test-secret",
            redirectUri = "http://localhost:9000/auth/callback",
            scopes = setOf("openid", "profile", "email")
        )

        // Create services
        sessionManager = SessionManager()
        oauthService = StandardsBasedOAuthService(oauthConfig)
        authRoutes = AuthRoutes(oauthService, sessionManager)

        // Create test app with auth filter
        val routes = routes(
            "/ping" bind GET to { Response(OK).body("pong") },
            "/assets" bind static(ResourceLoader.Classpath("public")),
            authRoutes.routes,
            "/" bind GET to { Response(OK).body("Welcome home!") }
        )

        testApp = ServerFilters.InitialiseRequestContext(UserContext.contexts)
            .then(requireAuth(sessionManager, devMode = false))
            .then(routes)
    }

    @AfterEach
    fun tearDown() {
        mockOAuthServer.stop()
    }

    @Test
    fun `complete OAuth flow should authenticate user and redirect to home`() {
        // Step 1: Request root page, should redirect to login
        val homeRequest = Request(GET, "/")
        val homeResponse = testApp(homeRequest)

        homeResponse.status shouldBe SEE_OTHER
        val loginUrl = homeResponse.header("Location")!!
        loginUrl shouldContain "/auth/login?redirect="

        // Step 2: Request login page, should redirect to OAuth provider
        val loginRequest = Request(GET, loginUrl)
        val loginResponse = testApp(loginRequest)

        loginResponse.status shouldBe OK
        val loginPageHtml = loginResponse.bodyString()
        loginPageHtml shouldContain "Sign in with OAuth Provider"

        // Extract OAuth authorization URL from the login page
        val authUrlMatch = Regex("""href="([^"]+)"""").find(loginPageHtml)!!
        val authUrl = authUrlMatch.groupValues[1]

        // Step 3: Follow OAuth authorization URL (this would normally go to the provider)
        // Parse the authorization URL to get parameters manually since http4k Uri parsing has issues
        val queryString = authUrl.split("?", limit = 2)[1]
        val queryParams = queryString.split("&").associate { param ->
            val (key, value) = param.split("=", limit = 2)
            key to value
        }
        val clientId = queryParams["client_id"]!!
        val redirectUri = "http://localhost:9000/auth/callback"
        val state = queryParams["state"]!!

        // Step 4: Simulate user clicking "Login as Alice" on the mock OAuth server
        // This creates an authorization code
        val loginFormData =
            "user_id=user1&client_id=$clientId&redirect_uri=${java.net.URLEncoder.encode(redirectUri, "UTF-8")}&state=$state"

        val mockServerLoginRequest = Request(POST, "http://localhost:8081/auth/login")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(loginFormData)

        // Make direct call to mock OAuth server (simulating browser redirect)
        val mockLoginResponse = mockOAuthServer.routes(mockServerLoginRequest)

        // OAuth server might return either 302 (FOUND) or 303 (SEE_OTHER) for redirects
        (mockLoginResponse.status == SEE_OTHER || mockLoginResponse.status == Status.FOUND) shouldBe true
        val callbackUrl = mockLoginResponse.header("Location")!!

        // Step 5: Extract the callback request and send it to our app
        val callbackUri = Uri.of(callbackUrl)
        val callbackPath = callbackUri.path + "?" + callbackUri.query
        val callbackRequest = Request(GET, callbackPath)
        val callbackResponse = testApp(callbackRequest)


        // Step 6: Should redirect to home page with session cookie
        callbackResponse.status shouldBe SEE_OTHER
        val finalRedirectUrl = callbackResponse.header("Location")!!
        finalRedirectUrl shouldBe "/"

        // Extract session cookie from Set-Cookie header
        val setCookieHeader = callbackResponse.header("Set-Cookie")
        setCookieHeader shouldNotBe null
        val sessionCookieValue = setCookieHeader!!.split(";")[0].split("=")[1]

        // Step 7: Request home page with session cookie, should now work
        val authenticatedHomeRequest = Request(GET, "/")
            .header("Cookie", "DM_SESSION=$sessionCookieValue")
        val authenticatedHomeResponse = testApp(authenticatedHomeRequest)


        authenticatedHomeResponse.status shouldBe OK
        authenticatedHomeResponse.bodyString() shouldBe "Welcome home!"
    }

    @Test
    fun `unauthenticated request should redirect to login page`() {
        val request = Request(GET, "/")
        val response = testApp(request)

        response.status shouldBe SEE_OTHER
        val redirectUrl = response.header("Location")!!
        redirectUrl shouldContain "/auth/login"
    }
}
