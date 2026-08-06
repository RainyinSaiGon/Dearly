package com.dearly.app.ui.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dearly.app.R

@Composable
fun CaregiverHeader(shadowElevation: androidx.compose.ui.unit.Dp = 5.dp) {
    Surface(color = Color.White, shadowElevation = shadowElevation) {
        Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(Color.White)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
        ) {
        Icon(
            painter = painterResource(R.drawable.ic_dearly_mark),
            contentDescription = "Dearly",
            modifier = Modifier.size(26.dp),
            tint = Color.Unspecified
        )
        Spacer(Modifier.width(7.dp))
        Text("Dearly", color = Color(0xFF174D3B), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.weight(1f))
        Surface(color = Color(0xFFF4F5F2), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Outlined.PersonOutline,
                contentDescription = "Hồ sơ",
                modifier = Modifier.padding(6.dp),
                tint = Color(0xFF3A4845)
            )
        }
        }
    }
}
