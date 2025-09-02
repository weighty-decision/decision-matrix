package decisionmatrix.auth

data class AuthConfiguration(
    val devMode: Boolean = false,
    val devUserId: String? = null
) {
    companion object {
        fun fromEnvironment(): AuthConfiguration {
            return AuthConfiguration(
                devMode = System.getenv("DM_DEV_MODE")?.toBoolean() ?: false,
                devUserId = System.getenv("DM_DEV_USER_ID")
            )
        }
    }
}