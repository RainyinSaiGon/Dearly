package com.dearly.app.ui.elder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CallingForest = Color(0xFF174D3B)

@Composable
fun ElderCallingScreen(onOpenMedications: () -> Unit = {}, onOpenSettings: () -> Unit = {}) {
    var listening by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCF9))) {
        ElderHeader()
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 28.dp)) {
                Text(if (listening) "Đang lắng nghe" else "Bác muốn làm gì?", color = CallingForest, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Nói “Gọi cho...” hoặc “Thêm sổ...”", color = Color(0xFF64736E), fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                if (listening) ListeningWave(Modifier.padding(top = 32.dp), onClick = { listening = false }) else VoiceButton { listening = true }
                if (listening) Text("Chạm để dừng", color = CallingForest, fontSize = 15.sp, modifier = Modifier.padding(top = 24.dp).clickable { listening = false })
            }
        }
        ElderFooter(ElderTab.CALLS, {}, onOpenMedications, onOpenSettings)
    }
}

@Composable
private fun VoiceButton(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "voice_rings")
    val outerScale by transition.animateFloat(0.88f, 1.15f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "outer_ring")
    val innerScale by transition.animateFloat(0.94f, 1.08f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "inner_ring")
    Box(Modifier.padding(top = 18.dp).size(208.dp), contentAlignment = Alignment.Center) {
        Surface(color = CallingForest.copy(alpha = 0.07f), shape = CircleShape, modifier = Modifier.size(196.dp).graphicsLayer { scaleX = outerScale; scaleY = outerScale }) {}
        Surface(color = CallingForest.copy(alpha = 0.12f), shape = CircleShape, modifier = Modifier.size(168.dp).graphicsLayer { scaleX = innerScale; scaleY = innerScale }) {}
        Surface(color = CallingForest, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(4.dp, Color.White), shadowElevation = 5.dp, modifier = Modifier.size(130.dp).clickable(onClick = onClick)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                Icon(Icons.Outlined.Mic, null, tint = Color.White, modifier = Modifier.size(44.dp))
                Text("NHẤN ĐỂ NÓI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ListeningWave(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier.height(42.dp).clickable(onClick = onClick), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(720, 500, 860, 610, 780).forEachIndexed { index, duration -> WaveBar(duration, index) }
    }
}

@Composable
private fun WaveBar(duration: Int, index: Int) {
    val transition = rememberInfiniteTransition(label = "wave_$index")
    val scale by transition.animateFloat(0.22f, 1f, infiniteRepeatable(tween(duration), RepeatMode.Reverse), label = "bar_$index")
    Box(Modifier.height(38.dp), contentAlignment = Alignment.Center) {
        Surface(color = CallingForest, shape = RoundedCornerShape(3.dp), modifier = Modifier.width(6.dp).height(34.dp).graphicsLayer { scaleY = scale }) {}
    }
}
