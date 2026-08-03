package com.dearly.app.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.telephony.PhoneNumberUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import com.dearly.app.domain.model.UserRole
import com.dearly.app.domain.model.Contact
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
    val context = LocalContext.current
    val activity = context as? Activity
    val placeCall: (Contact) -> Unit = { contact ->
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(contact.phoneNumber)}")))
    }
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Flow
        composable(Screen.PhoneAuth.route) {
            PhoneAuthScreen(
                onSendOtpClicked = { phone, onCodeSent, onError ->
                    if (activity == null || !PhoneNumberUtils.isGlobalPhoneNumber(phone)) {
                        onError("Hãy nhập số điện thoại quốc tế hợp lệ, ví dụ +84912345678.")
                    } else {
                        try {
                            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                    FirebaseAuth.getInstance().signInWithCredential(credential)
                                        .addOnSuccessListener {
                                            navController.navigate(Screen.RoleSelection.route) {
                                                popUpTo(Screen.PhoneAuth.route) { inclusive = true }
                                            }
                                        }
                                        .addOnFailureListener { onError(it.localizedMessage ?: "Không thể xác thực số điện thoại.") }
                                }

                                override fun onVerificationFailed(exception: FirebaseException) {
                                    onError(exception.localizedMessage ?: "Không thể gửi mã OTP.")
                                }

                                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                                    onCodeSent(verificationId)
                                    navController.navigate(Screen.OtpVerification.createRoute(phone, verificationId))
                                }
                            }
                            PhoneAuthProvider.verifyPhoneNumber(
                                PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                                    .setPhoneNumber(phone)
                                    .setTimeout(60L, TimeUnit.SECONDS)
                                    .setActivity(activity)
                                    .setCallbacks(callbacks)
                                    .build()
                            )
                        } catch (exception: Exception) {
                            onError("Firebase chưa được cấu hình: ${exception.localizedMessage ?: "không thể gửi OTP"}")
                        }
                    }
                },
                onGoogleSignInClicked = { idToken, onSuccess, onError ->
                    FirebaseAuth.getInstance()
                        .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                        .addOnSuccessListener {
                            onSuccess()
                            navController.navigate(Screen.RoleSelection.route) {
                                popUpTo(Screen.PhoneAuth.route) { inclusive = true }
                            }
                        }
                        .addOnFailureListener { onError(it.localizedMessage ?: "Không thể đăng nhập bằng Google.") }
                }
            )
        }

        composable(Screen.OtpVerification.route) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
            OtpScreen(
                phoneNumber = phone,
                onVerifyOtp = { otp, onSuccess, onError ->
                    if (verificationId.isBlank()) {
                        onError("Phiên xác thực đã hết hạn. Hãy yêu cầu mã mới.")
                    } else {
                        FirebaseAuth.getInstance()
                            .signInWithCredential(PhoneAuthProvider.getCredential(verificationId, otp))
                            .addOnSuccessListener {
                                onSuccess()
                                navController.navigate(Screen.RoleSelection.route) {
                                    popUpTo(Screen.PhoneAuth.route) { inclusive = true }
                                }
                            }
                            .addOnFailureListener { onError(it.localizedMessage ?: "Mã OTP không hợp lệ.") }
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
                onCallClicked = placeCall
            )
        }

        composable(Screen.ElderMedication.route) {
            ElderMedicationScreen(
                onMarkAsTaken = { /* Repository persistence is introduced with the medication data layer. */ }
            )
        }

        composable(Screen.ElderSettings.route) {
            ElderSettingsScreen(
                onReEnrollVoiceClicked = { navController.navigate(Screen.VoiceEnrollment.route) },
                onSignOutClicked = {
                    FirebaseAuth.getInstance().signOut()
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
                    FirebaseAuth.getInstance().signOut()
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
