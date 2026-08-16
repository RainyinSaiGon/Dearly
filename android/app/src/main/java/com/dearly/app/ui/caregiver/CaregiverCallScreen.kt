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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.dearly.app.domain.model.CallMethod
import com.dearly.app.domain.model.Contact
import com.dearly.app.domain.model.NewContact

private val CallForest = Color(0xFF174D3B)

@Composable
fun CaregiverCallScreen(
    contacts: List<Contact> = emptyList(),
    busy: Boolean = false,
    error: String? = null,
    onAdd: (NewContact) -> Unit = {},
    onUpdate: (Contact) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onOpenActivity: () -> Unit = {},
    onOpenMedications: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var editing by remember { mutableStateOf<Contact?>(null) }
    var deleting by remember { mutableStateOf<Contact?>(null) }
    var adding by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCF9))) {
        CaregiverHeader(shadowElevation = 5.dp)
        Column(Modifier.weight(1f).padding(horizontal = 22.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Danh sách gọi", color = CallForest, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { adding = true },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = CallForest)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Thêm")
                }
            }
            error?.let { Text(it, color = Color(0xFFC62828), fontSize = 13.sp) }
            if (contacts.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.PersonOutline, null, tint = Color(0xFF8A9692), modifier = Modifier.size(44.dp))
                    Text("Chưa có liên hệ", color = Color(0xFF66736F), modifier = Modifier.padding(top = 10.dp))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    items(contacts, key = Contact::id) { contact ->
                        ContactRow(contact, onEdit = { editing = contact }, onDelete = { deleting = contact })
                    }
                }
            }
        }
        CaregiverBottomNavigation(
            selectedTab = CaregiverTab.CALLS,
            onTabSelected = {
                if (it == CaregiverTab.ACTIVITY) onOpenActivity()
                if (it == CaregiverTab.MEDICATIONS) onOpenMedications()
                if (it == CaregiverTab.SETTINGS) onOpenSettings()
            }
        )
    }
    if (adding) {
        ContactEditorDialog(
            initial = null,
            onDismiss = { adding = false },
            onSave = { onAdd(it); adding = false }
        )
    }
    editing?.let { contact ->
        ContactEditorDialog(
            initial = contact,
            onDismiss = { editing = null },
            onSave = { updated ->
                onUpdate(
                    contact.copy(
                        nickname = updated.nickname,
                        fullName = updated.fullName,
                        phoneNumber = updated.phoneNumber,
                        relationship = updated.relationship,
                        callMethod = updated.callMethod
                    )
                )
                editing = null
            }
        )
    }
    deleting?.let { contact ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Xóa liên hệ?") },
            text = { Text("${contact.fullName} sẽ bị xóa khỏi danh sách gọi.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(contact.id); deleting = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun ContactRow(contact: Contact, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(8.dp), shadowElevation = 1.dp) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = Color(0xFF287054), shape = CircleShape, modifier = Modifier.size(44.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(initials(contact.fullName), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(contact.nickname.ifBlank { contact.fullName }, fontWeight = FontWeight.Bold, color = Color(0xFF24322F))
                Text(contact.fullName, color = Color(0xFF66736F), fontSize = 12.sp)
                Text(contact.phoneNumber, color = Color(0xFF8A9692), fontSize = 12.sp)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.MoreHoriz, contentDescription = "Sửa liên hệ")
            }
            Text(
                "Xóa",
                color = Color(0xFFC62828),
                fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onDelete).padding(6.dp)
            )
        }
    }
}

@Composable
private fun ContactEditorDialog(
    initial: Contact?,
    onDismiss: () -> Unit,
    onSave: (NewContact) -> Unit
) {
    var nickname by remember(initial) { mutableStateOf(initial?.nickname.orEmpty()) }
    var fullName by remember(initial) { mutableStateOf(initial?.fullName.orEmpty()) }
    var phone by remember(initial) { mutableStateOf(initial?.phoneNumber.orEmpty()) }
    var relationship by remember(initial) { mutableStateOf(initial?.relationship.orEmpty()) }
    var method by remember(initial) { mutableStateOf(initial?.callMethod ?: CallMethod.PHONE) }
    val valid = fullName.isNotBlank() && phone.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Thêm liên hệ" else "Sửa liên hệ", color = CallForest) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nickname, { nickname = it }, label = { Text("Tên gọi thân mật") }, singleLine = true)
                OutlinedTextField(fullName, { fullName = it }, label = { Text("Họ và tên") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Số điện thoại") }, singleLine = true)
                OutlinedTextField(relationship, { relationship = it }, label = { Text("Quan hệ") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = method == CallMethod.PHONE, onClick = { method = CallMethod.PHONE })
                    Text("Điện thoại")
                    RadioButton(selected = method == CallMethod.ZALO_VIDEO, onClick = { method = CallMethod.ZALO_VIDEO })
                    Text("Zalo")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(NewContact(nickname, fullName, phone, relationship, method))
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = CallForest)
            ) { Text("Lưu") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

private fun initials(name: String): String = name.trim().split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { "?" }
