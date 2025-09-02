package decisionmatrix.auth.providers

import decisionmatrix.auth.OAuthConfig
import decisionmatrix.auth.OAuthProvider
import decisionmatrix.auth.UserInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.body.form
import org.slf4j.LoggerFactory
import java.net.URLEncoder

class GoogleOAuthProvider : OAuthProvider {
    override val name: String = "Google"

    private val client = OkHttp()
    private val json = Json { ignoreUnknownKeys = true }
    private val log = LoggerFactory.getLogger(GoogleOAuthProvider::class.java)

    private companion object {
        const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        const val USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo"
    }

    override fun authorizationUrl(config: OAuthConfig, state: String?): String {
        val params = mutableMapOf(
            "client_id" to config.clientId,
            "redirect_uri" to config.redirectUri,
            "response_type" to "code",
            "scope" to "openid email profile"
        )

        if (state != null) {
            params["state"] = state
        }

        val queryString = params.map { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }.joinToString("&")

        return "$AUTH_URL?$queryString"
    }

    override fun exchangeCodeForToken(config: OAuthConfig, code: String): String {
        val request = Request(Method.POST, TOKEN_URL)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .form("client_id", config.clientId)
            .form("client_secret", config.clientSecret)
            .form("code", code)
            .form("grant_type", "authorization_code")
            .form("redirect_uri", config.redirectUri)

        val response = client(request)

        if (response.status != Status.OK) {
            val error = response.bodyString()
            log.error("Token exchange failed: {} - {}", response.status, error)
            throw RuntimeException("Failed to exchange code for token: ${response.status}")
        }

        val tokenData = json.parseToJsonElement(response.bodyString()).jsonObject
        return tokenData["access_token"]?.jsonPrimitive?.content
            ?: throw RuntimeException("No access token in response")
    }

    override fun getUserInfo(accessToken: String): UserInfo {
        val request = Request(Method.GET, USER_INFO_URL)
            .header("Authorization", "Bearer $accessToken")

        val response = client(request)

        if (response.status != Status.OK) {
            val error = response.bodyString()
            log.error("User info request failed: {} - {}", response.status, error)
            throw RuntimeException("Failed to get user info: ${response.status}")
        }

        val userData = json.parseToJsonElement(response.bodyString()).jsonObject

        return UserInfo(
            id = userData["id"]?.jsonPrimitive?.content
                ?: throw RuntimeException("No user ID in response"),
            email = userData["email"]?.jsonPrimitive?.content
                ?: throw RuntimeException("No email in response"),
            name = userData["name"]?.jsonPrimitive?.content
        )
    }
}
