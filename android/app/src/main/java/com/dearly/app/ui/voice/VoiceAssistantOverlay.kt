package com.dearly.app.ui.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class VoiceAssistantState {
    LISTENING,
    PROCESSING,
    VERIFYING_SPEAKER,
    RESPONDING
}

@Composable
fun VoiceAssistantOverlay(
    onDismiss: () -> Unit
) {
    var state by remember { mutableStateOf(VoiceAssistantState.LISTENING) }
    var recognizedText by remember { mutableStateOf("Đang lắng nghe câu lệnh của bác...") }
    var assistantResponseText by remember { mutableStateOf("") }
    var verificationStatus by remember { mutableStateOf<Boolean?>(null) }

    // Waveform Pulsing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(48.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Verification Badge (Shown when checking SV)
                if (verificationStatus != null || state == VoiceAssistantState.VERIFYING_SPEAKER) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (verificationStatus) {
                                true -> Color(0xFFDCFCE7)
                                false -> Color(0xFFFEE2E2)
                                null -> Color(0xFFFEF3C7)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "SV Security", tint = Color.Unspecified)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (verificationStatus) {
                                    true -> "Xác minh giọng nói thành công (ECAPA-TDNN)"
                                    false -> "Xác minh giọng nói thất bại"
                                    null -> "Đang xác minh giọng nói (SV)..."
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Pulsing Audio Microphone Graphic
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(if (state == VoiceAssistantState.LISTENING) scale else 1.0f)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Transcript & Response Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = recognizedText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        if (assistantResponseText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = assistantResponseText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
