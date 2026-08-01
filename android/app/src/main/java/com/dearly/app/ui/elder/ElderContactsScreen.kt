package com.dearly.app.ui.elder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dearly.app.domain.model.CallMethod
import com.dearly.app.domain.model.Contact

@Composable
fun ElderContactsScreen(
    onNavigateBack: () -> Unit,
    onCallClicked: (Contact) -> Unit
) {
    val sampleContacts = listOf(
        Contact("1", "elder1", "Con Lan", "Nguyễn Thị Lan", "0987654321", "Con gái", CallMethod.ZALO_VIDEO),
        Contact("2", "elder1", "Thằng Tí", "Nguyễn Văn Tí", "0912345678", "Con trai", CallMethod.PHONE),
        Contact("3", "elder1", "Bác Bác Bác", "Trần Văn Bình", "0909090909", "Bạn thân", CallMethod.PHONE)
    )

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Danh Sách Gọi Điện",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(20.dp)
                )
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
            items(sampleContacts) { contact ->
                ContactCard(contact = contact, onCall = { onCallClicked(contact) })
            }
        }
    }
}

@Composable
private fun ContactCard(
    contact: Contact,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.nickname,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = "${contact.relationship} • ${contact.fullName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Button(
                onClick = onCall,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(60.dp)
            ) {
                Icon(
                    imageVector = if (contact.callMethod == CallMethod.ZALO_VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = "Gọi",
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "GỌI", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
