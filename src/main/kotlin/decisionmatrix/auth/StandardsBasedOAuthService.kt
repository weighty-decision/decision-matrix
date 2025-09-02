package decisionmatrix.auth
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.pkce.CodeChallenge
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import com.nimbusds.openid.connect.sdk.token.OIDCTokens
import org.slf4j.LoggerFactory
import java.net.URI
import java.security.SecureRandom
import java.util.*

class StandardsBasedOAuthService(private val config: OAuthConfiguration) : OAuthServiceInterface {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureRandom = SecureRandom()
    
    private val providerMetadata: OIDCProviderMetadata by lazy {
        loadProviderMetadata()
    }
    
    private val pendingAuthorizations = mutableMapOf<String, PendingAuthorization>()
    
    data class PendingAuthorization(
        val codeVerifier: CodeVerifier,
        val state: State,
        val redirectAfterLogin: String
    )
    
    private fun loadProviderMetadata(): OIDCProviderMetadata {
        return try {
            val issuerURI = URI.create(config.issuerUrl)
            val httpRequest = HTTPRequest(HTTPRequest.Method.GET, issuerURI.resolve("/.well-known/openid-configuration").toURL())
            val httpResponse = httpRequest.send()
            
            if (httpResponse.statusCode != 200) {
                throw RuntimeException("Failed to fetch provider metadata: HTTP ${httpResponse.statusCode}")
            }
            
            OIDCProviderMetadata.parse(httpResponse.content)
        } catch (e: Exception) {
            log.error("Failed to load OAuth provider metadata for issuer: ${config.issuerUrl}", e)
            throw RuntimeException("Failed to load OAuth provider metadata", e)
        }
    }
    
    override fun createAuthorizationUrl(redirectAfterLogin: String): String {
        val state = State()
        val codeVerifier = CodeVerifier()
        val codeChallenge = CodeChallenge.compute(CodeChallengeMethod.S256, codeVerifier)
        
        // Store pending authorization for callback validation
        pendingAuthorizations[state.value] = PendingAuthorization(
            codeVerifier = codeVerifier,
            state = state,
            redirectAfterLogin = redirectAfterLogin
        )
        
        val authRequest = AuthorizationRequest.Builder(
            ResponseType.CODE,
            ClientID(config.clientId)
        )
            .scope(Scope(*config.scopes.toTypedArray()))
            .state(state)
            .redirectionURI(URI.create(config.redirectUri))
            .codeChallenge(codeChallenge, CodeChallengeMethod.S256)
            .endpointURI(providerMetadata.authorizationEndpointURI)
            .build()
        
        return authRequest.toURI().toString()
    }
    
    override fun handleCallback(code: String?, state: String?, error: String?): CallbackResult {
        if (error != null) {
            log.warn("OAuth error: {}", error)
            return CallbackResult.Error("Authentication failed: $error")
        }
        
        if (code == null || state == null) {
            log.warn("Missing authorization code or state in callback")
            return CallbackResult.Error("Missing authorization code or state")
        }
        
        val pendingAuth = pendingAuthorizations.remove(state)
        if (pendingAuth == null) {
            log.warn("Invalid or expired state parameter: {}", state)
            return CallbackResult.Error("Invalid or expired authentication request")
        }
        
        return try {
            val tokens = exchangeCodeForTokens(code, pendingAuth.codeVerifier)
            val userInfo = extractUserInfo(tokens)
            
            CallbackResult.Success(
                user = userInfo,
                redirectAfterLogin = pendingAuth.redirectAfterLogin
            )
        } catch (e: Exception) {
            log.error("Failed to complete OAuth callback", e)
            CallbackResult.Error("Authentication failed")
        }
    }
    
    private fun exchangeCodeForTokens(code: String, codeVerifier: CodeVerifier): OIDCTokens {
        val tokenRequest = TokenRequest(
            providerMetadata.tokenEndpointURI,
            ClientSecretBasic(ClientID(config.clientId), com.nimbusds.oauth2.sdk.auth.Secret(config.clientSecret)),
            AuthorizationCodeGrant(
                AuthorizationCode(code),
                URI.create(config.redirectUri),
                codeVerifier
            )
        )
        
        val tokenResponse = TokenResponse.parse(tokenRequest.toHTTPRequest().send())
        
        if (!tokenResponse.indicatesSuccess()) {
            val error = tokenResponse.toErrorResponse()
            log.error("Token exchange failed: {} - {}", error.errorObject.code, error.errorObject.description)
            throw RuntimeException("Failed to exchange authorization code for tokens")
        }
        
        val successResponse = tokenResponse.toSuccessResponse() as OIDCTokenResponse
        return successResponse.oidcTokens
    }
    
    private fun extractUserInfo(tokens: OIDCTokens): UserInfo {
        // Extract claims from ID token
        val idToken = tokens.idToken
        if (idToken == null) {
            log.error("No ID token received from provider")
            throw RuntimeException("No ID token received")
        }
        
        val claims = idToken.jwtClaimsSet
        
        val userId = claims.subject
            ?: throw RuntimeException("No subject (user ID) in ID token")
        
        val email = claims.getStringClaim("email")
            ?: throw RuntimeException("No email claim in ID token")
        
        val name = claims.getStringClaim("name")
            ?: claims.getStringClaim("preferred_username")
        
        log.info("Successfully authenticated user: {}", email)
        
        return UserInfo(
            id = userId,
            email = email,
            name = name
        )
    }
    
    sealed class CallbackResult {
        data class Success(val user: UserInfo, val redirectAfterLogin: String) : CallbackResult()
        data class Error(val message: String) : CallbackResult()
    }
}