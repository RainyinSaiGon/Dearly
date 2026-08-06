package com.dearly.app.ui.elder

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MedicationForest = Color(0xFF174D3B)

@Composable
fun ElderMedicationScreen(onOpenCalls: () -> Unit = {}, onOpenSettings: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCF9))) {
        ElderHeader()
        Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Surface(color = MedicationForest, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProgressDonut()
                    Column(Modifier.padding(start = 30.dp)) { Text("Tiến độ hôm nay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Hôm nay bác đã hoàn thành\n3/5 liều thuốc.", color = Color(0xFFDCECE4), fontSize = 14.sp) }
                }
            }
            Period("☀ Buổi Sáng") { Dose("07:00", "Huyết áp", true); Dose("07:00", "Tiểu đường", true) }
            Period("☀ Buổi Chiều") { Dose("13:00", "Tiểu đường", true) }
            Period("◔ Buổi Tối") { Dose("19:00", "Huyết áp", false); Dose("19:00", "Tiểu đường", false) }
        }
        ElderFooter(ElderTab.MEDICATIONS, onOpenCalls, {}, onOpenSettings)
    }
}

@Composable private fun Period(title: String, content: @Composable () -> Unit) { Text(title, color = MedicationForest, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)); Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { content() } }
@Composable private fun Dose(time: String, name: String, complete: Boolean) { Surface(color = Color.White, shape = RoundedCornerShape(7.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(horizontal = 13.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text(time, color = MedicationForest, fontSize = 14.sp, modifier = Modifier.width(60.dp)); Column(Modifier.weight(1f)) { Text(name, color = Color(0xFF243C33), fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("1 viên", color = Color(0xFF66736F), fontSize = 13.sp) }; Surface(color = if (complete) Color(0xFF287054) else Color(0xFFE5E8E6), shape = CircleShape) { Text(if (complete) "✓ Đã nhắc" else "◷ Chưa", color = if (complete) Color.White else Color(0xFF6C7370), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) } } } }

@Composable
private fun ProgressDonut() {
    Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(58.dp)) {
            drawArc(Color(0xFF2F765E), -90f, 360f, false, style = Stroke(width = 5.dp.toPx()))
            drawArc(Color(0xFFA7F1CD), -90f, 216f, false, style = Stroke(width = 5.dp.toPx()))
        }
        Text("3/5", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
