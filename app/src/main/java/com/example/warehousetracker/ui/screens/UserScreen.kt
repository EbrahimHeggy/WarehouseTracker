package com.example.warehousetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.warehousetracker.AmberColor
import com.example.warehousetracker.GreenColor
import com.example.warehousetracker.LightBg
import com.example.warehousetracker.NavyBlue
import com.example.warehousetracker.data.model.Employee
import com.example.warehousetracker.data.model.EmployeeTrack
import com.example.warehousetracker.data.model.PhaseData
import com.example.warehousetracker.data.repository.EmployeeRepository
import com.example.warehousetracker.data.repository.TrackingRepository
import com.example.warehousetracker.formatDuration
import com.example.warehousetracker.ui.viewmodel.AuthViewModel

@Composable
fun UserScreen(authViewModel: AuthViewModel) {
    val authState by authViewModel.state.collectAsState()
    val profile = authState.profile

    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var tracks by remember { mutableStateOf<Map<String, EmployeeTrack>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val empRepo = remember { EmployeeRepository() }
    val trackRepo = remember { TrackingRepository() }

    LaunchedEffect(profile?.branchId) {
        if (!profile?.branchId.isNullOrBlank()) {
            isLoading = true
            employees = empRepo.getEmployeesByBranch(profile!!.branchId)
            tracks = trackRepo.getTracksForBranch(employees, trackRepo.today())
            isLoading = false
        }
    }

    val filteredEmployees = remember(employees, searchQuery) {
        if (searchQuery.isBlank()) employees
        else employees.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.code.contains(searchQuery, ignoreCase = true)
        }
    }

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
                Text(
                    "Branch View — ${trackRepo.today()}",
                    color = Color.White.copy(0.7f),
                    fontSize = 12.sp
                )
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Profile Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(NavyBlue.copy(0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = profile?.name?.split(" ")
                                        ?.filter { it.isNotBlank() }?.take(2)
                                        ?.map { it.first().uppercase() }?.joinToString("") ?: "U"
                                    Text(
                                        initials,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyBlue
                                    )
                                }
                                Column {
                                    Text(
                                        profile?.name ?: "Employee",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(profile?.email ?: "", color = Color.Gray, fontSize = 12.sp)
                                    Text(
                                        "Employee • View Only",
                                        color = NavyBlue.copy(0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Search Bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            placeholder = { Text("Search by name or code...", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = NavyBlue,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }

                    // Metrics
                    item {
                        val activeCount = employees.count { emp ->
                            val t = tracks[emp.id] ?: return@count false
                            t.preparation.isActive || t.cycleCount.isActive || t.loading.isActive
                        }
                        val totalSeconds = employees.sumOf { tracks[it.id]?.totalWHSeconds ?: 0 }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                Modifier.weight(1f),
                                "${employees.size}",
                                "Employees",
                                NavyBlue
                            )
                            MetricCard(Modifier.weight(1f), "$activeCount", "Active", GreenColor)
                            MetricCard(
                                Modifier.weight(1f),
                                formatDuration(totalSeconds),
                                "Total",
                                AmberColor
                            )
                        }
                    }

                    // Section Title
                    item {
                        Text(
                            "TODAY'S ACTIVITY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                    }

                    // Employee Read-Only Cards
                    items(filteredEmployees, key = { it.id }) { emp ->
                        val track = tracks[emp.id] ?: EmployeeTrack(
                            employeeId = emp.id,
                            date = trackRepo.today()
                        )
                        ReadOnlyEmployeeCard(emp = emp, track = track)
                    }
                }
            }
        }
    }
}

// ── Read Only Card (مش فيها أي buttons للتعديل) ──
@Composable
fun ReadOnlyEmployeeCard(emp: Employee, track: EmployeeTrack) {
    val isAnyActive =
        track.preparation.isActive || track.cycleCount.isActive || track.loading.isActive

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Employee Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NavyBlue.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = emp.name.split(" ").filter { it.isNotBlank() }.take(2)
                        .map { it.firstOrNull()?.uppercase() ?: "" }.joinToString("")
                    Text(initials, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Code: ${emp.code}", color = Color.Gray, fontSize = 11.sp)
                }
                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isAnyActive) GreenColor.copy(0.12f) else Color.Gray.copy(
                                0.1f
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isAnyActive) "Active" else "Idle",
                        fontSize = 11.sp,
                        color = if (isAnyActive) GreenColor else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.Gray.copy(0.1f))
            Spacer(Modifier.height(10.dp))

            // Phase Summary — View Only
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadOnlyPhase("Prep & Check", track.preparation, Modifier.weight(1f))
                ReadOnlyPhase("Cycle Count", track.cycleCount, Modifier.weight(1f))
                ReadOnlyPhase("Loading", track.loading, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color.Gray.copy(0.1f))
            Spacer(Modifier.height(8.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total WH Time", fontSize = 12.sp, color = Color.Gray)
                Text(
                    formatDuration(track.totalWHSeconds),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )
            }
        }
    }
}

@Composable
fun ReadOnlyPhase(label: String, data: PhaseData, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (data.isActive) GreenColor.copy(0.1f) else NavyBlue.copy(0.05f))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (data.isActive) "Active" else formatDuration(data.accumulatedSeconds),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (data.isActive) GreenColor else NavyBlue
            )
        }
        if (data.isActive) {
            Spacer(Modifier.height(2.dp))
            Text("Since ${data.currentStartTime}", fontSize = 8.sp, color = GreenColor)
        }
    }
}
