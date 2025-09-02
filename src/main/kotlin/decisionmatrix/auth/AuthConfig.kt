package decisionmatrix.auth

data class AuthConfiguration(
    val devMode: Boolean = false,
    val devUserId: String? = null,
    val oauthProvider: String = "google",
    val oauthConfig: OAuthConfig? = null
) {
    companion object {
        fun fromEnvironment(): AuthConfiguration {
            val devMode = System.getenv("DM_DEV_MODE")?.toBoolean() ?: false
            val devUserId = System.getenv("DM_DEV_USER_ID")
            
            val oauthConfig = if (!devMode) {
                val clientId = System.getenv("DM_OAUTH_CLIENT_ID") 
                    ?: throw IllegalStateException("DM_OAUTH_CLIENT_ID environment variable is required when not in dev mode")
                val clientSecret = System.getenv("DM_OAUTH_CLIENT_SECRET") 
                    ?: throw IllegalStateException("DM_OAUTH_CLIENT_SECRET environment variable is required when not in dev mode")
                val redirectUri = System.getenv("DM_OAUTH_REDIRECT_URI") 
                    ?: "http://localhost:9000/auth/callback"
                
                OAuthConfig(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    redirectUri = redirectUri
                )
            } else {
                null
            }
            
            return AuthConfiguration(
                devMode = devMode,
                devUserId = devUserId,
                oauthProvider = System.getenv("DM_OAUTH_PROVIDER") ?: "google",
                oauthConfig = oauthConfig
            )
        }
    }
}