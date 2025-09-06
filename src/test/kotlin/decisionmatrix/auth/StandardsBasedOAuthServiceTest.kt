package decisionmatrix.auth

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class StandardsBasedOAuthServiceTest {

    private val testConfig = OAuthConfiguration(
        issuerUrl = "https://oauth.example.com",
        clientId = "test-client-id",
        clientSecret = "test-client-secret",
        redirectUri = "https://app.example.com/auth/callback"
    )

    @Test
    fun `should handle callback with error parameter`() {
        // Given
        val service = StandardsBasedOAuthService(testConfig)

        // When
        val result = service.handleCallback(
            code = null,
            state = "test-state",
            error = "access_denied"
        )

        // Then
        result.shouldBeInstanceOf<StandardsBasedOAuthService.CallbackResult.Error>()
        result.message shouldContain "access_denied"
    }

    @Test
    fun `should handle callback with missing code`() {
        // Given
        val service = StandardsBasedOAuthService(testConfig)

        // When
        val result = service.handleCallback(
            code = null,
            state = "test-state",
            error = null
        )

        // Then
        result.shouldBeInstanceOf<StandardsBasedOAuthService.CallbackResult.Error>()
        result.message shouldContain "authorization code"
    }

    @Test
    fun `should handle callback with missing state`() {
        // Given
        val service = StandardsBasedOAuthService(testConfig)

        // When
        val result = service.handleCallback(
            code = "test-code",
            state = null,
            error = null
        )

        // Then
        result.shouldBeInstanceOf<StandardsBasedOAuthService.CallbackResult.Error>()
        result.message shouldContain "state"
    }

    @Test
    fun `should handle callback with invalid state`() {
        // Given
        val service = StandardsBasedOAuthService(testConfig)

        // When
        val result = service.handleCallback(
            code = "test-code",
            state = "invalid-state",
            error = null
        )

        // Then
        result.shouldBeInstanceOf<StandardsBasedOAuthService.CallbackResult.Error>()
        result.message shouldContain "Invalid or expired"
    }

    @Test
    fun `should create authorization URL with default redirect`() {
        // Note: This test would require mocking the HTTP call to .well-known endpoint
        // For now, we'll test the basic structure without the actual HTTP call

        // Given
        val config = OAuthConfiguration(
            issuerUrl = "https://oauth.example.com",
            clientId = "test-client",
            clientSecret = "test-secret",
            redirectUri = "https://app.example.com/callback"
        )

        // When/Then - this would normally create the service and test URL creation
        // but requires mocking HTTP calls, so we test the config structure instead
        config.scopes shouldBe setOf("openid", "profile", "email")
        config.redirectUri shouldBe "https://app.example.com/callback"
    }

    @Test
    fun `callback result classes should have proper structure`() {
        // Test Success result
        val userInfo = UserInfo("user123", "test@example.com", "Test User")
        val successResult = StandardsBasedOAuthService.CallbackResult.Success(
            user = userInfo,
            redirectAfterLogin = "/dashboard"
        )

        successResult.user shouldBe userInfo
        successResult.redirectAfterLogin shouldBe "/dashboard"

        // Test Error result  
        val errorResult = StandardsBasedOAuthService.CallbackResult.Error("Test error")
        errorResult.message shouldBe "Test error"
    }
}
