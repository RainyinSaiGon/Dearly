package com.dearly.app.service

import com.dearly.app.data.remote.DearlyApi
import com.dearly.app.data.remote.FcmTokenRequest
import com.dearly.app.data.session.TokenStore
import com.google.firebase.messaging.FirebaseMessagingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DearlyMessagingService : FirebaseMessagingService() {
    @Inject lateinit var api: DearlyApi
    @Inject lateinit var tokenStore: TokenStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        if (tokenStore.accessToken() != null) {
            scope.launch { runCatching { api.registerFcmToken(FcmTokenRequest(token)) } }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
