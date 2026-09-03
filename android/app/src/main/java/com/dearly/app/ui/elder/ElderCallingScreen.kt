package com.dearly.app.ui.elder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dearly.app.ui.components.VoiceCaptureButton
import com.dearly.app.ui.components.VoiceCaptureState
import com.dearly.app.data.remote.VoicePersonalizationDto
import java.io.File

private val CallingForest = Color(0xFF174D3B)

@Composable
fun ElderCallingScreen(
    busy: Boolean = false,
    transcript: String? = null,
    response: String? = null,
    personalization: VoicePersonalizationDto? = null,
    error: String? = null,
    requiresVerification: Boolean = false,
    onVoiceAudio: (File) -> Unit = {},
    onVerificationAudio: (File) -> Unit = {},
    onVoiceCaptureStarted: () -> Unit = {},
    onOpenMedications: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var captureState by remember { mutableStateOf(VoiceCaptureState.IDLE) }
    var captureError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(busy, transcript, response, error) {
        if (
            !busy && captureState in setOf(
                VoiceCaptureState.PROCESSING,
                VoiceCaptureState.SPEECH_PROCESSING
            )
        ) {
            captureState = VoiceCaptureState.IDLE
        }
    }

    val heading = when (captureState) {
        VoiceCaptureState.LISTENING -> "Dearly đang nghe..."
        VoiceCaptureState.SPEECH_DETECTED -> "Dearly đã nghe thấy bác"
        VoiceCaptureState.PROCESSING -> "Đang xử lý bản ghi âm..."
        VoiceCaptureState.SPEECH_PROCESSING -> "Đã nghe thấy, đang xử lý..."
        VoiceCaptureState.IDLE -> when {
            busy -> "Dearly đang xử lý..."
            requiresVerification -> "Xác nhận uống thuốc"
            else -> "Bác muốn làm gì?"
        }
    }
    val instruction = when (captureState) {
        VoiceCaptureState.LISTENING -> "Bác hãy nói vào micro"
        VoiceCaptureState.SPEECH_DETECTED -> "Đã nhận được giọng nói. Nhấn dừng để gửi."
        VoiceCaptureState.PROCESSING -> "Chưa phát hiện rõ giọng nói; Dearly đang kiểm tra bản ghi"
        VoiceCaptureState.SPEECH_PROCESSING -> "Đã nghe thấy giọng nói và đang phân tích yêu cầu"
        VoiceCaptureState.IDLE -> if (requiresVerification) {
            "Bác nói ‘Đúng rồi’ để xác nhận, rồi Dearly sẽ kiểm tra giọng nói."
        } else {
            "Nói “Tôi đã uống Amlodipine rồi” hoặc “Hôm nay uống thuốc gì?”"
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCF9))) {
        ElderHeader()
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                heading,
                color = CallingForest,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                instruction,
                color = Color(0xFF64736E),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            VoiceCaptureButton(
                onAudioReady = { audio ->
                    if (requiresVerification) onVerificationAudio(audio) else onVoiceAudio(audio)
                },
                enabled = !busy,
                modifier = Modifier.width(220.dp).padding(top = 28.dp),
                idleLabel = when {
                    captureError != null || error != null -> "Thử lại"
                    requiresVerification -> "Nói: Đúng rồi"
                    else -> "Nhấn để nói"
                },
                recordingLabel = if (captureState == VoiceCaptureState.SPEECH_DETECTED) {
                    "Dừng và gửi"
                } else {
                    "Dừng ghi âm"
                },
                onStateChanged = { state ->
                    captureState = state
                    if (state == VoiceCaptureState.LISTENING) captureError = null
                    if (state == VoiceCaptureState.LISTENING && !requiresVerification) {
                        onVoiceCaptureStarted()
                    }
                },
                onError = { captureError = it }
            )
            when (captureState) {
                VoiceCaptureState.LISTENING -> Text(
                    "Đang chờ giọng nói...",
                    color = Color(0xFF64736E),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 14.dp)
                )
                VoiceCaptureState.SPEECH_DETECTED -> Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = CallingForest
                    )
                    Text("Đã nghe thấy giọng nói", color = CallingForest, fontWeight = FontWeight.SemiBold)
                }
                VoiceCaptureState.PROCESSING,
                VoiceCaptureState.SPEECH_PROCESSING -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp),
                        strokeWidth = 2.dp,
                        color = CallingForest
                    )
                    Text(
                        if (captureState == VoiceCaptureState.SPEECH_PROCESSING) {
                            "Đã nghe thấy • Đang chuyển thành văn bản..."
                        } else {
                            "Đang kiểm tra bản ghi âm..."
                        },
                        color = Color(0xFF64736E),
                        fontSize = 14.sp
                    )
                }
                VoiceCaptureState.IDLE -> Unit
            }
            transcript?.let {
                Text(
                    "Bác nói: “$it”",
                    color = Color(0xFF66736F),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
            response?.let {
                Text(
                    it,
                    color = CallingForest,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            personalization?.let { preferences ->
                val style = if (preferences.reminderStyle == "DIRECT") "nhắc việc rõ ràng" else "nhắc nhẹ nhàng"
                val contact = preferences.preferredContactName?.let { ", ưu tiên liên hệ $it" }.orEmpty()
                Text(
                    "Cá nhân hoá theo giọng nói: $style$contact.",
                    color = Color(0xFF66736F),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            error?.let {
                Text(
                    it,
                    color = Color(0xFFC62828),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            Spacer(Modifier.padding(bottom = 20.dp))
        }
        ElderFooter(ElderTab.CALLS, {}, onOpenMedications, onOpenSettings)
    }
}
