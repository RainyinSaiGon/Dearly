package com.dearly.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dearly.app.R
import com.dearly.app.ui.components.VoiceCaptureButton
import com.dearly.app.ui.components.VoiceCaptureState
import java.io.File

private val PublicAssistantGreen = Color(0xFF1B4332)

@Composable
fun PublicAssistantScreen(
    busy: Boolean,
    transcript: String?,
    response: String?,
    error: String?,
    onAudioReady: (File) -> Unit,
    onVoiceCaptureStarted: () -> Unit,
    onSignIn: () -> Unit
) {
    var captureState by remember { mutableStateOf(VoiceCaptureState.IDLE) }

    LaunchedEffect(busy) {
        if (!busy && captureState in setOf(VoiceCaptureState.PROCESSING, VoiceCaptureState.SPEECH_PROCESSING)) {
            captureState = VoiceCaptureState.IDLE
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9F5)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.ic_dearly_mark),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Dearly", color = PublicAssistantGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSignIn) {
                Text("Đăng nhập", color = PublicAssistantGreen, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Bác cần Dearly giúp gì?",
                color = PublicAssistantGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            VoiceCaptureButton(
                onAudioReady = onAudioReady,
                enabled = !busy,
                idleLabel = "Bấm để nói",
                modifier = Modifier.width(220.dp),
                onStateChanged = { state ->
                    captureState = state
                    if (state == VoiceCaptureState.LISTENING) onVoiceCaptureStarted()
                }
            )
            if (busy || captureState == VoiceCaptureState.PROCESSING || captureState == VoiceCaptureState.SPEECH_PROCESSING) {
                Text("Dearly đang trả lời...", color = Color(0xFF64736E), modifier = Modifier.padding(top = 14.dp))
            }
            transcript?.let {
                Text("Bác nói: “$it”", color = Color(0xFF64736E), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp))
            }
            response?.let {
                Text(it, color = PublicAssistantGreen, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
            }
            error?.let {
                Text(it, color = Color(0xFFC62828), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}
