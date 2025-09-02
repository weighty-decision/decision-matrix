package decisionmatrix.auth

class MockOAuthService : OAuthServiceInterface {
    var mockUser: UserInfo = UserInfo(
        id = "mock-user-123",
        email = "mock@example.com", 
        name = "Mock User"
    )
    
    override fun createAuthorizationUrl(redirectAfterLogin: String): String {
        return "https://mock.oauth.provider/authorize?client_id=mock-client-id&redirect_uri=http%3A%2F%2Flocalhost%3A9000%2Fauth%2Fcallback&response_type=code&scope=openid%20profile%20email&state=mock-state"
    }
    
    override fun handleCallback(code: String?, state: String?, error: String?): StandardsBasedOAuthService.CallbackResult {
        if (error != null) {
            return StandardsBasedOAuthService.CallbackResult.Error("Mock error: $error")
        }
        
        if (code == null || state == null) {
            return StandardsBasedOAuthService.CallbackResult.Error("Missing code or state")
        }
        
        if (code == "invalid-code") {
            return StandardsBasedOAuthService.CallbackResult.Error("Invalid authorization code")
        }
        
        return StandardsBasedOAuthService.CallbackResult.Success(
            user = mockUser,
            redirectAfterLogin = "/"
        )
    }
}