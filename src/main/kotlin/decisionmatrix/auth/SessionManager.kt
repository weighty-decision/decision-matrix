package decisionmatrix.auth

import kotlinx.serialization.Serializable
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.replaceCookie
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class SessionData(
    val userId: String,
    val userEmail: String,
    val userName: String? = null
)

class SessionManager(
    private val sessionCookieName: String = "DM_SESSION",
    private val sessionTimeoutMinutes: Long = 24 * 60 // 24 hours
) {
    private val sessions = ConcurrentHashMap<String, SessionData>()
    private val sessionExpiry = ConcurrentHashMap<String, Long>()

    fun createSession(user: AuthenticatedUser): String {
        val sessionId = UUID.randomUUID().toString()
        val sessionData = SessionData(
            userId = user.id,
            userEmail = user.email,
            userName = user.name
        )

        sessions[sessionId] = sessionData
        sessionExpiry[sessionId] = System.currentTimeMillis() + (sessionTimeoutMinutes * 60 * 1000)

        return sessionId
    }

    fun getSession(sessionId: String): AuthenticatedUser? {
        val expiry = sessionExpiry[sessionId] ?: return null
        if (System.currentTimeMillis() > expiry) {
            removeSession(sessionId)
            return null
        }

        val sessionData = sessions[sessionId] ?: return null
        return AuthenticatedUser(
            id = sessionData.userId,
            email = sessionData.userEmail,
            name = sessionData.userName
        )
    }

    fun removeSession(sessionId: String) {
        sessions.remove(sessionId)
        sessionExpiry.remove(sessionId)
    }

    fun getSessionIdFromRequest(request: Request): String? =
        request.cookie(sessionCookieName)?.value

    fun addSessionCookie(response: Response, sessionId: String): Response =
        response.replaceCookie(
            Cookie(
                name = sessionCookieName,
                value = sessionId,
                httpOnly = true,
                secure = false, // Will be true in production
                sameSite = SameSite.Lax,
                maxAge = sessionTimeoutMinutes * 60
            )
        )

    fun removeSessionCookie(response: Response): Response =
        response.replaceCookie(
            Cookie(
                name = sessionCookieName,
                value = "",
                httpOnly = true,
                maxAge = 0
            )
        )
}
