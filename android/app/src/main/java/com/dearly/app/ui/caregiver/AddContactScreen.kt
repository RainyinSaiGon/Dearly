package com.dearly.app.ui.caregiver

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dearly.app.domain.model.CallMethod

@Composable
fun AddContactScreen(
    onNavigateBack: () -> Unit,
    onContactSaved: () -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(CallMethod.PHONE) }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thêm Liên Hệ", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Tên thân mật (VD: Con Lan, Thằng Tí)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Họ và tên đầy đủ") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Số điện thoại") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = relationship,
                onValueChange = { relationship = it },
                label = { Text("Quan hệ (Con trai, Con gái, Bác sĩ...)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text("Hình thức gọi:", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilterChip(
                    selected = selectedMethod == CallMethod.PHONE,
                    onClick = { selectedMethod = CallMethod.PHONE },
                    label = { Text("Cuộc gọi thường") }
                )
                FilterChip(
                    selected = selectedMethod == CallMethod.ZALO_VIDEO,
                    onClick = { selectedMethod = CallMethod.ZALO_VIDEO },
                    label = { Text("Zalo Video Call") }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { if (nickname.isNotBlank() && phoneNumber.isNotBlank()) onContactSaved() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("LƯU LIÊN HỆ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
