package com.dearly.app.ui.auth

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.dearly.app.domain.model.UserRole

private val DearlyGreen = Color(0xFF1B4332)
private val RoleBackground = Color(0xFFF8F9F5)
private val RoleBorder = Color(0xFFE2E3E0)
private val RoleIconBackground = Color(0xFFF3F4F1)
private val RoleBodyText = Color(0xFF414844)

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit,
    onBack: () -> Unit,
    busy: Boolean = false,
    error: String? = null
) {
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RoleBackground)
            .verticalScroll(rememberScrollState())
    ) {
        RoleHeader(onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(38.dp))
            RoleSelectionIllustration()

            Spacer(modifier = Modifier.height(0.dp))
            Text(
                text = "Gần như đã xong!",
                color = DearlyGreen,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Để chúng tôi chuẩn bị không gian\nphù hợp nhất cho bạn",
                color = RoleBodyText,
                fontSize = 16.sp,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))
            RoleOption(
                title = "Người được chăm sóc",
                icon = Icons.Default.AccessibilityNew,
                selected = selectedRole == UserRole.ELDER,
                onClick = { selectedRole = UserRole.ELDER }
            )
            Spacer(modifier = Modifier.height(24.dp))
            RoleOption(
                title = "Người chăm sóc",
                icon = Icons.Default.MedicalServices,
                selected = selectedRole == UserRole.CAREGIVER,
                onClick = { selectedRole = UserRole.CAREGIVER }
            )

            Spacer(modifier = Modifier.height(32.dp))
            error?.let {
                Text(
                    it,
                    color = Color(0xFFC62828),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            Button(
                onClick = { selectedRole?.let(onRoleSelected) },
                enabled = selectedRole != null && !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DearlyGreen,
                    disabledContainerColor = DearlyGreen.copy(alpha = 0.35f)
                )
            ) {
                Text(
                    text = "Hoàn thành",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.36.sp
                )
            }
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun RoleHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFFF9FAF6))
            .padding(horizontal = 20.dp),
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Quay lại",
            tint = DearlyGreen,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(22.dp)
                .clickable(onClick = onBack)
        )
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_dearly_mark),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Dearly",
                color = DearlyGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RoleSelectionIllustration() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.defaultTextEncodingName = "UTF-8"
                settings.allowFileAccess = true
                val svg = context.assets
                    .open("role_selection_illustration.svg")
                    .bufferedReader()
                    .use { it.readText() }
                    .replaceFirst(
                        "<svg width=\"222\" height=\"271\"",
                        "<svg width=\"810\" height=\"930\""
                    )
                loadDataWithBaseURL(
                    null,
                    svg,
                    "image/svg+xml",
                    "UTF-8",
                    null
                )
            }
        },
        modifier = Modifier
            .size(width = 270.dp, height = 310.dp)
            .offset(x = 20.dp, y = 12.dp)
    )
}

@Composable
private fun RoleOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE7F0E9) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = if (selected) DearlyGreen else RoleBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(RoleIconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DearlyGreen,
                    modifier = Modifier.size(44.dp)
                )
            }
            Text(
                text = title,
                color = DearlyGreen,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
