package com.dearly.app.ui.elder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dearly.app.domain.model.DoseStatus
import com.dearly.app.domain.model.MedicationLog

@Composable
fun ElderMedicationScreen(
    onMarkAsTaken: (MedicationLog) -> Unit
) {
    var logs by remember {
        mutableStateOf(
            listOf(
                MedicationLog("1", "m1", "Thuốc Huyết Áp (Amlodipine)", "08:00 AM", DoseStatus.TAKEN, "08:05 AM"),
                MedicationLog("2", "m2", "Thuốc Bổ Não (Ginkgo Biloba)", "12:00 PM", DoseStatus.PENDING),
                MedicationLog("3", "m3", "Thuốc Tim Mạch (Aspirin)", "07:00 PM", DoseStatus.PENDING)
            )
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Lịch Uống Thuốc",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(logs) { log ->
                MedicationLogCard(
                    log = log,
                    onToggleStatus = { targetLog ->
                        logs = logs.map {
                            if (it.id == targetLog.id) {
                                val newStatus = if (it.status == DoseStatus.TAKEN) DoseStatus.PENDING else DoseStatus.TAKEN
                                it.copy(status = newStatus).also(onMarkAsTaken)
                            } else it
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MedicationLogCard(
    log: MedicationLog,
    onToggleStatus: (MedicationLog) -> Unit
) {
    val isTaken = log.status == DoseStatus.TAKEN
    val containerColor = if (isTaken) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.medicationName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Giờ uống: ${log.scheduledTime}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            IconButton(
                onClick = { onToggleStatus(log) },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Trạng thái",
                    tint = if (isTaken) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}
