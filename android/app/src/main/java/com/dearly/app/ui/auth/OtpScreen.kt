package com.dearly.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OtpScreen(
    phoneNumber: String,
    onVerifySuccess: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }

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
                text = "Xác nhận mã OTP",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Mã xác thực đã được gửi tới số $phoneNumber",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            OutlinedTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it },
                label = { Text("Nhập 6 số OTP", fontSize = 18.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.displayMedium.copy(
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { if (otpCode.length == 6) onVerifySuccess() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "XÁC NHẬN",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 20.sp
                )
            }
        }
    }
}
