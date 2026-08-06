package com.dearly.app.ui.elder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ElderForest = Color(0xFF174D3B)
enum class ElderTab { CALLS, MEDICATIONS, SETTINGS }

@Composable
fun ElderFooter(selectedTab: ElderTab, onOpenCalls: () -> Unit, onOpenMedications: () -> Unit, onOpenSettings: () -> Unit) {
    Surface(shadowElevation = 8.dp, color = Color.White) {
        Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            ElderFooterItem(Icons.Outlined.Call, "Gọi điện", ElderTab.CALLS, selectedTab, onOpenCalls)
            ElderFooterItem(Icons.Outlined.CalendarToday, "Lịch thuốc", ElderTab.MEDICATIONS, selectedTab, onOpenMedications)
            ElderFooterItem(Icons.Outlined.Settings, "Cài đặt", ElderTab.SETTINGS, selectedTab, onOpenSettings)
        }
    }
}

@Composable
private fun ElderFooterItem(icon: ImageVector, label: String, tab: ElderTab, selected: ElderTab, onClick: () -> Unit) {
    val active = tab == selected
    Column(Modifier.width(86.dp).clip(RoundedCornerShape(18.dp)).background(if (active) Color(0xFFA7F1CD) else Color.Transparent).clickable(onClick = onClick).padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = if (active) ElderForest else Color(0xFF34423F), modifier = Modifier.size(24.dp))
        Text(label, color = if (active) ElderForest else Color(0xFF34423F), fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
    }
}
