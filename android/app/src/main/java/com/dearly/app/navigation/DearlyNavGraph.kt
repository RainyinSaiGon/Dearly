package com.dearly.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dearly.app.domain.model.UserRole
import com.dearly.app.ui.auth.OtpScreen
import com.dearly.app.ui.auth.PhoneAuthScreen
import com.dearly.app.ui.auth.RoleSelectionScreen
import com.dearly.app.ui.caregiver.AddContactScreen
import com.dearly.app.ui.caregiver.AddMedicationScreen
import com.dearly.app.ui.caregiver.CaregiverDashboardScreen
import com.dearly.app.ui.elder.ElderContactsScreen
import com.dearly.app.ui.elder.ElderHomeScreen
import com.dearly.app.ui.elder.ElderMedicationScreen
import com.dearly.app.ui.elder.ElderSettingsScreen
import com.dearly.app.ui.voice.VoiceAssistantOverlay
import com.dearly.app.ui.voice.VoiceEnrollmentScreen

@Composable
fun DearlyNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.PhoneAuth.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Flow
        composable(Screen.PhoneAuth.route) {
            PhoneAuthScreen(
                onSendOtpClicked = { phone ->
                    navController.navigate(Screen.OtpVerification.createRoute(phone))
                },
                onGoogleSignInClicked = {
                    navController.navigate(Screen.RoleSelection.route)
                }
            )
        }

        composable(Screen.OtpVerification.route) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            OtpScreen(
                phoneNumber = phone,
                onVerifySuccess = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.PhoneAuth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    if (role == UserRole.ELDER) {
                        navController.navigate(Screen.VoiceEnrollment.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.CaregiverDashboard.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.VoiceEnrollment.route) {
            VoiceEnrollmentScreen(
                onEnrollmentComplete = {
                    navController.navigate(Screen.ElderHome.route) {
                        popUpTo(Screen.VoiceEnrollment.route) { inclusive = true }
                    }
                }
            )
        }

        // Elder Navigation Flow
        composable(Screen.ElderHome.route) {
            ElderHomeScreen(
                onSpeakClicked = {
                    navController.navigate(Screen.VoiceAssistantOverlay.route)
                },
                onNavigateToContacts = { navController.navigate(Screen.ElderContacts.route) },
                onNavigateToMedication = { navController.navigate(Screen.ElderMedication.route) },
                onNavigateToSettings = { navController.navigate(Screen.ElderSettings.route) }
            )
        }

        composable(Screen.ElderContacts.route) {
            ElderContactsScreen(
                onNavigateBack = { navController.popBackStack() },
                onCallClicked = { contact -> /* Trigger Call */ }
            )
        }

        composable(Screen.ElderMedication.route) {
            ElderMedicationScreen(
                onMarkAsTaken = { log -> /* Mark log taken */ }
            )
        }

        composable(Screen.ElderSettings.route) {
            ElderSettingsScreen(
                onReEnrollVoiceClicked = { navController.navigate(Screen.VoiceEnrollment.route) },
                onSignOutClicked = {
                    navController.navigate(Screen.PhoneAuth.route) {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(Screen.VoiceAssistantOverlay.route) {
            VoiceAssistantOverlay(
                onDismiss = { navController.popBackStack() }
            )
        }

        // Caregiver Navigation Flow
        composable(Screen.CaregiverDashboard.route) {
            CaregiverDashboardScreen(
                onNavigateToAddContact = { navController.navigate(Screen.AddContact.route) },
                onNavigateToAddMedication = { navController.navigate(Screen.AddMedication.route) },
                onSignOut = {
                    navController.navigate(Screen.PhoneAuth.route) {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(Screen.AddContact.route) {
            AddContactScreen(
                onNavigateBack = { navController.popBackStack() },
                onContactSaved = { navController.popBackStack() }
            )
        }

        composable(Screen.AddMedication.route) {
            AddMedicationScreen(
                onNavigateBack = { navController.popBackStack() },
                onMedicationSaved = { navController.popBackStack() }
            )
        }
    }
}
