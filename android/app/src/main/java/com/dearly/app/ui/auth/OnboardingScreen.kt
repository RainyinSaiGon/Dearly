package com.dearly.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dearly.app.R
import kotlinx.coroutines.delay

private val OnboardingGreen = Color(0xFF1B4332)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1800)
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingGreen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_dearly_mark),
            contentDescription = null,
            modifier = Modifier.size(154.dp)
        )
        Spacer(modifier = Modifier.height(17.dp))
        androidx.compose.material3.Text(
            text = "Dearly",
            color = Color.White,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
