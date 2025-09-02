package decisionmatrix.auth.providers

import decisionmatrix.auth.OAuthConfig
import decisionmatrix.auth.OAuthProvider
import decisionmatrix.auth.UserInfo

class MockOAuthProvider(
    private val mockUserInfo: UserInfo = UserInfo(
        id = "test-user-123",
        email = "test@example.com", 
        name = "Test User"
    )
) : OAuthProvider {
    
    override val name: String = "Mock"
    
    override fun authorizationUrl(config: OAuthConfig, state: String?): String {
        // Return a mock URL that can be used in tests
        return "http://mock-oauth.test/auth?client_id=${config.clientId}&state=$state"
    }
    
    override fun exchangeCodeForToken(config: OAuthConfig, code: String): String {
        // In tests, any code will work
        return "mock-access-token-$code"
    }
    
    override fun getUserInfo(accessToken: String): UserInfo {
        return mockUserInfo
    }
    
    companion object {
        fun withUser(id: String, email: String, name: String? = null): MockOAuthProvider {
            return MockOAuthProvider(UserInfo(id, email, name))
        }
    }
}