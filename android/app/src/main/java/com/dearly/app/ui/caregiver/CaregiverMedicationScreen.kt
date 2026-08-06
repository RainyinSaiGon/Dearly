package com.dearly.app.ui.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private data class Medicine(val name: String, val frequency: String, val times: List<String>, val color: Color)
private val MedicineForest = Color(0xFF174D3B)

@Composable
fun CaregiverMedicationScreen(onOpenActivity: () -> Unit = {}, onOpenCalls: () -> Unit = {}, onOpenSettings: () -> Unit = {}) {
    var medicines by remember {
        mutableStateOf(listOf(
            Medicine("Thuốc huyết áp", "2 lần/ngày", listOf("07:00", "19:00"), Color(0xFFFFE8D7)),
            Medicine("Thuốc tiểu đường", "3 lần/ngày", listOf("07:00", "13:00", "19:00"), Color(0xFFFFE3EF))
        ))
    }
    var showAdd by remember { mutableStateOf(false) }
    var selectedMedicine by remember { mutableStateOf<Medicine?>(null) }
    var showMedicineActions by remember { mutableStateOf(false) }
    var showEditMedicine by remember { mutableStateOf(false) }
    var showDeleteMedicine by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCF9))) {
        CaregiverHeader()
        Column(Modifier.weight(1f).padding(horizontal = 22.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Danh sách thuốc", color = MedicineForest, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Surface(color = MedicineForest, shape = RoundedCornerShape(18.dp), modifier = Modifier.clickable { showAdd = true }) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("Thêm", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                medicines.forEach { medicine ->
                    MedicineCard(medicine) {
                        selectedMedicine = medicine
                        showMedicineActions = true
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
    if (showAdd) {
        AddMedicationDialog(
            onDismiss = { showAdd = false },
            onAdd = { name, dose, note, time ->
                medicines = medicines + Medicine(name.ifBlank { "Thuốc mới" }, "1 lần/ngày", listOf(time), Color(0xFFE1F9EB))
                showAdd = false
            }
        )
    }
    if (showMedicineActions && selectedMedicine != null) {
        MedicationActionsDialog(
            onDismiss = { showMedicineActions = false },
            onEdit = {
                showMedicineActions = false
                showEditMedicine = true
            },
            onDelete = {
                showMedicineActions = false
                showDeleteMedicine = true
            }
        )
    }
    if (showEditMedicine && selectedMedicine != null) {
        EditMedicationDialog(
            medicine = selectedMedicine!!,
            onDismiss = { showEditMedicine = false },
            onSave = { updatedMedicine ->
                medicines = medicines.map { if (it == selectedMedicine) updatedMedicine else it }
                showEditMedicine = false
            }
        )
    }
    if (showDeleteMedicine && selectedMedicine != null) {
        DeleteMedicationDialog(
            medicine = selectedMedicine!!,
            onDismiss = { showDeleteMedicine = false },
            onConfirm = {
                medicines = medicines.filterNot { it == selectedMedicine }
                selectedMedicine = null
                showDeleteMedicine = false
            }
        )
    }
}

@Composable
private fun MedicineCard(medicine: Medicine, onMoreClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(17.dp), shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = medicine.color, shape = RoundedCornerShape(7.dp)) {
                    Text("● ${medicine.frequency}", color = Color(0xFFC95D22), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.MoreHoriz, contentDescription = "Tùy chọn", tint = Color(0xFFC5CED0), modifier = Modifier.size(26.dp).clickable(onClick = onMoreClick))
            }
            Text(medicine.name, color = Color(0xFF24322F), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                medicine.times.flatMap { it.split("|") }.forEachIndexed { index, time ->
                    Surface(color = if (index == 0) Color(0xFFE1F9EB) else Color(0xFFF5F6F6), shape = RoundedCornerShape(7.dp)) {
                        Text(time, color = if (index == 0) Color(0xFF168154) else Color(0xFF9AA3A5), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegacyAddMedicationDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("07:00") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Thêm thuốc mới", color = MedicineForest, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("×", color = Color(0xFF66736F), fontSize = 24.sp, modifier = Modifier.clickable(onClick = onDismiss))
                }
                Spacer(Modifier.height(14.dp))
                MedicationInput("Tên thuốc", name, "Nhập tên thuốc...") { name = it }
                MedicationInput("Liều lượng", dose, "1 viên, 5ml...") { dose = it }
                Text("Ghi chú", color = Color(0xFF52615D), fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, placeholder = { Text("Ví dụ: Uống sau khi ăn 30 phút...", fontSize = 14.sp) }, modifier = Modifier.fillMaxWidth().height(80.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp))
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Thời gian uống thuốc", color = Color(0xFF52615D), fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text("+ Thêm giờ", color = MedicineForest, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Surface(color = Color(0xFFF4F8F5), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MedicineForest, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(time, color = Color(0xFF24322F), fontSize = 15.sp)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Xóa giờ", tint = Color(0xFFD22C2C), modifier = Modifier.size(16.dp))
                    }
                }
                Button(onClick = { onAdd(name, dose, note, time) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = MedicineForest)) {
                    Text("Lưu thông tin", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("Hủy", color = Color(0xFF1B4332), fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onDismiss))
            }
        }
    }
}

@Composable
private fun MedicationInput(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Text(label, color = Color(0xFF52615D), fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
    if (label.contains("trong")) {
        val count = value.toIntOrNull()?.coerceIn(1, 5) ?: 1
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(7.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF77807D)),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(
                    Icons.Outlined.Remove,
                    contentDescription = "Giảm số lần uống",
                    tint = if (count > 1) MedicineForest else Color(0xFFB6C0BC),
                    modifier = Modifier.size(24.dp).clickable { if (count > 1) onValueChange((count - 1).toString()) }
                )
                Text(count.toString(), color = Color(0xFF24322F), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 28.dp))
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Tăng số lần uống",
                    tint = if (count < 5) MedicineForest else Color(0xFFB6C0BC),
                    modifier = Modifier.size(24.dp).clickable { if (count < 5) onValueChange((count + 1).toString()) }
                )
            }
        }
    } else {
        OutlinedTextField(value = value, onValueChange = onValueChange, placeholder = { Text(placeholder, fontSize = 14.sp) }, modifier = Modifier.fillMaxWidth().height(52.dp), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp))
    }
    Spacer(Modifier.height(9.dp))
}

@Composable
private fun MedicationActionsDialog(onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(horizontal = 28.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tùy chọn chỉnh sửa", color = MedicineForest, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text("Thay đổi", color = MedicineForest, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(vertical = 12.dp), textAlign = TextAlign.Center)
                Text("Xóa thuốc", color = Color(0xFFC62828), fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable(onClick = onDelete).padding(vertical = 12.dp), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun LegacyEditMedicationDialog(medicine: Medicine, onDismiss: () -> Unit, onSave: (Medicine) -> Unit) {
    var name by remember(medicine) { mutableStateOf(medicine.name) }
    var frequency by remember(medicine) { mutableStateOf(medicine.frequency) }
    var note by remember { mutableStateOf("") }
    var time by remember(medicine) { mutableStateOf(medicine.times.firstOrNull() ?: "07:00") }
    var times by remember(medicine) { mutableStateOf(medicine.times) }
    var doseCountText by remember(medicine) { mutableStateOf(medicine.times.size.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Chỉnh sửa thuốc", color = MedicineForest, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("×", color = Color(0xFF66736F), fontSize = 24.sp, modifier = Modifier.clickable(onClick = onDismiss))
                }
                Spacer(Modifier.height(14.dp))
                MedicationInput("Tên thuốc", name, "Nhập tên thuốc...") { name = it }
                MedicationInput("Số lần uống mỗi ngày", frequency, "1 lần") { frequency = it }
                Text("Ghi chú", color = Color(0xFF52615D), fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, placeholder = { Text("Ví dụ: Uống sau khi ăn 30 phút...", fontSize = 14.sp) }, modifier = Modifier.fillMaxWidth().height(80.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp))
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Thời gian uống thuốc", color = Color(0xFF52615D), fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "+ Thêm giờ",
                        color = MedicineForest,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            if (times.size < 5) {
                                times = times + "07:00"
                                doseCountText = times.size.toString()
                            }
                        }
                    )
                }
                Surface(color = Color(0xFFF4F8F5), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MedicineForest, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(time, color = Color(0xFF24322F), fontSize = 15.sp)
                    }
                }
                Button(onClick = { onSave(medicine.copy(name = name, frequency = frequency, times = listOf(time))) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = MedicineForest)) {
                    Text("Lưu thay đổi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("Hủy", color = Color(0xFF1B4332), fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onDismiss))
            }
        }
    }
}

@Composable
private fun DeleteMedicationDialog(medicine: Medicine, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Xóa thuốc?", color = Color(0xFF24322F), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("Bạn có chắc muốn xóa ${medicine.name} khỏi danh sách thuốc?", color = Color(0xFF66736F), fontSize = 16.sp, modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 20.dp), textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Hủy", color = Color(0xFF1B4332), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) {
                        Text("Xóa", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditMedicationDialog(medicine: Medicine, onDismiss: () -> Unit, onSave: (Medicine) -> Unit) {
    var name by remember(medicine) { mutableStateOf(medicine.name) }
    var doseCountText by remember(medicine) { mutableStateOf(medicine.times.size.toString()) }
    var note by remember { mutableStateOf("") }
    var times by remember(medicine) { mutableStateOf(medicine.times) }

    fun updateDoseCount(input: String) {
        val digits = input.filter { it.isDigit() }
        val count = digits.toIntOrNull()?.coerceIn(1, 5) ?: return
        doseCountText = count.toString()
        times = when {
            count < times.size -> times.take(count)
            count > times.size -> times + List(count - times.size) { "07:00" }
            else -> times
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Chỉnh sửa thuốc", color = MedicineForest, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("×", color = Color(0xFF66736F), fontSize = 24.sp, modifier = Modifier.clickable(onClick = onDismiss))
                }
                Spacer(Modifier.height(14.dp))
                MedicationInput("Tên thuốc", name, "Nhập tên thuốc...") { name = it }
                MedicationInput("Số lần uống trong ngày", doseCountText, "Ví dụ: 2") { updateDoseCount(it) }
                Text("Ghi chú", color = Color(0xFF52615D), fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, placeholder = { Text("Ví dụ: Uống sau khi ăn 30 phút...", fontSize = 14.sp) }, modifier = Modifier.fillMaxWidth().height(72.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp))
                Text("Thời gian uống thuốc", color = Color(0xFF52615D), fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "+ Thêm giờ",
                        color = MedicineForest,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            if (times.size < 5) {
                                times = times + "07:00"
                                doseCountText = times.size.toString()
                            }
                        }
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    times.forEachIndexed { index, time ->
                        Surface(color = Color(0xFFF4F8F5), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MedicineForest, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = time,
                                    onValueChange = { value -> times = times.toMutableList().also { it[index] = value } },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
                                    trailingIcon = {
                                        if (times.size > 1) {
                                            Icon(
                                                Icons.Outlined.DeleteOutline,
                                                contentDescription = "Xóa giờ",
                                                tint = Color(0xFFD22C2C),
                                                modifier = Modifier.size(18.dp).clickable {
                                                    times = times.toMutableList().also { it.removeAt(index) }
                                                    doseCountText = times.size.toString()
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        val finalTimes = times.ifEmpty { listOf("07:00") }
                        onSave(medicine.copy(name = name, frequency = "${finalTimes.size} lần/ngày", times = finalTimes))
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicineForest)
                ) { Text("Lưu thay đổi", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Text("Hủy", color = Color(0xFF1B4332), fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onDismiss))
            }
        }
    }
}

@Composable
private fun AddMedicationDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var doseCountText by remember { mutableStateOf("1") }
    var note by remember { mutableStateOf("") }
    var times by remember { mutableStateOf(listOf("07:00")) }

    fun updateDoseCount(input: String) {
        val count = input.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 5) ?: return
        doseCountText = count.toString()
        times = when {
            count < times.size -> times.take(count)
            count > times.size -> times + List(count - times.size) { "07:00" }
            else -> times
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Thêm thuốc mới", color = MedicineForest, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("×", color = Color(0xFF66736F), fontSize = 24.sp, modifier = Modifier.clickable(onClick = onDismiss))
                }
                Spacer(Modifier.height(14.dp))
                MedicationInput("Tên thuốc", name, "Nhập tên thuốc...") { name = it }
                MedicationInput("Số lần uống trong ngày", doseCountText, "Ví dụ: 2") { updateDoseCount(it) }
                Text("Ghi chú", color = Color(0xFF52615D), fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, placeholder = { Text("Ví dụ: Uống sau khi ăn 30 phút...", fontSize = 14.sp) }, modifier = Modifier.fillMaxWidth().height(72.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp))
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Thời gian uống thuốc", color = Color(0xFF52615D), fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text("+ Thêm giờ", color = MedicineForest, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                        if (times.size < 5) {
                            times = times + "07:00"
                            doseCountText = times.size.toString()
                        }
                    })
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    times.forEachIndexed { index, time ->
                        Surface(color = Color(0xFFF4F8F5), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MedicineForest, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(value = time, onValueChange = { value -> times = times.toMutableList().also { it[index] = value } }, modifier = Modifier.weight(1f).height(56.dp), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp), trailingIcon = {
                                    if (times.size > 1) Icon(Icons.Outlined.DeleteOutline, contentDescription = "Xóa giờ", tint = Color(0xFFD22C2C), modifier = Modifier.size(18.dp).clickable {
                                        times = times.toMutableList().also { it.removeAt(index) }
                                        doseCountText = times.size.toString()
                                    })
                                })
                            }
                        }
                    }
                }
                Button(onClick = { onAdd(name, doseCountText, note, times.joinToString("|")) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = MedicineForest)) {
                    Text("Lưu thông tin", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("Hủy", color = Color(0xFF1B4332), fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onDismiss))
            }
        }
    }
}
