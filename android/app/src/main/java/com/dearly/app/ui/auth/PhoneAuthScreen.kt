package com.dearly.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.dearly.app.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    onSendOtpClicked: (String, (String) -> Unit, (String) -> Unit) -> Unit,
    onGoogleSignInClicked: (String, () -> Unit, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phoneNumber by remember { mutableStateOf("") }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isSigningInWithGoogle by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Dearly",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Trợ lý ảo thông minh dành cho người cao tuổi",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Số điện thoại", fontSize = 18.sp) },
                placeholder = { Text("0912 345 678", fontSize = 18.sp) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone Icon") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    errorMessage = null
                    if (phoneNumber.isBlank()) {
                        errorMessage = "Vui lòng nhập số điện thoại."
                    } else {
                        isSendingOtp = true
                        onSendOtpClicked(
                            phoneNumber,
                            { isSendingOtp = false },
                            { message ->
                                isSendingOtp = false
                                errorMessage = message
                            }
                        )
                    }
                },
                enabled = !isSendingOtp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (isSendingOtp) "ĐANG GỬI..." else "GỬI MÃ OTP",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 20.sp
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    errorMessage = null
                    isSigningInWithGoogle = true
                    scope.launch {
                        try {
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setServerClientId(context.getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .setAutoSelectEnabled(false)
                                .build()
                            val result = CredentialManager.create(context).getCredential(
                                context = context,
                                request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()
                            )
                            val credential = result.credential
                            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                onGoogleSignInClicked(
                                    GoogleIdTokenCredential.createFrom(credential.data).idToken,
                                    { isSigningInWithGoogle = false },
                                    { message ->
                                        isSigningInWithGoogle = false
                                        errorMessage = message
                                    }
                                )
                            } else {
                                isSigningInWithGoogle = false
                                errorMessage = "Không nhận được thông tin đăng nhập Google hợp lệ."
                            }
                        } catch (exception: GetCredentialException) {
                            isSigningInWithGoogle = false
                            errorMessage = exception.localizedMessage ?: "Không thể đăng nhập bằng Google."
                        }
                    }
                },
                enabled = !isSendingOtp && !isSigningInWithGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isSigningInWithGoogle) "ĐANG ĐĂNG NHẬP..." else "Đăng nhập bằng Google",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 18.sp
                )
            }
        }
    }
}
