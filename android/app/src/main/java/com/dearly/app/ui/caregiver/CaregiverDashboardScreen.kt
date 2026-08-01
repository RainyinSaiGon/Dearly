package com.dearly.app.ui.caregiver

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CaregiverDashboardScreen(
    onNavigateToAddContact: () -> Unit,
    onNavigateToAddMedication: () -> Unit,
    onSignOut: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bảng Điều Khiển Chăm Sóc",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Theo dõi: Bác Nguyễn Văn An",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Health Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sức Khoẻ: Bình Thường",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14532D)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tuân thủ uống thuốc 100% trong hôm nay. 1/1 liều đã hoàn thành.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }

            // Quick Actions Header
            item {
                Text(
                    text = "Quản lý cài đặt người dùng",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Add Contact Action Card
            item {
                Card(
                    onClick = onNavigateToAddContact,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Thêm Người Liên Hệ Mới", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Thêm số điện thoại hoặc Zalo Video", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Add Medication Action Card
            item {
                Card(
                    onClick = onNavigateToAddMedication,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Thêm Lịch Uống Thuốc", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Cài đặt giờ nhắc nhở tự động", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
