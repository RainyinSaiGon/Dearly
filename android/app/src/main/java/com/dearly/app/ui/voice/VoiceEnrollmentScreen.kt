package com.dearly.app.ui.voice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ENROLLMENT_PHRASES = listOf(
    "Xin chào Dearly, tôi tên là An",
    "Hôm nay tôi muốn xem lịch uống thuốc",
    "Hãy giúp tôi gọi điện cho con gái",
    "Dearly ơi, bây giờ là mấy giờ rồi",
    "Cảm ơn trợ lý ảo Dearly"
)

@Composable
fun VoiceEnrollmentScreen(
    onEnrollmentComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Progress
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Thu Âm Mẫu Giọng Nói",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Bước ${currentStep + 1} / ${ENROLLMENT_PHRASES.size}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (currentStep + 1) / 5.0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Phrase Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\"${ENROLLMENT_PHRASES[currentStep]}\"",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 26.sp,
                        lineHeight = 36.sp
                    )
                }
            }

            // Record Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = {
                        if (!isRecording) {
                            isRecording = true
                        } else {
                            isRecording = false
                            if (currentStep < 4) {
                                currentStep++
                            } else {
                                onEnrollmentComplete()
                            }
                        }
                    },
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Thu âm",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isRecording) "Đang thu âm... (Bấm để hoàn thành câu)" else "Bấm nút và đọc to câu trên",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
