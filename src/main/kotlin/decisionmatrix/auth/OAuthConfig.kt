package decisionmatrix.auth

data class OAuthConfiguration(
    val issuerUrl: String,
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val scopes: Set<String> = setOf("openid", "profile", "email")
) {
    companion object {
        fun fromEnvironment(): OAuthConfiguration {
            val issuerUrl = requireNotNull(System.getenv("DM_OAUTH_ISSUER_URL")) {
                "DM_OAUTH_ISSUER_URL environment variable is required"
            }
            val clientId = requireNotNull(System.getenv("DM_OAUTH_CLIENT_ID")) {
                "DM_OAUTH_CLIENT_ID environment variable is required"
            }
            val clientSecret = requireNotNull(System.getenv("DM_OAUTH_CLIENT_SECRET")) {
                "DM_OAUTH_CLIENT_SECRET environment variable is required"
            }
            val redirectUri = requireNotNull(System.getenv("DM_OAUTH_REDIRECT_URI")) {
                "DM_OAUTH_REDIRECT_URI environment variable is required"
            }
            
            val scopes = System.getenv("DM_OAUTH_SCOPES")
                ?.split(",")
                ?.map { it.trim() }
                ?.toSet()
                ?: setOf("openid", "profile", "email")

            return OAuthConfiguration(
                issuerUrl = issuerUrl,
                clientId = clientId,
                clientSecret = clientSecret,
                redirectUri = redirectUri,
                scopes = scopes
            )
        }
    }
}

data class UserInfo(
    val id: String,
    val email: String,
    val name: String? = null
)

interface OAuthServiceInterface {
    fun createAuthorizationUrl(redirectAfterLogin: String = "/"): String
    fun handleCallback(code: String?, state: String?, error: String?): StandardsBasedOAuthService.CallbackResult
}