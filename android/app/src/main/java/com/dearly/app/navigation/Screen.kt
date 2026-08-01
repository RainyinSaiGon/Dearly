package com.dearly.app.navigation

sealed class Screen(val route: String) {
    object PhoneAuth : Screen("phone_auth")
    object OtpVerification : Screen("otp_verification/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "otp_verification/$phoneNumber"
    }
    object RoleSelection : Screen("role_selection")
    object VoiceEnrollment : Screen("voice_enrollment")
    
    // Elder Screens
    object ElderHome : Screen("elder_home")
    object ElderContacts : Screen("elder_contacts")
    object ElderMedication : Screen("elder_medication")
    object ElderSettings : Screen("elder_settings")
    object VoiceAssistantOverlay : Screen("voice_assistant_overlay")

    // Caregiver Screens
    object CaregiverDashboard : Screen("caregiver_dashboard")
    object AddContact : Screen("add_contact")
    object AddMedication : Screen("add_medication")
    object ElderDetail : Screen("elder_detail/{elderId}") {
        fun createRoute(elderId: String) = "elder_detail/$elderId"
    }
}
