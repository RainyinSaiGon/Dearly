package com.dearly.app.ui.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dearly.app.domain.model.DoseStatus
import com.dearly.app.domain.model.MedicationLog
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val ActivityForest = Color(0xFF174D3B)
private val ActivityForestSoft = Color(0xFF326854)
private val ActivityMuted = Color(0xFF7D8B90)

@Composable
fun CaregiverActivityScreen(
    medicationLogs: List<MedicationLog> = emptyList(),
    contactsCount: Int = 0,
    hasLinkedElder: Boolean = true,
    busy: Boolean = false,
    error: String? = null,
    onOpenCalls: () -> Unit = {},
    onOpenMedications: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val completed = medicationLogs.count { it.status == DoseStatus.TAKEN }
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCF9))) {
        CaregiverHeader()
        ElderSummary(contactsCount, completed, medicationLogs.size)
        if (hasLinkedElder) {
            ActivityContent(
                logs = medicationLogs,
                busy = busy,
                error = error,
                modifier = Modifier.weight(1f)
            )
        } else {
            LinkElderPrompt(
                modifier = Modifier.weight(1f),
                onOpenSettings = onOpenSettings
            )
        }
        CaregiverBottomNavigation(
            selectedTab = CaregiverTab.ACTIVITY,
            onTabSelected = {
                if (it == CaregiverTab.CALLS) onOpenCalls()
                if (it == CaregiverTab.MEDICATIONS) onOpenMedications()
                if (it == CaregiverTab.SETTINGS) onOpenSettings()
            }
        )
    }
}

@Composable
private fun LinkElderPrompt(modifier: Modifier, onOpenSettings: () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.Medication, null, tint = ActivityMuted)
        Text(
            "Chưa kết nối người được chăm sóc",
            color = ActivityForest,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            "Vào Cài đặt để nhập mã kết nối.",
            color = ActivityMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.padding(top = 18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ActivityForest)
        ) {
            Text("Mở Cài đặt")
        }
    }
}

@Composable
private fun ElderSummary(contactsCount: Int, completed: Int, total: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
            .background(ActivityForest)
            .padding(horizontal = 16.dp, vertical = 17.dp)
    ) {
        Text("Người thân của bạn", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Dữ liệu chăm sóc hôm nay", color = Color(0xFFD0DFD7), fontSize = 12.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 14.dp)
        ) {
            SummaryMetric(contactsCount.toString(), "Liên hệ", Modifier.weight(1f))
            SummaryMetric("$completed/$total", "Liều đã uống", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.height(62.dp).background(ActivityForestSoft, RoundedCornerShape(7.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFFD2DED8), fontSize = 11.sp)
    }
}

@Composable
private fun ActivityContent(
    logs: List<MedicationLog>,
    busy: Boolean,
    error: String?,
    modifier: Modifier
) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Thuốc hôm nay", color = ActivityForest, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                color = ActivityMuted,
                fontSize = 11.sp
            )
        }
        error?.let {
            Text(it, color = Color(0xFFC62828), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        if (logs.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.Medication, null, tint = ActivityMuted)
                Text(
                    if (busy) "Đang tải lịch thuốc..." else "Hôm nay chưa có lịch thuốc",
                    color = ActivityMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                items(logs, key = MedicationLog::id) { log -> MedicationLogRow(log) }
            }
        }
    }
}

@Composable
private fun MedicationLogRow(log: MedicationLog) {
    val completed = log.status == DoseStatus.TAKEN
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(formatTime(log.scheduledTime), color = ActivityMuted, fontSize = 12.sp, modifier = Modifier.width(48.dp))
        Text(log.medicationName, color = Color(0xFF33403D), fontSize = 14.sp, modifier = Modifier.weight(1f))
        Surface(
            color = if (completed) Color(0xFFE1F9EB) else Color(0xFFF3F5F6),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                statusLabel(log.status),
                color = if (completed) Color(0xFF167A50) else ActivityMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }
    }
}

private fun statusLabel(status: DoseStatus): String = when (status) {
    DoseStatus.PENDING -> "Chưa uống"
    DoseStatus.SNOOZED -> "Đã hoãn"
    DoseStatus.MISSED -> "Đã lỡ"
    DoseStatus.TAKEN -> "Đã uống"
}

private fun formatTime(value: String): String = runCatching {
    OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault(value.take(5))
