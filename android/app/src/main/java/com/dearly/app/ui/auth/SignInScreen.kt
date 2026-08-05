package com.dearly.app.ui.auth

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dearly.app.R

private val SignInGreen = Color(0xFF1B4332)
private val SignInBackground = Color(0xFFF8F9F5)

@Composable
fun SignInScreen(onContinue: () -> Unit, onSignUp: () -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SignInBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SignInHeader()
            Spacer(modifier = Modifier.height(18.dp))
            SignInIllustration()
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(548.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFC1C7CF), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFC1C7CF), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = SignInGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Chào mừng trở lại!",
                    color = SignInGreen,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxSize()
                    .padding(top = 72.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SignInField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = "Số điện thoại",
                    placeholder = "Nhập số điện thoại",
                    icon = Icons.Default.Phone
                )
                SignInField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Mật khẩu",
                    placeholder = "Nhập mật khẩu",
                    icon = Icons.Default.Lock,
                    password = true
                )
                Text(
                    text = "Quên mật khẩu?",
                    color = Color(0xFF2C694E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SignInGreen)
                ) {
                    Text("Đăng nhập  →", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                DividerLabel()
                OutlinedButton(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape
                ) {
                    Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Google", color = Color(0xFF1A1C1A))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSignUp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Bạn chưa có tài khoản? ", color = Color(0xFF414844), fontSize = 14.sp)
                    Text("Đăng ký ngay", color = Color(0xFF2C694E), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun SignInHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_dearly_mark),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Dearly", color = SignInGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SignInIllustration() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.defaultTextEncodingName = "UTF-8"
                settings.allowFileAccess = true
                val svg = context.assets.open("role_selection_illustration.svg")
                    .bufferedReader().use { it.readText() }
                    .replaceFirst("<svg width=\"222\" height=\"271\"", "<svg width=\"810\" height=\"930\"")
                loadDataWithBaseURL(null, svg, "image/svg+xml", "UTF-8", null)
            }
        },
        modifier = Modifier
            .size(width = 270.dp, height = 310.dp)
            .offset(x = 20.dp)
    )
}

@Composable
private fun SignInField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    password: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Color(0xFF41474E), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF6B7280), fontSize = 15.sp) },
            leadingIcon = { Icon(icon, null, tint = SignInGreen, modifier = Modifier.size(20.dp)) },
            singleLine = true,
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun DividerLabel() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.width(76.dp).height(1.dp).background(Color(0xFFC1C8C2)))
        Text("hoặc đăng nhập", color = Color(0xFF414844), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 12.dp))
        Box(modifier = Modifier.width(76.dp).height(1.dp).background(Color(0xFFC1C8C2)))
    }
}
