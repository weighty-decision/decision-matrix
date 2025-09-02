package decisionmatrix.auth

import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.with
import org.http4k.lens.RequestContextKey
import org.http4k.lens.RequestContextLens

data class AuthenticatedUser(
    val id: String,
    val email: String,
    val name: String? = null
)

object UserContext {
    val contexts = RequestContexts()
    private val userLens: RequestContextLens<AuthenticatedUser> = RequestContextKey.required(contexts)
    
    fun authenticated(user: AuthenticatedUser): (Request) -> Request = { request ->
        request.with(userLens of user)
    }
    
    fun current(request: Request): AuthenticatedUser? = try {
        userLens(request)
    } catch (e: Exception) {
        null
    }
    
    fun requireCurrent(request: Request): AuthenticatedUser = 
        current(request) ?: throw IllegalStateException("No authenticated user found in request context")
}