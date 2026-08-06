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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Support
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SettingsForest = Color(0xFF174D3B)

@Composable
fun CaregiverSettingsScreen(
    onOpenActivity: () -> Unit = {},
    onOpenCalls: () -> Unit = {},
    onOpenMedications: () -> Unit = {}
) {
    var editProfile by remember { mutableStateOf(false) }
    var savedRole by remember { mutableStateOf("Bác") }
    var savedVoice by remember { mutableStateOf("Nam miền Bắc") }
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCF9))) {
        CaregiverHeader()
        if (editProfile) {
            ProfileEditor(
                modifier = Modifier.weight(1f),
                initialRole = savedRole,
                initialVoice = savedVoice,
                onSave = { role, voice ->
                    savedRole = role
                    savedVoice = voice
                    editProfile = false
                },
                onCancel = { editProfile = false }
            )
        } else {
            SettingsContent(Modifier.weight(1f)) { editProfile = true }
        }
        if (!editProfile) {
            CaregiverBottomNavigation(
                selectedTab = CaregiverTab.SETTINGS,
                onTabSelected = {
                    if (it == CaregiverTab.ACTIVITY) onOpenActivity()
                    if (it == CaregiverTab.CALLS) onOpenCalls()
                    if (it == CaregiverTab.MEDICATIONS) onOpenMedications()
                }
            )
        }
    }
}

@Composable
private fun SettingsContent(modifier: Modifier, onEdit: () -> Unit) {
    Column(modifier.padding(18.dp)) {
        Surface(color = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 1.dp) {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(editable = false)
                Spacer(Modifier.height(10.dp))
                Text("Nguyễn Văn An", color = SettingsForest, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("090 123 4567", color = Color(0xFF788387), fontSize = 15.sp)
                Button(onClick = onEdit, modifier = Modifier.padding(top = 12.dp).height(40.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA7F1CD), contentColor = SettingsForest)) { Text("Chỉnh sửa", fontSize = 15.sp) }
            }
        }
        Text("TÙY CHỌN", color = Color(0xFF66736F), fontSize = 11.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        Surface(color = Color.White, shape = RoundedCornerShape(10.dp)) {
            Column { SettingRow(Icons.Outlined.PersonOutline, "Cài đặt tài khoản"); SettingRow(Icons.Outlined.Notifications, "Thông báo"); SettingRow(Icons.Outlined.Shield, "Bảo mật"); SettingRow(Icons.Outlined.Language, "Ngôn ngữ", "Tiếng Việt"); SettingRow(Icons.Outlined.Support, "Hỗ trợ") }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF6F4), contentColor = Color(0xFFD22C2C))) { Text("Đăng xuất", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth().clickable { }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Color(0xFFF1F5F2), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(30.dp)) { Icon(icon, null, tint = SettingsForest, modifier = Modifier.padding(7.dp)) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, color = Color(0xFF33403D), fontSize = 14.sp); if (subtitle != null) Text(subtitle, color = Color(0xFF7D8B90), fontSize = 11.sp) }
        Icon(Icons.Outlined.ChevronRight, null, tint = Color(0xFF7D8B90))
    }
}

@Composable
private fun ProfileEditor(
    modifier: Modifier,
    initialRole: String,
    initialVoice: String,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var role by remember(initialRole) { mutableStateOf(initialRole) }
    var voice by remember(initialVoice) { mutableStateOf(initialVoice) }
    Column(modifier.padding(22.dp)) {
        Spacer(Modifier.height(18.dp))
        Avatar(editable = true, avatarSize = 112.dp)
        Text("Xưng hô với ba/mẹ", color = Color(0xFF33403D), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 34.dp))
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf("Bác", "Ông", "Ba/Mẹ").forEach { item -> Choice(item, role == item) { role = item } } }
        Text("Giọng đọc", color = Color(0xFF33403D), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
        listOf("Nữ miền Bắc", "Nam miền Bắc", "Nữ miền Nam", "Nam miền Nam").forEach { item -> Choice(item, voice == item, Modifier.fillMaxWidth().padding(vertical = 4.dp)) { voice = item } }
        Spacer(Modifier.weight(1f))
        Button(onClick = { onSave(role, voice) }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = SettingsForest)) { Text("Lưu thông tin", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        Text("Hủy", color = SettingsForest, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onCancel), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun Avatar(editable: Boolean, avatarSize: androidx.compose.ui.unit.Dp = 78.dp) { androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { androidx.compose.foundation.layout.Box(Modifier.size(avatarSize)) { Surface(color = Color(0xFFE8EBE9), shape = CircleShape, modifier = Modifier.fillMaxSize()) { Icon(Icons.Outlined.PersonOutline, null, tint = Color(0xFF404946), modifier = Modifier.padding(if (editable) 30.dp else 22.dp)) }; if (editable) Surface(color = SettingsForest, shape = CircleShape, modifier = Modifier.size(26.dp).align(Alignment.BottomEnd)) { Icon(Icons.Outlined.Edit, null, tint = Color.White, modifier = Modifier.padding(6.dp)) } } } }

@Composable
private fun Choice(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) { Surface(color = if (selected) Color(0xFFE1F0E9) else Color.White, shape = RoundedCornerShape(9.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) SettingsForest else Color(0xFFE2E6E3)), modifier = modifier.clickable(onClick = onClick)) { Text(label, color = Color(0xFF33403D), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp)) } }
