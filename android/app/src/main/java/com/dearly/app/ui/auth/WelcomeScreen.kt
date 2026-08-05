package com.dearly.app.ui.auth

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
fun WelcomeScreen(onContinue: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF8F9F5))) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_dearly_mark), null, Modifier.size(32.dp), tint = Color.Unspecified)
                Spacer(Modifier.width(8.dp))
                Text("Dearly", color = Color(0xFF1B4332), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(70.dp))
            AndroidView(
                factory = { context -> WebView(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT); settings.javaScriptEnabled = false
                    settings.loadWithOverviewMode = true; settings.useWideViewPort = true
                    val svg = context.assets.open("welcome_illustration.svg").bufferedReader().use { it.readText() }
                        .replaceFirst("<svg width=\"258\" height=\"270\"", "<svg width=\"810\" height=\"930\"")
                    loadDataWithBaseURL(null, svg, "image/svg+xml", "UTF-8", null)
                } },
                modifier = Modifier
                    .size(335.dp, 375.dp)
                    .offset(x = 10.dp)
            )
            Text("Chào mừng bạn\nđến với Dearly!", color = Color(0xFF1B4332), fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Kết nối yêu thương mỗi ngày", color = Color(0xFF414844), fontSize = 14.sp)
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 64.dp).height(56.dp),
            shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4332))
        ) { Text("Tiếp tục  →", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
    }
}
