package com.dearly.app.data.session

import com.dearly.app.BuildConfig
import com.dearly.app.data.remote.RefreshRequest
import com.dearly.app.data.remote.SessionDto
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val gson: Gson
) : Authenticator {
    private val refreshClient = OkHttpClient()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2 || response.request.url.encodedPath.endsWith("/auth/refresh")) {
            return null
        }
        return synchronized(this) {
            val tokenUsed = response.request.header("Authorization")?.removePrefix("Bearer ")
            val latestToken = tokenStore.accessToken()
            if (!latestToken.isNullOrBlank() && latestToken != tokenUsed) {
                return@synchronized response.request.newBuilder()
                    .header("Authorization", "Bearer $latestToken")
                    .build()
            }
            val refreshToken = tokenStore.refreshToken() ?: return@synchronized null
            val body = gson.toJson(RefreshRequest(refreshToken))
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BuildConfig.API_BASE_URL + "auth/refresh")
                .post(body)
                .build()
            refreshClient.newCall(request).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) {
                    tokenStore.clear()
                    return@synchronized null
                }
                val session = gson.fromJson(refreshResponse.body?.charStream(), SessionDto::class.java)
                    ?: return@synchronized null
                tokenStore.save(session.accessToken, session.refreshToken, session.user.id, session.user.role)
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${session.accessToken}")
                    .build()
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var previous = response.priorResponse
        while (previous != null) {
            count++
            previous = previous.priorResponse
        }
        return count
    }
}
