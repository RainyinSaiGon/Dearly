package com.dearly.app.ui.auth

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dearly.app.R
import kotlinx.coroutines.delay

private val OtpGreen = Color(0xFF1B4332)
private val OtpBackground = Color(0xFFF8F9F5)
private val OtpErrorBackground = Color(0xFFFFF3F1)
private val OtpErrorBorder = Color(0xFFFFC8C2)
private val OtpErrorText = Color(0xFFB3261E)

@Composable
fun OtpVerificationScreen(
    onContinue: (String) -> Unit,
    onResend: () -> Unit,
    busy: Boolean = false,
    error: String? = null
) {
    val otp = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(otp.size) { FocusRequester() } }
    var secondsRemaining by remember { mutableIntStateOf(60) }
    var focusedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        focusRequesters.first().requestFocus()
    }
    LaunchedEffect(secondsRemaining) {
        if (secondsRemaining > 0) {
            delay(1_000)
            secondsRemaining -= 1
        }
    }

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
        Spacer(modifier = Modifier.height(64.dp))
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
        Text(
            text = if (secondsRemaining > 0) "Gửi lại mã sau ${secondsRemaining}s" else "Bạn chưa nhận được mã?",
            color = Color(0xFF414844),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (secondsRemaining > 0) {
            Text("Gửi lại mã", color = Color(0xFF6B7280), fontSize = 11.sp)
        } else {
            Text(
                text = "Gửi lại mã",
                color = OtpGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !busy) {
                    secondsRemaining = 60
                    onResend()
                }
            )
        }
        error?.let {
            OtpErrorMessage(it)
        }
        Spacer(modifier = Modifier.height(if (error == null) 14.dp else 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            otp.forEachIndexed { index, digit ->
                BasicTextField(
                    value = digit,
                    onValueChange = { value ->
                        val enteredDigits = value.filter(Char::isDigit)
                        if (enteredDigits.isEmpty()) {
                            otp[index] = ""
                        } else {
                            enteredDigits.take(otp.size - index).forEachIndexed { offset, enteredDigit ->
                                otp[index + offset] = enteredDigit.toString()
                            }
                            focusRequesters[(index + enteredDigits.length).coerceAtMost(otp.lastIndex)]
                                .requestFocus()
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .size(width = 44.dp, height = 52.dp)
                        .focusRequester(focusRequesters[index])
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) focusedIndex = index
                        }
                        .onPreviewKeyEvent { event ->
                            if (
                                event.type == KeyEventType.KeyDown &&
                                event.key == Key.Backspace &&
                                otp[index].isEmpty() &&
                                index > 0
                            ) {
                                focusRequesters[index - 1].requestFocus()
                                true
                            } else {
                                false
                            }
                        },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = if (index == otp.lastIndex) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequesters[(index + 1).coerceAtMost(otp.lastIndex)].requestFocus() },
                        onDone = { if (otp.all { it.length == 1 }) onContinue(otp.joinToString("")) }
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = OtpGreen,
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(OtpGreen),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White, RoundedCornerShape(7.dp))
                                .border(
                                    width = if (focusedIndex == index) 2.dp else 1.dp,
                                    color = if (focusedIndex == index) OtpGreen else Color(0xFF8A8F98),
                                    shape = RoundedCornerShape(7.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            innerTextField()
                        }
                    }
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
private fun OtpErrorMessage(error: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .background(OtpErrorBackground, RoundedCornerShape(14.dp))
            .border(1.dp, OtpErrorBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(OtpErrorText, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "Mã xác thực chưa đúng",
                color = OtpErrorText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = localizedOtpError(error),
                color = Color(0xFF70413D),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

private fun localizedOtpError(error: String): String {
    val normalized = error.lowercase()
    return when {
        "expired" in normalized || "hết hạn" in normalized ->
            "Mã có thể đã hết hạn. Bác hãy yêu cầu gửi lại mã mới nhé."
        "network" in normalized || "kết nối" in normalized ->
            "Không thể kết nối ngay lúc này. Bác kiểm tra mạng rồi thử lại nhé."
        "invalid" in normalized || "verification code" in normalized || "sms/totp" in normalized ->
            "Bác hãy kiểm tra tin nhắn và nhập lại 6 số vừa nhận được nhé."
        else -> "Bác hãy kiểm tra tin nhắn và nhập lại mã xác thực nhé."
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
            .size(width = 244.dp, height = 280.dp)
            .offset(x = 20.dp, y = 12.dp)
    )
}
