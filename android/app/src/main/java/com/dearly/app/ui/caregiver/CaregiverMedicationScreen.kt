package com.dearly.app.ui.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dearly.app.domain.model.Medication
import com.dearly.app.domain.model.NewMedication

private val MedicineForest = Color(0xFF174D3B)

@Composable
fun CaregiverMedicationScreen(
    medications: List<Medication> = emptyList(),
    busy: Boolean = false,
    error: String? = null,
    onAdd: (NewMedication) -> Unit = {},
    onUpdate: (String, NewMedication) -> Unit = { _, _ -> },
    onDelete: (String) -> Unit = {},
    onOpenActivity: () -> Unit = {},
    onOpenCalls: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Medication?>(null) }
    var deleting by remember { mutableStateOf<Medication?>(null) }
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCF9))) {
        CaregiverHeader()
        Column(Modifier.weight(1f).padding(horizontal = 22.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Danh sách thuốc", color = MedicineForest, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { adding = true },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MedicineForest)
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                    Text("Thêm", modifier = Modifier.padding(start = 5.dp))
                }
            }
            error?.let { Text(it, color = Color(0xFFC62828), fontSize = 13.sp) }
            if (medications.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Medication, null, tint = Color(0xFF8A9692), modifier = Modifier.size(44.dp))
                    Text("Chưa có lịch thuốc", color = Color(0xFF66736F), modifier = Modifier.padding(top = 10.dp))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    items(medications, key = Medication::id) { medication ->
                        MedicationRow(medication, { editing = medication }, { deleting = medication })
                    }
                }
            }
        }
        CaregiverBottomNavigation(
            selectedTab = CaregiverTab.MEDICATIONS,
            onTabSelected = {
                if (it == CaregiverTab.ACTIVITY) onOpenActivity()
                if (it == CaregiverTab.CALLS) onOpenCalls()
                if (it == CaregiverTab.SETTINGS) onOpenSettings()
            }
        )
    }
    if (adding) {
        MedicationEditorDialog(null, { adding = false }) { onAdd(it); adding = false }
    }
    editing?.let { medication ->
        MedicationEditorDialog(medication, { editing = null }) {
            onUpdate(medication.id, it)
            editing = null
        }
    }
    deleting?.let { medication ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Xóa thuốc?") },
            text = { Text("Lịch ${medication.name} và các liều chưa uống sẽ bị xóa.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(medication.id); deleting = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun MedicationRow(medication: Medication, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(8.dp), shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(medication.name, color = Color(0xFF24322F), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(medication.dosage, color = Color(0xFF66736F), fontSize = 13.sp)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.MoreHoriz, "Sửa thuốc") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, "Xóa thuốc", tint = Color(0xFFC62828))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                medication.timeSlots.forEach { time ->
                    Surface(color = Color(0xFFE1F0E9), shape = RoundedCornerShape(6.dp)) {
                        Text(time, color = MedicineForest, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationEditorDialog(
    initial: Medication?,
    onDismiss: () -> Unit,
    onSave: (NewMedication) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var dosage by remember(initial) { mutableStateOf(initial?.dosage.orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var slots by remember(initial) { mutableStateOf(initial?.timeSlots ?: listOf("07:00")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Thêm thuốc" else "Sửa lịch thuốc", color = MedicineForest) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Tên thuốc") }, singleLine = true)
                OutlinedTextField(dosage, { dosage = it }, label = { Text("Liều lượng") }, singleLine = true)
                slots.forEachIndexed { index, value ->
                    OutlinedTextField(
                        value,
                        { updated -> slots = slots.toMutableList().also { it[index] = updated } },
                        label = { Text("Giờ ${index + 1} (HH:mm)") },
                        singleLine = true,
                        trailingIcon = {
                            if (slots.size > 1) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    "Xóa giờ",
                                    modifier = Modifier.clickable {
                                        slots = slots.toMutableList().also { it.removeAt(index) }
                                    }
                                )
                            }
                        }
                    )
                }
                TextButton(onClick = { if (slots.size < 5) slots = slots + "07:00" }) {
                    Icon(Icons.Outlined.Add, null)
                    Text("Thêm giờ")
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Ghi chú") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(NewMedication(name, dosage, slots, notes.ifBlank { null })) },
                enabled = name.isNotBlank() && slots.all { it.matches(Regex("\\d{2}:\\d{2}")) },
                colors = ButtonDefaults.buttonColors(containerColor = MedicineForest)
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}
