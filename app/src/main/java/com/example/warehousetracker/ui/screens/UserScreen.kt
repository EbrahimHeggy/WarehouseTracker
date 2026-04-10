package com.example.warehousetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.warehousetracker.LightBg
import com.example.warehousetracker.NavyBlue
import com.example.warehousetracker.ui.viewmodel.AuthViewModel

@Composable
fun UserScreen(authViewModel: AuthViewModel) {
    val authState by authViewModel.state.collectAsState()
    val profile = authState.profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlue)
            .statusBarsPadding()
    ) {
        // Header
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    "Warehouse Management",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Employee Portal", color = Color.White.copy(0.7f), fontSize = 12.sp)
            }
            IconButton(
                onClick = { authViewModel.logout() },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(Icons.Default.Logout, null, tint = Color.White)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = LightBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(NavyBlue.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = profile?.name?.split(" ")?.filter { it.isNotBlank() }
                        ?.take(2)?.map { it.first().uppercase() }?.joinToString("") ?: "U"
                    Text(initials, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
                }

                Text(
                    profile?.name ?: "Employee",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )
                Text(profile?.email ?: "", fontSize = 14.sp, color = Color.Gray)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileRow("Role", if (profile?.role == "admin") "Admin" else "Employee")
                        HorizontalDivider(color = Color.Gray.copy(0.1f))
                        ProfileRow("Branch ID", profile?.branchId ?: "—")
                        HorizontalDivider(color = Color.Gray.copy(0.1f))
                        ProfileRow("Email", profile?.email ?: "—")
                    }
                }

                Spacer(Modifier.weight(1f))

                Text(
                    "Contact your admin to track working hours",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NavyBlue)
    }
}