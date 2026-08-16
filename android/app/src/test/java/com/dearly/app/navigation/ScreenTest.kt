package com.dearly.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTest {
    @Test
    fun routesAreStableAndUnique() {
        val routes = listOf(
            Screen.Onboarding.route,
            Screen.PhoneAuth.route,
            Screen.SignUp.route,
            Screen.OtpVerification.route,
            Screen.Welcome.route,
            Screen.Welcome1.route,
            Screen.Welcome2.route,
            Screen.RoleSelection.route,
            Screen.CaregiverActivity.route,
            Screen.CaregiverCalls.route,
            Screen.CaregiverMedications.route,
            Screen.CaregiverSettings.route,
            Screen.ElderCalls.route,
            Screen.ElderMedications.route,
            Screen.ElderSettings.route
        )

        assertEquals(routes.size, routes.toSet().size)
        assertEquals("onboarding", Screen.Onboarding.route)
        assertEquals("caregiver_activity", Screen.CaregiverActivity.route)
        assertEquals("elder_calls", Screen.ElderCalls.route)
        assertEquals("otp_verification/id%2Fwith%20spaces", Screen.OtpVerification.createRoute("id/with spaces"))
    }
}
