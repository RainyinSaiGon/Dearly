package com.dearly.app.ui.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dearly.app.R

private val Forest = Color(0xFF174D3B)
private val ForestSoft = Color(0xFF326854)
private val ScreenBackground = Color(0xFFFCFCF9)
private val MutedText = Color(0xFF7D8B90)
private val Mint = Color(0xFFA7F1CD)
private val MintPale = Color(0xFFE1F9EB)

@Composable
fun CaregiverActivityScreen(onOpenCalls: () -> Unit = {}, onOpenMedications: () -> Unit = {}, onOpenSettings: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        CaregiverHeader()
        ElderSummary()
        ActivityContent(modifier = Modifier.weight(1f))
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
private fun LegacyTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(Color.White)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_dearly_mark),
            contentDescription = "Dearly",
            modifier = Modifier.size(26.dp),
            tint = Color.Unspecified
        )
        Spacer(Modifier.width(7.dp))
        Text("Dearly", color = Forest, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.weight(1f))
        Surface(color = Color(0xFFF4F5F2), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(28.dp)) {
            Icon(Icons.Outlined.PersonOutline, contentDescription = "Hồ sơ", modifier = Modifier.padding(6.dp), tint = Color(0xFF3A4845))
        }
    }
}

@Composable
private fun ElderSummary() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 25.dp, bottomEnd = 25.dp))
            .background(Forest)
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 17.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ba An", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("76 tuổi · Hà Nội", color = Color(0xFFD0DFD7), fontSize = 12.sp)
            }
            Surface(color = ForestSoft, shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(Color(0xFF5BE294)))
                    Spacer(Modifier.width(4.dp))
                    Text("Bình thường", color = Color.White, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(15.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryMetric("2", "Cuộc gọi", Modifier.weight(1f))
            SummaryMetric("3/5", "Đúng thuốc", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ForestSoft),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFFD2DED8), fontSize = 11.sp)
    }
}

@Composable
private fun ActivityContent(modifier: Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
        SectionTitle("Nhật ký cuộc gọi", "27/7/2026")
        CallRow("Nguyễn Thị Lợi (Bạn bè)", "10:15 AM · 5 phút", Icons.Outlined.Done)
        CallRow("Trần Thu Lan (Con gái)", "08:30 AM · 12 phút", Icons.Outlined.TrendingUp)
        Spacer(Modifier.height(18.dp))
        SectionTitle("Thuốc hôm nay", "27/7/2026")
        MedicationRow("07:00", "Thuốc huyết áp", true)
        MedicationRow("07:00", "Thuốc tiêu đường", true)
        MedicationRow("13:00", "Thuốc tiểu đường", true)
        MedicationRow("19:00", "Thuốc huyết áp", false)
        MedicationRow("19:00", "Thuốc tiểu đường", false)
    }
}

@Composable
private fun SectionTitle(title: String, date: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Forest, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(date, color = MutedText, fontSize = 11.sp)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun CallRow(name: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, color = Color(0xFF283533), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = MutedText, fontSize = 11.sp)
        }
        Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun MedicationRow(time: String, name: String, completed: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(time, color = MutedText, fontSize = 11.sp, modifier = Modifier.width(38.dp))
        Text(name, color = Color(0xFF33403D), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Surface(color = if (completed) MintPale else Color(0xFFF3F5F6), shape = RoundedCornerShape(6.dp)) {
            Text(if (completed) "✓ Đã nhắc" else "Chưa", color = if (completed) Color(0xFF169B61) else MutedText, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
        }
    }
}

@Composable
private fun LegacyBottomNavigation() {
    Surface(shadowElevation = 8.dp, color = Color.White) {
        Row(
            modifier = Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 5.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(Icons.Outlined.Home, "Hoạt động", selected = true)
            NavItem(Icons.Outlined.Call, "Gọi điện")
            NavItem(Icons.Outlined.CalendarToday, "Lịch thuốc")
            NavItem(Icons.Outlined.Settings, "Cài đặt")
        }
    }
}

@Composable
private fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean = false) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Mint else Color.Transparent)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Forest else Color(0xFF34423F), modifier = Modifier.size(24.dp))
        Text(label, color = if (selected) Forest else Color(0xFF34423F), fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
