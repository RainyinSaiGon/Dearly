package com.dearly.app.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object PhoneAuth : Screen("phone_auth")
    object SignUp : Screen("sign_up")
    object OtpVerification : Screen("otp_verification")
    object Welcome : Screen("welcome")
    object Welcome1 : Screen("welcome_1")
    object Welcome2 : Screen("welcome_2")
    object RoleSelection : Screen("role_selection")
    object CaregiverActivity : Screen("caregiver_activity")
    object CaregiverCalls : Screen("caregiver_calls")
    object CaregiverMedications : Screen("caregiver_medications")
    object CaregiverSettings : Screen("caregiver_settings")
}
