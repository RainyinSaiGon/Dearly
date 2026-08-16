package com.dearly.app.ui.elder

import androidx.compose.runtime.Composable
import com.dearly.app.ui.caregiver.CaregiverSettingsScreen
import java.io.File

@Composable
fun ElderSettingScreen(
    displayName: String = "Dearly user",
    phoneNumber: String = "",
    onOpenCalls: () -> Unit = {},
    onOpenMedications: () -> Unit = {},
    onLogout: () -> Unit = {},
    onRoleChanged: (String) -> Unit = {},
    linkCode: String? = null,
    linkCodeExpiresAt: String? = null,
    linkBusy: Boolean = false,
    linkError: String? = null,
    voiceEnrollmentCount: Int = 0,
    voiceMessage: String? = null,
    onGenerateLinkCode: () -> Unit = {},
    onEnrollVoice: (File) -> Unit = {}
) {
    CaregiverSettingsScreen(
        displayName = displayName,
        phoneNumber = phoneNumber,
        onOpenCalls = onOpenCalls,
        onOpenMedications = onOpenMedications,
        onLogout = onLogout,
        elderMode = true,
        onRoleChanged = onRoleChanged,
        initialAccountRole = "Người được chăm sóc",
        linkCode = linkCode,
        linkCodeExpiresAt = linkCodeExpiresAt,
        linkBusy = linkBusy,
        linkError = linkError,
        voiceEnrollmentCount = voiceEnrollmentCount,
        voiceMessage = voiceMessage,
        onGenerateLinkCode = onGenerateLinkCode,
        onEnrollVoice = onEnrollVoice
    )
}
