package com.dearly.app.ui.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonOutline
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val CallScreenBackground = Color(0xFFFCFCF9)
private val CallForest = Color(0xFF174D3B)

private data class CallContact(
    val initials: String,
    val displayName: String,
    val fullName: String,
    val relation: String,
    val number: String,
    val color: Color
)

@Composable
fun CaregiverCallScreen(
    onOpenActivity: () -> Unit = {},
    onOpenMedications: () -> Unit = {}, onOpenSettings: () -> Unit = {}
) {
    var contacts by remember { mutableStateOf(listOf(
        CallContact("TK", "thằng Tí", "Trần Minh Khoa", "Con trai", "0901234567", Color(0xFF079BE5)),
        CallContact("TL", "con Lan", "Trần Thu Lan", "Con gái", "0912345678", Color(0xFFD92B7B)),
        CallContact("NL", "bà Lợi", "Nguyễn Thị Lợi", "Bạn bè", "0947392874", Color(0xFFF45D08)),
        CallContact("LH", "em Hoa", "Lâm Như Hoa", "Họ hàng", "0975928274", Color(0xFF188844))
    )) }
    var selectedContact by remember { mutableStateOf<CallContact?>(null) }
    var showActions by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showAddContact by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(CallScreenBackground)) {
        CaregiverHeader(shadowElevation = 5.dp)
        Column(Modifier.weight(1f).padding(horizontal = 22.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Danh sách gọi", color = CallForest, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Surface(color = CallForest, shape = RoundedCornerShape(18.dp), modifier = Modifier.clickable { showAddContact = true }) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Thêm", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(
                "Ba gọi bằng tên gọi thân mật",
                color = Color(0xFF7D8B90),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 14.dp, bottom = 22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                contacts.forEach { contact ->
                    CallContactCard(contact) {
                        selectedContact = contact
                        showActions = true
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

    if (showActions && selectedContact != null) {
        ContactActionsDialog(
            onDismiss = { showActions = false },
            onEdit = {
                showActions = false
                showEdit = true
            },
            onDelete = {
                showActions = false
                showDeleteConfirmation = true
            }
        )
    }
    if (showEdit && selectedContact != null) {
        ContactEditDialog(
            contact = selectedContact!!,
            onDismiss = { showEdit = false },
            onSave = { updatedContact ->
                contacts = contacts.map { if (it == selectedContact) updatedContact else it }
                showEdit = false
            }
        )
    }
    if (showDeleteConfirmation && selectedContact != null) {
        DeleteContactDialog(
            contact = selectedContact!!,
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                contacts = contacts.filterNot { it == selectedContact }
                showDeleteConfirmation = false
                selectedContact = null
            }
        )
    }
    if (showAddContact) {
        AddContactDialog(
            onDismiss = { showAddContact = false },
            onAdd = { nickname, relation, phone ->
                val initials = nickname.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "MH" }
                contacts = contacts + CallContact(initials, nickname, nickname, relation, phone, Color(0xFF188844))
                showAddContact = false
            }
        )
    }
}

@Composable
private fun CallContactCard(contact: CallContact, onEditContact: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(17.dp), shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = contact.color, shape = CircleShape, modifier = Modifier.size(46.dp)) {
                BoxedInitials(contact.initials)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\"${contact.displayName}\"", color = Color(0xFF24322F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(contact.relation, color = Color(0xFF788387), fontSize = 10.sp, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFF1F3F3)).padding(horizontal = 6.dp, vertical = 3.dp))
                }
                Text(contact.fullName, color = Color(0xFF788387), fontSize = 12.sp)
                Text("${contact.number} - Zalo Video Call", color = Color(0xFF9AA3A5), fontSize = 10.sp)
            }
            Icon(Icons.Outlined.MoreHoriz, contentDescription = "Chỉnh sửa liên hệ", tint = Color(0xFFC5CED0), modifier = Modifier.size(28.dp).clickable(onClick = onEditContact))
        }
    }
}

@Composable
private fun LegacyContactActionsDialog(onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(20.dp)) {
                Text("Tùy chọn", color = CallForest, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Text("Chỉnh sửa liên hệ", color = CallForest, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(vertical = 12.dp))
                Text("Xóa liên hệ", color = Color(0xFFC62828), fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable(onClick = onDelete).padding(vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun ContactActionsDialog(onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Tùy chọn liên hệ",
                    color = CallForest,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Thay đổi",
                    color = CallForest,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Xóa liên hệ",
                    color = Color(0xFFC62828),
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onDelete).padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LegacyContactEditDialog(contact: CallContact, onDismiss: () -> Unit, onSave: (CallContact) -> Unit) {
    var nickname by remember(contact) { mutableStateOf(contact.displayName) }
    var relation by remember(contact) { mutableStateOf(contact.relation) }
    var method by remember(contact) { mutableStateOf("Zalo") }
    var phone by remember(contact) { mutableStateOf(contact.number) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sửa đổi", color = CallForest, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("×", color = Color(0xFF66736F), fontSize = 20.sp, modifier = Modifier.clickable(onClick = onDismiss))
                }
                Spacer(Modifier.height(12.dp))
                Text("Tên gọi nhớ", color = Color(0xFF52615D), fontSize = 10.sp)
                CompactTextField(nickname) { nickname = it }
                Text("Quan hệ", color = Color(0xFF52615D), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                CompactTextField(relation) { relation = it }
                Text("Phương thức liên hệ", color = Color(0xFF52615D), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                CompactTextField(method) { method = it }
                Text("Hình thức", color = Color(0xFF52615D), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MethodChip("Video Call", true)
                    MethodChip("Gọi điện", false)
                }
                Text("Số điện thoại", color = Color(0xFF52615D), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                CompactTextField(phone) { phone = it }
                androidx.compose.material3.Button(
                    onClick = { onSave(contact.copy(displayName = nickname, relation = relation, number = phone)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(38.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CallForest)
                ) { Text("Lưu thay đổi", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Text("Hủy", color = CallForest, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clickable(onClick = onDismiss))
            }
        }
    }
}

@Composable
private fun CompactTextField(value: String, onValueChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().height(42.dp), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp))
}

@Composable
private fun MethodChip(label: String, selected: Boolean) {
    var isSelected by remember { mutableStateOf(selected) }
    Surface(
        color = if (isSelected) Color(0xFFE1F9EB) else Color.White,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CallForest else Color(0xFFD7DEDA)),
        modifier = Modifier.clickable { isSelected = !isSelected }
    ) {
        Text(label, color = CallForest, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp))
    }
}

@Composable
private fun LegacyDeleteContactDialog(contact: CallContact, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(20.dp)) {
                Text("Xóa liên hệ?", color = Color(0xFF24322F), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Bạn có chắc muốn xóa ${contact.fullName} khỏi danh sách gọi?", color = Color(0xFF66736F), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Hủy") }
                    androidx.compose.material3.Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) { Text("Xóa") }
                }
            }
        }
    }
}

@Composable
private fun BoxedInitials(initials: String) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun LegacyStyledContactEditDialog(contact: CallContact, onDismiss: () -> Unit, onSave: (CallContact) -> Unit) {
    var nickname by remember(contact) { mutableStateOf(contact.displayName) }
    var relation by remember(contact) { mutableStateOf(contact.relation) }
    var method by remember(contact) { mutableStateOf("Zalo") }
    var phone by remember(contact) { mutableStateOf(contact.number) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column(Modifier.padding(18.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
                    Text(
                        "Sửa đổi",
                        color = CallForest,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "×",
                        color = Color(0xFF66736F),
                        fontSize = 22.sp,
                        modifier = Modifier.align(Alignment.CenterEnd).clickable(onClick = onDismiss)
                    )
                }
                Spacer(Modifier.height(14.dp))
                PopupFieldLabel("Tên gọi nhớ")
                LargeCompactTextField(nickname) { nickname = it }
                PopupFieldLabel("Quan hệ", Modifier.padding(top = 10.dp))
                LargeCompactTextField(relation) { relation = it }
                PopupFieldLabel("Phương thức liên hệ", Modifier.padding(top = 10.dp))
                LargeCompactTextField(method) { method = it }
                PopupFieldLabel("Hình thức", Modifier.padding(top = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MethodChip("Video Call", true)
                    MethodChip("Gọi điện", false)
                }
                PopupFieldLabel("Số điện thoại", Modifier.padding(top = 10.dp))
                LargeCompactTextField(phone) { phone = it }
                androidx.compose.material3.Button(
                    onClick = { onSave(contact.copy(displayName = nickname, relation = relation, number = phone)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(44.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CallForest)
                ) { Text("Lưu thay đổi", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                Text(
                    "Hủy",
                    color = Color(0xFF1B4332),
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onDismiss),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PopupFieldLabel(label: String, modifier: Modifier = Modifier) {
    Text(label, color = Color(0xFF52615D), fontSize = 12.sp, modifier = modifier.padding(bottom = 5.dp))
}

@Composable
private fun LargeCompactTextField(value: String, onValueChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
    )
}

@Composable
private fun DeleteContactDialog(contact: CallContact, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Xóa liên hệ?",
                    color = Color(0xFF24322F),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Bạn có chắc muốn xóa ${contact.fullName} khỏi danh sách gọi?",
                    color = Color(0xFF66736F),
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 20.dp),
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Hủy", color = Color(0xFF1B4332), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    androidx.compose.material3.Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                    ) { Text("Xóa", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun LegacyFinalContactEditDialog(contact: CallContact, onDismiss: () -> Unit, onSave: (CallContact) -> Unit) {
    var nickname by remember(contact) { mutableStateOf(contact.displayName) }
    var relation by remember(contact) { mutableStateOf(contact.relation) }
    var method by remember(contact) { mutableStateOf("Zalo") }
    var phone by remember(contact) { mutableStateOf(contact.number) }
    var selectedCallType by remember(contact) { mutableStateOf("Video Call") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column(Modifier.padding(18.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
                    Text("Sửa đổi", color = CallForest, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text("×", color = Color(0xFF66736F), fontSize = 22.sp, modifier = Modifier.align(Alignment.CenterEnd).clickable(onClick = onDismiss))
                }
                Spacer(Modifier.height(14.dp))
                PopupFieldLabel("Tên gọi nhớ")
                LargeCompactTextField(nickname) { nickname = it }
                PopupFieldLabel("Quan hệ", Modifier.padding(top = 10.dp))
                LargeCompactTextField(relation) { relation = it }
                PopupFieldLabel("Phương thức liên hệ", Modifier.padding(top = 10.dp))
                LargeCompactTextField(method) { method = it }
                PopupFieldLabel("Hình thức", Modifier.padding(top = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExclusiveMethodChip("Video Call", selectedCallType == "Video Call") { selectedCallType = "Video Call" }
                    ExclusiveMethodChip("Gọi điện", selectedCallType == "Gọi điện") { selectedCallType = "Gọi điện" }
                }
                PopupFieldLabel("Số điện thoại", Modifier.padding(top = 10.dp))
                LargeCompactTextField(phone) { phone = it }
                androidx.compose.material3.Button(
                    onClick = { onSave(contact.copy(displayName = nickname, relation = relation, number = phone)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(44.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CallForest)
                ) { Text("Lưu thay đổi", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                Text("Hủy", color = Color(0xFF1B4332), fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onDismiss), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ExclusiveMethodChip(label: String, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        color = if (selected) Color(0xFFE1F9EB) else Color.White,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) CallForest else Color(0xFFD7DEDA)),
        modifier = Modifier.clickable(onClick = onSelect)
    ) {
        Text(label, color = CallForest, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp))
    }
}

@Composable
private fun LegacyAddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (nickname: String, relation: String, phone: String) -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCallType by remember { mutableStateOf("Video Call") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(18.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
                    Text("Thêm liên hệ", color = CallForest, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text("×", color = Color(0xFF66736F), fontSize = 22.sp, modifier = Modifier.align(Alignment.CenterEnd).clickable(onClick = onDismiss))
                }
                Spacer(Modifier.height(16.dp))
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(color = Color(0xFFF7FAF8), shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB8C9C1)), modifier = Modifier.size(66.dp)) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Text("Ảnh\nđại diện", color = Color(0xFF66736F), fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                PopupFieldLabel("Tên gọi nhớ")
                AddContactTextField(nickname, "Ví dụ: Thằng Tí, Con Lan...") { nickname = it }
                PopupFieldLabel("Quan hệ", Modifier.padding(top = 10.dp))
                AddContactTextField(relation, "Ví dụ: Con cái, Bạn bè...") { relation = it }
                PopupFieldLabel("Phương thức liên hệ", Modifier.padding(top = 10.dp))
                AddContactTextField(method, "Ví dụ: Zalo, Điện thoại") { method = it }
                PopupFieldLabel("Hình thức", Modifier.padding(top = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExclusiveMethodChip("Video Call", selectedCallType == "Video Call") { selectedCallType = "Video Call" }
                    ExclusiveMethodChip("Gọi điện", selectedCallType == "Gọi điện") { selectedCallType = "Gọi điện" }
                }
                PopupFieldLabel("Số điện thoại", Modifier.padding(top = 10.dp))
                AddContactTextField(phone, "Nhập số điện thoại") { phone = it }
                androidx.compose.material3.Button(
                    onClick = { onAdd(nickname.ifBlank { "Liên hệ mới" }, relation.ifBlank { "Khác" }, phone) },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(46.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CallForest)
                ) { Text("Thêm vào danh sách", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                Text("Hủy", color = Color(0xFF1B4332), fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onDismiss), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AddContactTextField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 13.sp, color = Color(0xFF87918E)) },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
    )
}

@Composable
private fun LegacyContactAvatarPicker() {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            color = Color(0xFFF7FAF8),
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB8C9C1)),
            modifier = Modifier.size(58.dp)
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PersonOutline, contentDescription = "Ảnh đại diện", tint = Color(0xFF66736F), modifier = Modifier.size(28.dp))
            }
        }
        Surface(
            color = CallForest,
            shape = CircleShape,
            modifier = Modifier.size(21.dp).align(Alignment.BottomCenter).padding(1.dp)
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = "Đổi ảnh đại diện", tint = Color.White, modifier = Modifier.padding(4.dp))
        }
    }
}

@Composable
private fun ContactAvatarPicker() {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Box(Modifier.size(58.dp)) {
            Surface(
                color = Color(0xFFF7FAF8),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB8C9C1)),
                modifier = Modifier.fillMaxSize()
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PersonOutline, contentDescription = "Ảnh đại diện", tint = Color(0xFF66736F), modifier = Modifier.size(28.dp))
                }
            }
            Surface(color = CallForest, shape = CircleShape, modifier = Modifier.size(21.dp).align(Alignment.BottomEnd)) {
                Icon(Icons.Outlined.Edit, contentDescription = "Đổi ảnh đại diện", tint = Color.White, modifier = Modifier.padding(4.dp))
            }
        }
    }
}

@Composable
private fun ContactEditDialog(contact: CallContact, onDismiss: () -> Unit, onSave: (CallContact) -> Unit) {
    var nickname by remember(contact) { mutableStateOf(contact.displayName) }
    var relation by remember(contact) { mutableStateOf(contact.relation) }
    var method by remember(contact) { mutableStateOf("Zalo") }
    var phone by remember(contact) { mutableStateOf(contact.number) }
    var selectedCallType by remember(contact) { mutableStateOf("Video Call") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(16.dp)) {
                PopupDialogTitle("Sửa đổi", onDismiss)
                Spacer(Modifier.height(8.dp))
                ContactAvatarPicker()
                Spacer(Modifier.height(10.dp))
                PopupFieldLabel("Tên gọi nhớ")
                LargeCompactTextField(nickname) { nickname = it }
                PopupFieldLabel("Quan hệ", Modifier.padding(top = 8.dp))
                LargeCompactTextField(relation) { relation = it }
                PopupFieldLabel("Phương thức liên hệ", Modifier.padding(top = 8.dp))
                LargeCompactTextField(method) { method = it }
                PopupFieldLabel("Hình thức", Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExclusiveMethodChip("Video Call", selectedCallType == "Video Call") { selectedCallType = "Video Call" }
                    ExclusiveMethodChip("Gọi điện", selectedCallType == "Gọi điện") { selectedCallType = "Gọi điện" }
                }
                PopupFieldLabel("Số điện thoại", Modifier.padding(top = 8.dp))
                LargeCompactTextField(phone) { phone = it }
                PopupSaveButton("Lưu thay đổi") { onSave(contact.copy(displayName = nickname, relation = relation, number = phone)) }
                PopupCancel(onDismiss)
            }
        }
    }
}

@Composable
private fun AddContactDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCallType by remember { mutableStateOf("Video Call") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(16.dp)) {
                PopupDialogTitle("Thêm liên hệ", onDismiss)
                Spacer(Modifier.height(8.dp))
                ContactAvatarPicker()
                Spacer(Modifier.height(10.dp))
                PopupFieldLabel("Tên gọi nhớ")
                CompactAddField(nickname, "Ví dụ: Thằng Tí, Con Lan...") { nickname = it }
                PopupFieldLabel("Quan hệ", Modifier.padding(top = 8.dp))
                CompactAddField(relation, "Ví dụ: Con cái, Bạn bè...") { relation = it }
                PopupFieldLabel("Phương thức liên hệ", Modifier.padding(top = 8.dp))
                CompactAddField(method, "Ví dụ: Zalo, Điện thoại") { method = it }
                PopupFieldLabel("Hình thức", Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExclusiveMethodChip("Video Call", selectedCallType == "Video Call") { selectedCallType = "Video Call" }
                    ExclusiveMethodChip("Gọi điện", selectedCallType == "Gọi điện") { selectedCallType = "Gọi điện" }
                }
                PopupFieldLabel("Số điện thoại", Modifier.padding(top = 8.dp))
                CompactAddField(phone, "Nhập số điện thoại") { phone = it }
                PopupSaveButton("Thêm vào danh sách") { onAdd(nickname.ifBlank { "Liên hệ mới" }, relation.ifBlank { "Khác" }, phone) }
                PopupCancel(onDismiss)
            }
        }
    }
}

@Composable
private fun PopupDialogTitle(title: String, onDismiss: () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
        Text(title, color = CallForest, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("×", color = Color(0xFF66736F), fontSize = 22.sp, modifier = Modifier.align(Alignment.CenterEnd).clickable(onClick = onDismiss))
    }
}

@Composable
private fun CompactAddField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF77807D)),
        modifier = Modifier.fillMaxWidth().height(44.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color(0xFF24322F)),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            decorationBox = { innerTextField ->
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(placeholder, fontSize = 12.sp, color = Color(0xFF87918E))
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun PopupSaveButton(label: String, onClick: () -> Unit) {
    androidx.compose.material3.Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(42.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CallForest)) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PopupCancel(onDismiss: () -> Unit) {
    Text("Hủy", color = Color(0xFF1B4332), fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clickable(onClick = onDismiss), textAlign = TextAlign.Center)
}
