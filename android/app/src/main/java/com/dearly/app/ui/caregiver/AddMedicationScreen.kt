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

@Composable
fun AddMedicationScreen(
    onNavigateBack: () -> Unit,
    onMedicationSaved: () -> Unit
) {
    var medName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("2") }
    var timeSlot1 by remember { mutableStateOf("08:00") }
    var timeSlot2 by remember { mutableStateOf("20:00") }

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
                    Text("Thêm Lịch Uống Thuốc", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
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
                value = medName,
                onValueChange = { medName = it },
                label = { Text("Tên thuốc (VD: Thuốc Huyết Áp Amlodipine)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Liều lượng (VD: 1 viên sau ăn)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = timeSlot1,
                    onValueChange = { timeSlot1 = it },
                    label = { Text("Giờ lần 1") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = timeSlot2,
                    onValueChange = { timeSlot2 = it },
                    label = { Text("Giờ lần 2") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { if (medName.isNotBlank()) onMedicationSaved() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("LƯU LỊCH THUỐC", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
