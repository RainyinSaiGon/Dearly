package com.dearly.app.ui.elder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dearly.app.ui.components.VoiceCaptureButton
import java.io.File

private val CallingForest = Color(0xFF174D3B)

@Composable
fun ElderCallingScreen(
    busy: Boolean = false,
    transcript: String? = null,
    response: String? = null,
    error: String? = null,
    onVoiceAudio: (File) -> Unit = {},
    onOpenMedications: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCF9))) {
        ElderHeader()
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (busy) "Dearly đang xử lý" else "Bác muốn làm gì?",
                color = CallingForest,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Nói “Gọi cho...” hoặc “Hôm nay uống thuốc gì?”",
                color = Color(0xFF64736E),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            VoiceCaptureButton(
                onAudioReady = onVoiceAudio,
                enabled = !busy,
                modifier = Modifier.width(220.dp).padding(top = 28.dp),
                idleLabel = "Nhấn để nói"
            )
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
