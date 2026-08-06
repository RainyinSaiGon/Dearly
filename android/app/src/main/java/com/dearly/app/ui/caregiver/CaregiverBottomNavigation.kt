package com.dearly.app.ui.caregiver

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
import androidx.compose.material.icons.outlined.Home
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

enum class CaregiverTab { ACTIVITY, CALLS, MEDICATIONS, SETTINGS }

@Composable
fun CaregiverBottomNavigation(
    selectedTab: CaregiverTab,
    onTabSelected: (CaregiverTab) -> Unit = {}
) {
    Surface(shadowElevation = 8.dp, color = Color.White) {
        Row(
            modifier = Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 5.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CaregiverNavItem(Icons.Outlined.Home, "Hoạt động", CaregiverTab.ACTIVITY, selectedTab, onTabSelected)
            CaregiverNavItem(Icons.Outlined.Call, "Gọi điện", CaregiverTab.CALLS, selectedTab, onTabSelected)
            CaregiverNavItem(Icons.Outlined.CalendarToday, "Lịch thuốc", CaregiverTab.MEDICATIONS, selectedTab, onTabSelected)
            CaregiverNavItem(Icons.Outlined.Settings, "Cài đặt", CaregiverTab.SETTINGS, selectedTab, onTabSelected)
        }
    }
}

@Composable
private fun CaregiverNavItem(
    icon: ImageVector,
    label: String,
    tab: CaregiverTab,
    selectedTab: CaregiverTab,
    onTabSelected: (CaregiverTab) -> Unit
) {
    val selected = tab == selectedTab
    val selectedColor = Color(0xFF174D3B)
    val contentColor = if (selected) selectedColor else Color(0xFF34423F)

    Column(
        modifier = Modifier
            .width(76.dp)
            .clickable { onTabSelected(tab) }
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFFA7F1CD) else Color.Transparent)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(24.dp))
        Text(label, color = contentColor, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
