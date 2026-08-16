package com.dearly.app.ui.auth

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dearly.app.R

private val OtpGreen = Color(0xFF1B4332)
private val OtpBackground = Color(0xFFF8F9F5)

@Composable
fun OtpVerificationScreen(
    onContinue: (String) -> Unit,
    busy: Boolean = false,
    error: String? = null
) {
    val otp = remember { mutableStateListOf("", "", "", "", "", "") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OtpBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        OtpHeader()
        Spacer(modifier = Modifier.height(96.dp))
        OtpIllustration()
        Spacer(modifier = Modifier.height(0.dp))
        Text(
            text = "Xác thực số điện thoại",
            color = OtpGreen,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text("Gửi lại mã sau 59s", color = Color(0xFF414844), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Gửi lại mã", color = Color(0xFF6B7280), fontSize = 11.sp)
        error?.let {
            Text(it, color = Color(0xFFC62828), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            otp.forEachIndexed { index, digit ->
                OutlinedTextField(
                    value = digit,
                    onValueChange = { value -> otp[index] = value.takeLast(1).filter(Char::isDigit) },
                    singleLine = true,
                    modifier = Modifier.size(width = 32.dp, height = 48.dp),
                    shape = RoundedCornerShape(7.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp)
                )
            }
        }
        }
        Button(
            onClick = { onContinue(otp.joinToString("")) },
            enabled = !busy && otp.all { it.length == 1 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 64.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OtpGreen)
        ) {
            Text("Tiếp tục  →", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun OtpHeader() {
    Row(
        modifier = Modifier.height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_dearly_mark), null, Modifier.size(32.dp), tint = Color.Unspecified)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Dearly", color = OtpGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OtpIllustration() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                val svg = context.assets.open("role_selection_illustration.svg")
                    .bufferedReader().use { it.readText() }
                    .replaceFirst("<svg width=\"222\" height=\"271\"", "<svg width=\"810\" height=\"930\"")
                loadDataWithBaseURL(null, svg, "image/svg+xml", "UTF-8", null)
            }
        },
        modifier = Modifier
            .size(width = 270.dp, height = 310.dp)
            .offset(x = 20.dp, y = 12.dp)
    )
}
