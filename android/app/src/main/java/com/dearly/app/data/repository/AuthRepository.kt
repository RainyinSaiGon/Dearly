package com.dearly.app.data.repository

import com.dearly.app.data.remote.DearlyApi
import com.dearly.app.data.remote.FcmTokenRequest
import com.dearly.app.data.remote.RefreshRequest
import com.dearly.app.data.remote.SessionRequest
import com.dearly.app.data.remote.UserDto
import com.dearly.app.data.session.TokenStore
import com.dearly.app.domain.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: DearlyApi,
    private val tokenStore: TokenStore
) {
    suspend fun createBackendSession(role: UserRole): UserDto {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
            ?: error("Firebase user is not authenticated")
        val firebaseIdToken = firebaseUser.getIdToken(false).await().token
            ?: error("Firebase did not return an ID token")
        val session = api.createSession(SessionRequest(firebaseIdToken, role.name))
        tokenStore.save(session.accessToken, session.refreshToken, session.user.id, session.user.role)
        runCatching {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            api.registerFcmToken(FcmTokenRequest(fcmToken))
        }
        return session.user
    }

    suspend fun elders(): List<UserDto> = api.elders()

    suspend fun signOut() {
        tokenStore.refreshToken()?.let { token ->
            runCatching { api.logout(RefreshRequest(token)) }
        }
        tokenStore.clear()
        FirebaseAuth.getInstance().signOut()
    }

    fun userId(): String? = tokenStore.userId()
    fun role(): String? = tokenStore.role()
}
