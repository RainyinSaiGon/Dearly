package com.dearly.app.ui.elder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dearly.app.domain.model.HealthStatusLevel

@Composable
fun ElderHomeScreen(
    onSpeakClicked: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToMedication: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        bottomBar = {
            ElderBottomBar(
                currentScreen = "home",
                onNavigateToContacts = onNavigateToContacts,
                onNavigateToMedication = onNavigateToMedication,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header / Greeting
            Text(
                text = "Xin chào, Bác An!",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Hôm nay là Thứ Bảy, 01/08",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Health & Reminder Badge Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Status",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Trạng thái: Bình thường",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14532D)
                        )
                        Text(
                            text = "Đã uống đủ liều thuốc buổi sáng",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Giant Central Speak / Micro Button (Mode A: Tap to speak)
            Surface(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .clickable { onSpeakClicked() },
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak Button",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "NÓI CHUYỆN",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }

            Text(
                text = "Bấm nút hoặc nói 'Hey Dearly'",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ElderBottomBar(
    currentScreen: String,
    onNavigateToContacts: () -> Unit,
    onNavigateToMedication: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    NavigationBar(
        modifier = Modifier.height(88.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        NavigationBarItem(
            selected = currentScreen == "home",
            onClick = {},
            icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ", modifier = Modifier.size(32.dp)) },
            label = { Text("Trang chủ", fontSize = 16.sp) }
        )
        NavigationBarItem(
            selected = currentScreen == "contacts",
            onClick = onNavigateToContacts,
            icon = { Icon(Icons.Default.Call, contentDescription = "Gọi điện", modifier = Modifier.size(32.dp)) },
            label = { Text("Gọi điện", fontSize = 16.sp) }
        )
        NavigationBarItem(
            selected = currentScreen == "medication",
            onClick = onNavigateToMedication,
            icon = { Icon(Icons.Default.Medication, contentDescription = "Lịch thuốc", modifier = Modifier.size(32.dp)) },
            label = { Text("Lịch thuốc", fontSize = 16.sp) }
        )
        NavigationBarItem(
            selected = currentScreen == "settings",
            onClick = onNavigateToSettings,
            icon = { Icon(Icons.Default.Settings, contentDescription = "Cài đặt", modifier = Modifier.size(32.dp)) },
            label = { Text("Cài đặt", fontSize = 16.sp) }
        )
    }
}
