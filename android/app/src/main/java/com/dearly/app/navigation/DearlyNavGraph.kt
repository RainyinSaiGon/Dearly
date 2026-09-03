package com.dearly.app.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dearly.app.ui.auth.OnboardingScreen
import com.dearly.app.ui.auth.OtpVerificationScreen
import com.dearly.app.ui.auth.PublicAssistantScreen
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
import com.dearly.app.ui.elder.ElderCallingScreen
import com.dearly.app.ui.elder.ElderMedicationScreen
import com.dearly.app.ui.elder.ElderSettingScreen
import com.dearly.app.ui.DearlyViewModel
import com.dearly.app.domain.PhoneNumberNormalizer
import com.dearly.app.domain.model.UserRole
import com.dearly.app.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import java.util.concurrent.TimeUnit

@Composable
fun DearlyNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Onboarding.route,
    viewModel: DearlyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val medicationLogs by viewModel.medicationLogs.collectAsState()
    val linkedElders by viewModel.linkedElders.collectAsState()
    val context = LocalContext.current
    val activity = context.findActivity()
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    var authenticationBusy by remember { mutableStateOf(false) }
    var authenticationError by remember { mutableStateOf<String?>(null) }
    var pendingDisplayName by remember { mutableStateOf("") }
    var pendingE164PhoneNumber by remember { mutableStateOf<String?>(null) }
    var forceResendingToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    val navigateToHome: (UserRole) -> Unit = { role ->
        val destination = if (role == UserRole.CAREGIVER) {
            Screen.CaregiverActivity.route
        } else {
            Screen.ElderCalls.route
        }
        navController.navigate(destination) {
            popUpTo(Screen.PublicAssistant.route) { inclusive = true }
        }
    }
    val authenticatedDestination = {
        authenticationBusy = true
        viewModel.resumeBackendSession(
            onExistingAccount = { role ->
                authenticationBusy = false
                navigateToHome(role)
            },
            onRoleSelectionRequired = {
                authenticationBusy = false
                navController.navigate(Screen.RoleSelection.route) {
                    popUpTo(Screen.PublicAssistant.route) { inclusive = true }
                }
            },
            onFailure = {
                authenticationBusy = false
                authenticationError = "Không thể mở tài khoản lúc này. Bác hãy thử lại nhé."
            }
        )
    }
    val signInWithCredential: (PhoneAuthCredential) -> Unit = { credential ->
        authenticationBusy = true
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val displayName = pendingDisplayName.trim()
                if (displayName.isBlank()) {
                    authenticatedDestination()
                } else {
                    result.user?.updateProfile(
                        UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
                    )?.addOnCompleteListener { authenticatedDestination() }
                        ?: authenticatedDestination()
                }
            }
            .addOnFailureListener { error ->
                authenticationBusy = false
                authenticationError = error.localizedMessage ?: "Không thể xác thực tài khoản."
            }
    }
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            authenticationBusy = true
            firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener { authenticatedDestination() }
                .addOnFailureListener { error ->
                    authenticationBusy = false
                    authenticationError = error.localizedMessage ?: "Không thể đăng nhập bằng Google."
                }
        } catch (error: ApiException) {
            authenticationBusy = false
            authenticationError = error.localizedMessage ?: "Đăng nhập Google đã bị hủy."
        }
    }
    val startGoogleSignIn = {
        authenticationError = null
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleLauncher.launch(GoogleSignIn.getClient(context, options).signInIntent)
    }
    val requestPhoneVerification: (String, PhoneAuthProvider.ForceResendingToken?, Boolean) -> Unit = request@{
            e164PhoneNumber, resendToken, navigateToOtp ->
        authenticationError = null
        if (activity == null) {
            authenticationError = "Hãy nhập số điện thoại Việt Nam hợp lệ, ví dụ 0818916621."
            return@request
        }
        authenticationBusy = true
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(error: FirebaseException) {
                authenticationBusy = false
                authenticationError = error.localizedMessage ?: "Không thể gửi mã OTP."
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                authenticationBusy = false
                pendingE164PhoneNumber = e164PhoneNumber
                forceResendingToken = token
                if (navigateToOtp) {
                    navController.navigate(Screen.OtpVerification.createRoute(verificationId))
                }
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                authenticationBusy = false
                authenticationError = "Chưa nhận được mã OTP. Hãy kiểm tra tin nhắn rồi thử gửi lại mã."
            }
        }
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(e164PhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        if (resendToken != null) {
            options.setForceResendingToken(resendToken)
        }
        PhoneAuthProvider.verifyPhoneNumber(
            options.build()
        )
    }
    val startPhoneSignIn: (String) -> Unit = phone@{ phoneNumber ->
        val e164PhoneNumber = PhoneNumberNormalizer.toE164(phoneNumber)
        if (e164PhoneNumber == null) {
            authenticationError = "Hãy nhập số điện thoại Việt Nam hợp lệ, ví dụ 0818916621."
            return@phone
        }
        requestPhoneVerification(e164PhoneNumber, null, true)
    }
    val resendPhoneVerification = {
        val e164PhoneNumber = pendingE164PhoneNumber
        val resendToken = forceResendingToken
        if (e164PhoneNumber == null || resendToken == null) {
            authenticationError = "Không thể gửi lại mã. Hãy quay lại và thử lại."
        } else {
            requestPhoneVerification(e164PhoneNumber, resendToken, false)
        }
    }
    val resolvedStartDestination = if (startDestination == Screen.Onboarding.route) {
        when (uiState.sessionRole) {
            UserRole.CAREGIVER -> Screen.CaregiverActivity.route
            UserRole.ELDER -> Screen.ElderCalls.route
            null -> startDestination
        }
    } else {
        startDestination
    }
    NavHost(
        navController = navController,
        startDestination = resolvedStartDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.PublicAssistant.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PhoneAuth.route) {
            SignInScreen(
                onContinue = { phoneNumber ->
                    pendingDisplayName = ""
                    startPhoneSignIn(phoneNumber)
                },
                onGoogleSignIn = startGoogleSignIn,
                busy = authenticationBusy,
                error = authenticationError,
                onSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }

        composable(Screen.PublicAssistant.route) {
            LaunchedEffect(Unit) { viewModel.beginVoiceCapture() }
            PublicAssistantScreen(
                busy = uiState.busy,
                transcript = uiState.voiceTranscript,
                response = uiState.voiceMessage,
                error = uiState.error,
                onAudioReady = viewModel::queryPublicVoice,
                onVoiceCaptureStarted = viewModel::beginVoiceCapture,
                onSignIn = { navController.navigate(Screen.PhoneAuth.route) }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onContinue = { displayName, phoneNumber ->
                    pendingDisplayName = displayName
                    startPhoneSignIn(phoneNumber)
                },
                onGoogleSignIn = startGoogleSignIn,
                onSignIn = { navController.popBackStack() },
                busy = authenticationBusy,
                error = authenticationError
            )
        }

        composable(
            Screen.OtpVerification.route,
            arguments = listOf(navArgument("verificationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val verificationId = backStackEntry.arguments?.getString("verificationId").orEmpty()
            OtpVerificationScreen(
                busy = authenticationBusy,
                error = authenticationError,
                onResend = resendPhoneVerification,
                onContinue = { code ->
                    if (verificationId.isBlank()) {
                        authenticationError = "Phiên xác thực đã hết hạn."
                    } else {
                        signInWithCredential(PhoneAuthProvider.getCredential(verificationId, code))
                    }
                }
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

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    viewModel.createSession(role) { actualRole ->
                        val destination = if (actualRole == UserRole.CAREGIVER) {
                            Screen.CaregiverActivity.route
                        } else {
                            Screen.ElderCalls.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                busy = uiState.busy,
                error = uiState.error
            )
        }

        composable(Screen.CaregiverActivity.route) {
            LaunchedEffect(Unit) {
                viewModel.refreshContacts()
                viewModel.refreshMedications()
            }
            CaregiverActivityScreen(
                medicationLogs = medicationLogs,
                contactsCount = contacts.size,
                hasLinkedElder = uiState.elderId != null,
                busy = uiState.busy,
                error = uiState.error,
                onOpenCalls = { navController.navigate(Screen.CaregiverCalls.route) },
                onOpenMedications = { navController.navigate(Screen.CaregiverMedications.route) },
                onOpenSettings = { navController.navigate(Screen.CaregiverSettings.route) }
            )
        }
        composable(Screen.CaregiverCalls.route) {
            LaunchedEffect(Unit) { viewModel.refreshContacts() }
            CaregiverCallScreen(
                contacts = contacts,
                busy = uiState.busy,
                error = uiState.error,
                onAdd = { viewModel.addContact(it) {} },
                onUpdate = { viewModel.updateContact(it) {} },
                onDelete = { viewModel.deleteContact(it) {} },
                onOpenActivity = { navController.popBackStack() },
                onOpenMedications = { navController.navigate(Screen.CaregiverMedications.route) },
                onOpenSettings = { navController.navigate(Screen.CaregiverSettings.route) }
            )
        }
        composable(Screen.CaregiverMedications.route) {
            LaunchedEffect(Unit) { viewModel.refreshMedications() }
            CaregiverMedicationScreen(
                medications = medications,
                busy = uiState.busy,
                error = uiState.error,
                onAdd = { viewModel.addMedication(it) {} },
                onUpdate = { id, medication -> viewModel.updateMedication(id, medication) {} },
                onDelete = { viewModel.deleteMedication(it) {} },
                onOpenActivity = { navController.navigate(Screen.CaregiverActivity.route) },
                onOpenCalls = { navController.navigate(Screen.CaregiverCalls.route) },
                onOpenSettings = { navController.navigate(Screen.CaregiverSettings.route) }
            )
        }
        composable(Screen.CaregiverSettings.route) {
            CaregiverSettingsScreen(
                displayName = firebaseAuth.currentUser?.displayName ?: "Dearly user",
                phoneNumber = firebaseAuth.currentUser?.phoneNumber.orEmpty(),
                onOpenActivity = { navController.navigate(Screen.CaregiverActivity.route) },
                onOpenCalls = { navController.navigate(Screen.CaregiverCalls.route) },
                onOpenMedications = { navController.navigate(Screen.CaregiverMedications.route) },
                onLogout = {
                    viewModel.signOut {
                        navController.navigate(Screen.PublicAssistant.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                },
                onRoleChanged = { role ->
                    if (role == "Người được chăm sóc") {
                        navController.navigate(Screen.ElderCalls.route) {
                            popUpTo(Screen.CaregiverActivity.route) { inclusive = true }
                        }
                    }
                },
                linkBusy = uiState.busy,
                linkError = uiState.error,
                linkMessage = uiState.linkMessage,
                linkedElders = linkedElders,
                onGenerateLinkCode = viewModel::generateElderLinkCode,
                onLinkElder = viewModel::linkElder,
                onUnlinkElder = viewModel::unlinkElder
            )
        }
        composable(Screen.ElderCalls.route) {
            ElderCallingScreen(
                busy = uiState.busy,
                transcript = uiState.voiceTranscript,
                response = uiState.voiceMessage,
                personalization = uiState.voicePersonalization,
                error = uiState.error,
                requiresVerification = uiState.voiceRequiresVerification,
                onVoiceAudio = viewModel::queryVoice,
                onVerificationAudio = viewModel::verifySpokenMedication,
                onVoiceCaptureStarted = viewModel::beginVoiceCapture,
                onOpenMedications = { navController.navigate(Screen.ElderMedications.route) },
                onOpenSettings = { navController.navigate(Screen.ElderSettings.route) }
            )
        }
        composable(Screen.ElderMedications.route) {
            LaunchedEffect(Unit) { viewModel.refreshMedications() }
            ElderMedicationScreen(
                logs = medicationLogs,
                busy = uiState.busy,
                error = uiState.error,
                onVerifyTaken = viewModel::verifyAndMarkTaken,
                onOpenCalls = { navController.navigate(Screen.ElderCalls.route) },
                onOpenSettings = { navController.navigate(Screen.ElderSettings.route) }
            )
        }
        composable(Screen.ElderSettings.route) {
            ElderSettingScreen(
                displayName = firebaseAuth.currentUser?.displayName ?: "Dearly user",
                phoneNumber = firebaseAuth.currentUser?.phoneNumber.orEmpty(),
                onOpenCalls = { navController.navigate(Screen.ElderCalls.route) },
                onOpenMedications = { navController.navigate(Screen.ElderMedications.route) },
                onLogout = {
                    viewModel.signOut {
                        navController.navigate(Screen.PublicAssistant.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                },
                onRoleChanged = { role ->
                    if (role == "Người chăm sóc") {
                        navController.navigate(Screen.CaregiverActivity.route) {
                            popUpTo(Screen.ElderCalls.route) { inclusive = true }
                        }
                    }
                },
                linkCode = uiState.linkCode,
                linkCodeExpiresAt = uiState.linkCodeExpiresAt,
                linkBusy = uiState.busy,
                linkError = uiState.error,
                voiceEnrollmentCount = uiState.voiceEnrollmentCount,
                voiceMessage = uiState.voiceMessage,
                onGenerateLinkCode = viewModel::generateElderLinkCode,
                onEnrollVoice = viewModel::enrollVoice
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
