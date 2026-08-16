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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dearly.app.R

private val SignUpGreen = Color(0xFF1B4332)
private val SignUpBackground = Color(0xFFF8F9F5)

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onContinue: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onSignIn: () -> Unit,
    busy: Boolean = false,
    error: String? = null
) {
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SignUpBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SignUpHeader()
            Spacer(modifier = Modifier.height(18.dp))
            SignUpIllustration()
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
                    tint = SignUpGreen,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onBack)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("Tạo tài khoản mới", color = SignUpGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxSize()
                    .padding(top = 72.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SignUpField(fullName, { fullName = it }, "Họ và tên", "Nhập họ và tên", Icons.Default.Person)
                SignUpField(phoneNumber, { phoneNumber = it }, "Số điện thoại", "Nhập số điện thoại", Icons.Default.Phone)
                error?.let { Text(it, color = Color(0xFFC62828), fontSize = 13.sp) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Checkbox(
                        checked = acceptedTerms,
                        onCheckedChange = { acceptedTerms = it },
                    modifier = Modifier.size(24.dp)
                )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Tôi đồng ý với Điều khoản dịch vụ và \nChính sách bảo mật của Dearly.",
                        color = Color(0xFF414844),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Left
                    )
                }
                Button(
                    onClick = { onContinue(fullName.trim(), phoneNumber.trim()) },
                    enabled = !busy && acceptedTerms && fullName.isNotBlank() && phoneNumber.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(64.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignUpGreen,
                        disabledContainerColor = SignUpGreen.copy(alpha = 0.4f)
                    )
                ) {
                    Text("Tiếp tục  →", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                SignUpDivider()
                OutlinedButton(
                    onClick = onGoogleSignIn,
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(56.dp),
                    shape = CircleShape
                ) {
                    Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Google", color = Color(0xFF1A1C1A))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clickable(onClick = onSignIn),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Bạn đã có tài khoản? ", color = Color(0xFF414844), fontSize = 14.sp)
                    Text("Đăng nhập ngay", color = Color(0xFF2C694E), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SignUpHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_dearly_mark), null, Modifier.size(32.dp), tint = Color.Unspecified)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Dearly", color = SignUpGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SignUpIllustration() {
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
        modifier = Modifier.size(width = 270.dp, height = 310.dp).offset(x = 20.dp)
    )
}

@Composable
private fun SignUpField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color(0xFF41474E), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF6B7280), fontSize = 15.sp) },
            leadingIcon = { Icon(icon, null, tint = SignUpGreen, modifier = Modifier.size(21.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun SignUpDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.width(42.dp).height(1.dp).background(Color(0xFFC1C8C2)))
        Text("hoặc đăng ký", color = Color(0xFF414844), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp))
        Box(modifier = Modifier.width(42.dp).height(1.dp).background(Color(0xFFC1C8C2)))
    }
}
