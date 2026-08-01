package com.dearly.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    onSendOtpClicked: (String) -> Unit,
    onGoogleSignInClicked: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }

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
                onClick = { if (phoneNumber.isNotBlank()) onSendOtpClicked(phoneNumber) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "GỬI MÃ OTP",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onGoogleSignInClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Đăng nhập bằng Google",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 18.sp
                )
            }
        }
    }
}
