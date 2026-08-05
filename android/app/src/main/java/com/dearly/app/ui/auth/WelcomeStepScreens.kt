package com.dearly.app.ui.auth

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dearly.app.R

@Composable
fun WelcomeScreen1(onContinue: () -> Unit, onSkip: () -> Unit) = WelcomeScreenOneLayout(
    title = "Kết nối yêu thương",
    subtitle = "Gọi điện cho người thân chỉ với \n" +
            "một chạm hoặc bằng giọng nói đơn giản.",
    activeStep = 0,
    illustrationAsset = "welcome_illustration_1.svg",
    onContinue = onContinue,
    onSkip = onSkip
)

@Composable
fun WelcomeScreen2(onContinue: () -> Unit, onSkip: () -> Unit) = WelcomeScreenTwoLayout(
    title = "Chăm sóc sức khỏe",
    subtitle = "Không bỏ lỡ mỗi liều thuốc với \n" +
            "lời nhắc đúng giờ, mỗi ngày.",
    activeStep = 1,
    illustrationAsset = "welcome_illustration_2.svg",
    onContinue = onContinue,
    onSkip = onSkip
)

@Composable
private fun WelcomeScreenOneLayout(title: String, subtitle: String, activeStep: Int, illustrationAsset: String, onContinue: () -> Unit, onSkip: () -> Unit) {
    val green = Color(0xFF1B4332)
    val isSecondIllustration = illustrationAsset == "welcome_illustration_2.svg"
    val illustrationWidth = if (isSecondIllustration) 360.dp else 335.dp
    val illustrationHeight = if (isSecondIllustration) 390.dp else 375.dp
    val illustrationOffset = if (isSecondIllustration) 40.dp else 15.dp
    Box(Modifier.fillMaxSize().background(Color(0xFFF8F9F5))) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_dearly_mark), null, Modifier.size(32.dp), tint = Color.Unspecified)
                    Spacer(Modifier.width(8.dp))
                    Text("Dearly", color = green, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onSkip) { Text("Bỏ qua", color = green, fontSize = 16.sp, fontWeight = FontWeight.Medium) }
            }
            Spacer(Modifier.height(70.dp))
            AndroidView(
                factory = { context -> WebView(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT); settings.javaScriptEnabled = false
                    settings.loadWithOverviewMode = true; settings.useWideViewPort = true
                    val svg = context.assets.open(illustrationAsset).bufferedReader().use { it.readText() }
                        .replaceFirst("<svg width=\"258\" height=\"270\"", "<svg width=\"810\" height=\"930\"")
                        .replaceFirst("<svg width=\"258\" height=\"245\"", "<svg width=\"810\" height=\"930\"")
                    loadDataWithBaseURL(null, svg, "image/svg+xml", "UTF-8", null)
                } },
                modifier = Modifier
                    .size(illustrationWidth, illustrationHeight)
                    .offset(x = illustrationOffset)
            )
            Text(title, color = green, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color(0xFF414844), fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            StepIndicator(activeStep)
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 64.dp).height(56.dp),
            shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = green)
        ) { Text("Tiếp tục  →", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
    }
}

@Composable
private fun WelcomeScreenTwoLayout(
    title: String,
    subtitle: String,
    activeStep: Int,
    illustrationAsset: String,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    WelcomeScreenOneLayout(title, subtitle, activeStep, illustrationAsset, onContinue, onSkip)
}

@Composable
private fun StepIndicator(activeStep: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(2) { index ->
            Box(
                Modifier
                    .size(if (index == activeStep) 24.dp else 4.dp, 4.dp)
                    .background(if (index == activeStep) Color(0xFF2C694E) else Color(0xFFC1C8C2), CircleShape)
            )
        }
    }
}
