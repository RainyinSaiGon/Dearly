package com.dearly.app.ui.elder

import androidx.compose.runtime.Composable
import com.dearly.app.ui.caregiver.CaregiverSettingsScreen

@Composable
fun ElderSettingScreen(onOpenCalls: () -> Unit = {}, onOpenMedications: () -> Unit = {}, onLogout: () -> Unit = {}, onRoleChanged: (String) -> Unit = {}) {
    CaregiverSettingsScreen(
        onOpenCalls = onOpenCalls,
        onOpenMedications = onOpenMedications,
        onLogout = onLogout,
        elderMode = true,
        onRoleChanged = onRoleChanged,
        initialAccountRole = "Người được chăm sóc"
    )
}
