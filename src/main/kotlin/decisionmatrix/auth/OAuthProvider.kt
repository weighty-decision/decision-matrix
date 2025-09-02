package decisionmatrix.auth

data class OAuthConfig(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val additionalParams: Map<String, String> = emptyMap()
)

data class UserInfo(
    val id: String,
    val email: String,
    val name: String? = null
)

interface OAuthProvider {
    val name: String

    fun authorizationUrl(config: OAuthConfig, state: String? = null): String

    fun exchangeCodeForToken(config: OAuthConfig, code: String): String

    fun getUserInfo(accessToken: String): UserInfo
}
