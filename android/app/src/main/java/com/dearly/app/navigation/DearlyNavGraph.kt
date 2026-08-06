package com.dearly.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dearly.app.ui.auth.OnboardingScreen
import com.dearly.app.ui.auth.OtpVerificationScreen
import com.dearly.app.ui.auth.SignInScreen
import com.dearly.app.ui.auth.RoleSelectionScreen
import com.dearly.app.ui.auth.SignUpScreen
import com.dearly.app.ui.auth.WelcomeScreen
import com.dearly.app.ui.auth.WelcomeScreen1
import com.dearly.app.ui.auth.WelcomeScreen2
import com.dearly.app.ui.caregiver.CaregiverActivityScreen
import com.dearly.app.ui.caregiver.CaregiverCallScreen
import com.dearly.app.ui.caregiver.CaregiverMedicationScreen
import com.dearly.app.ui.caregiver.CaregiverSettingsScreen
import com.dearly.app.domain.model.UserRole

@Composable
fun DearlyNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Onboarding.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Authentication UI is enabled; backend verification remains disabled.
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.PhoneAuth.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PhoneAuth.route) {
            SignInScreen(
                onContinue = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.PhoneAuth.route) { inclusive = true }
                    }
                },
                onSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(Screen.OtpVerification.route)
                },
                onSignIn = { navController.popBackStack() }
            )
        }

        composable(Screen.OtpVerification.route) {
            OtpVerificationScreen(
                onContinue = { navController.navigate(Screen.Welcome.route) }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onContinue = { navController.navigate(Screen.Welcome1.route) }
            )
        }

        composable(Screen.Welcome1.route) {
            WelcomeScreen1(
                onContinue = { navController.navigate(Screen.Welcome2.route) },
                onSkip = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome2.route) {
            WelcomeScreen2(
                onContinue = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        /*
         * Backend authentication is intentionally disabled for local UI development.
         * Keep this flow here to restore Firebase phone/Google sign-in later.
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
        */

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    if (role == UserRole.CAREGIVER) {
                        navController.navigate(Screen.CaregiverActivity.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CaregiverActivity.route) {
            CaregiverActivityScreen(
                onOpenCalls = { navController.navigate(Screen.CaregiverCalls.route) },
                onOpenMedications = { navController.navigate(Screen.CaregiverMedications.route) }, onOpenSettings = { navController.navigate(Screen.CaregiverSettings.route) }
            )
        }
        composable(Screen.CaregiverCalls.route) {
            CaregiverCallScreen(
                onOpenActivity = { navController.popBackStack() },
                onOpenMedications = { navController.navigate(Screen.CaregiverMedications.route) }, onOpenSettings = { navController.navigate(Screen.CaregiverSettings.route) }
            )
        }
        composable(Screen.CaregiverMedications.route) {
            CaregiverMedicationScreen(
                onOpenActivity = { navController.navigate(Screen.CaregiverActivity.route) },
                onOpenCalls = { navController.navigate(Screen.CaregiverCalls.route) }, onOpenSettings = { navController.navigate(Screen.CaregiverSettings.route) }
            )
        }
        composable(Screen.CaregiverSettings.route) { CaregiverSettingsScreen() }
    }
}
