package com.dearly.app.ui.elder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.dearly.app.domain.model.DoseStatus
import com.dearly.app.domain.model.MedicationLog
import com.dearly.app.ui.components.VoiceCaptureButton
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val MedicationForest = Color(0xFF174D3B)

@Composable
fun ElderMedicationScreen(
    logs: List<MedicationLog> = emptyList(),
    busy: Boolean = false,
    error: String? = null,
    onVerifyTaken: (MedicationLog, File, () -> Unit) -> Unit = { _, _, _ -> },
    onOpenCalls: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var selected by remember { mutableStateOf<MedicationLog?>(null) }
    val completed = logs.count { it.status == DoseStatus.TAKEN }
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCF9))) {
        ElderHeader()
        Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Surface(color = MedicationForest, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tiến độ hôm nay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Bác đã hoàn thành $completed/${logs.size} liều thuốc.",
                        color = Color(0xFFDCECE4),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            error?.let { Text(it, color = Color(0xFFC62828), fontSize = 13.sp) }
            if (logs.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Medication, null, tint = Color(0xFF8A9692))
                    Text("Hôm nay không có liều thuốc", color = Color(0xFF66736F), modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    items(logs, key = MedicationLog::id) { log ->
                        DoseRow(log, enabled = !busy && log.status != DoseStatus.TAKEN) { selected = log }
                    }
                }
            }
        }
        ElderFooter(ElderTab.MEDICATIONS, onOpenCalls, {}, onOpenSettings)
    }
    selected?.let { log ->
        AlertDialog(
            onDismissRequest = { if (!busy) selected = null },
            title = { Text("Xác nhận đã uống thuốc", color = MedicationForest) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Bác đọc câu xác minh để đánh dấu ${log.medicationName} đã uống.",
                        textAlign = TextAlign.Center
                    )
                    VoiceCaptureButton(
                        onAudioReady = { audio ->
                            onVerifyTaken(log, audio) { selected = null }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        idleLabel = "Nói câu xác minh"
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun DoseRow(log: MedicationLog, enabled: Boolean, onClick: () -> Unit) {
    val taken = log.status == DoseStatus.TAKEN
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(7.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(log.scheduledTime), color = MedicationForest, modifier = Modifier.width(60.dp))
            Column(Modifier.weight(1f)) {
                Text(log.medicationName, color = Color(0xFF243C33), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(if (taken) "Đã uống" else "Chạm để xác nhận", color = Color(0xFF66736F), fontSize = 13.sp)
            }
            Text(
                if (taken) "Đã xong" else "Chưa uống",
                color = if (taken) Color(0xFF287054) else Color(0xFF8A6D22),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

private fun formatTime(value: String): String = runCatching {
    OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault(value.take(5))
