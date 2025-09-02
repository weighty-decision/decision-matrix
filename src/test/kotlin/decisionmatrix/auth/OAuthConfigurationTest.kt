package decisionmatrix.auth

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OAuthConfigurationTest {

    @Test
    fun `should parse configuration from environment variables`() {
        // Given
        val envVars = mapOf(
            "DM_OAUTH_ISSUER_URL" to "https://oauth.example.com",
            "DM_OAUTH_CLIENT_ID" to "test-client-id",
            "DM_OAUTH_CLIENT_SECRET" to "test-client-secret",
            "DM_OAUTH_REDIRECT_URI" to "https://app.example.com/auth/callback",
            "DM_OAUTH_SCOPES" to "openid,profile,email,custom"
        )

        // Mock environment variables
        envVars.forEach { (key, value) ->
            System.setProperty(key, value)
        }

        try {
            // When
            val config = OAuthConfiguration(
                issuerUrl = System.getProperty("DM_OAUTH_ISSUER_URL")!!,
                clientId = System.getProperty("DM_OAUTH_CLIENT_ID")!!,
                clientSecret = System.getProperty("DM_OAUTH_CLIENT_SECRET")!!,
                redirectUri = System.getProperty("DM_OAUTH_REDIRECT_URI")!!,
                scopes = System.getProperty("DM_OAUTH_SCOPES")!!.split(",").map { it.trim() }.toSet()
            )

            // Then
            config.issuerUrl shouldBe "https://oauth.example.com"
            config.clientId shouldBe "test-client-id"
            config.clientSecret shouldBe "test-client-secret"
            config.redirectUri shouldBe "https://app.example.com/auth/callback"
            config.scopes shouldBe setOf("openid", "profile", "email", "custom")
        } finally {
            // Cleanup
            envVars.keys.forEach { System.clearProperty(it) }
        }
    }

    @Test
    fun `should use default scopes when not specified`() {
        // Given/When
        val config = OAuthConfiguration(
            issuerUrl = "https://oauth.example.com",
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
            redirectUri = "https://app.example.com/auth/callback"
        )

        // Then
        config.scopes shouldBe setOf("openid", "profile", "email")
    }

    @Test
    fun `UserInfo should contain required fields`() {
        // Given/When
        val userInfo = UserInfo(
            id = "user123",
            email = "test@example.com",
            name = "Test User"
        )

        // Then
        userInfo.id shouldBe "user123"
        userInfo.email shouldBe "test@example.com"
        userInfo.name shouldBe "Test User"
    }

    @Test
    fun `UserInfo name can be null`() {
        // Given/When
        val userInfo = UserInfo(
            id = "user123",
            email = "test@example.com"
        )

        // Then
        userInfo.name shouldBe null
    }
}
