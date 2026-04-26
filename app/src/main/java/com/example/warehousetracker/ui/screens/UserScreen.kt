package com.example.warehousetracker.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehousetracker.AmberColor
import com.example.warehousetracker.GreenColor
import com.example.warehousetracker.LightBg
import com.example.warehousetracker.NavyBlue
import com.example.warehousetracker.data.model.Employee
import com.example.warehousetracker.data.model.EmployeeTrack
import com.example.warehousetracker.data.model.PhaseData
import com.example.warehousetracker.data.model.VehicleTrack
import com.example.warehousetracker.formatDuration
import com.example.warehousetracker.ui.viewmodel.AuthViewModel
import com.example.warehousetracker.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@Composable
fun UserScreen(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val state by dashboardViewModel.state.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { 2 })

    // Initialize with employee's assigned branch
    LaunchedEffect(authState.profile?.branchId) {
        dashboardViewModel.loadBranches(authState.profile?.branchId)
    }

    // Sync tab selection with pager
    LaunchedEffect(state.activeTab) {
        val targetPage = if (state.activeTab == "outbound") 0 else 1
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Update ViewModel when pager is swiped
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val targetTab = if (page == 0) "outbound" else "inbound"
            if (state.activeTab != targetTab) {
                dashboardViewModel.setTab(targetTab)
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showProfile by remember { mutableStateOf(false) }

    // Handle Back Button - Important fix here
    BackHandler(enabled = showProfile) {
        showProfile = false
    }

    if (showProfile) {
        ProfileScreen(
            authViewModel = authViewModel,
            onBack = { showProfile = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlue)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 16.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Text(
                    "Warehouse Viewer",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${state.selectedBranch?.name ?: ""} — ${state.date}",
                    color = Color.White.copy(0.7f),
                    fontSize = 13.sp
                )
            }
            Row(modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(top = 24.dp)
            ) {
                IconButton(onClick = { showProfile = true }) {
                    Icon(
                        Icons.Default.AccountCircle,
                        null,
                        tint = Color.White
                    )
                }
                IconButton(onClick = { authViewModel.logout() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        null,
                        tint = Color.White
                    )
                }
            }
        }

        // Tab Switcher (Outbound / Inbound)
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = Color.White
                )
            }
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } }) {
                Text(
                    "OUTBOUND",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } }) {
                Text(
                    "INBOUND",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = LightBg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Fixed Top Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Branch Display
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(NavyBlue.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    null,
                                    tint = NavyBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Current Location", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    state.selectedBranch?.name ?: "No Branch Assigned",
                                    fontSize = 16.sp,
                                    color = NavyBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = { Text("Search...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
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

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = NavyBlue) }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f),
                            beyondViewportPageCount = 1
                        ) { page ->
                            if (page == 0) {
                                // OUTBOUND CONTENT (Employees)
                                val filteredEmps =
                                    if (searchQuery.isBlank()) state.employees else state.employees.filter {
                                        it.name.contains(
                                            searchQuery,
                                            true
                                        ) || it.code.contains(searchQuery, true)
                                    }
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 12.dp)
                                ) {
                                    items(filteredEmps, key = { it.id }) { emp ->
                                        val track = state.tracks[emp.id] ?: EmployeeTrack(
                                            employeeId = emp.id,
                                            date = state.date
                                        )
                                        EmployeeCardViewOnly(emp.name, emp.code, track)
                                    }
                                }
                            } else {
                                // INBOUND CONTENT (Vehicles)
                                val filteredVTracks =
                                    if (searchQuery.isBlank()) state.vehicleTracks else state.vehicleTracks.filter {
                                        it.type.contains(
                                            searchQuery,
                                            true
                                        ) || it.plateNumber.contains(searchQuery, true)
                                    }
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 12.dp)
                                ) {
                                    items(filteredVTracks, key = { it.vehicleId }) { track ->
                                        VehicleCardViewOnly(track)
                                    }
                                }
                            }
                        }

                        // Bottom Summary
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightBg)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            if (pagerState.currentPage == 0) {
                                SummaryCardViewOnly(
                                    state.selectedBranch?.name ?: "",
                                    state.employees,
                                    state.tracks
                                )
                            } else {
                                VehicleSummaryCardViewOnly(
                                    state.selectedBranch?.name ?: "",
                                    state.vehicleTracks
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── View Only UI Components ──────────────────

@Composable
fun EmployeeCardViewOnly(
    name: String,
    code: String,
    track: EmployeeTrack
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NavyBlue.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = name.split(" ").filter { it.isNotBlank() }.take(2)
                        .map { it.firstOrNull()?.uppercase() ?: "" }.joinToString(""); Text(
                    initials,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Code: $code", color = Color.Gray, fontSize = 11.sp)
                }
                Text(
                    formatDuration(track.totalWHSeconds),
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = NavyBlue.copy(0.05f))
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PhaseViewOnly("Prep", track.preparation, Modifier.weight(1f))
                PhaseViewOnly("Cycle", track.cycleCount, Modifier.weight(1f))
                PhaseViewOnly("Load", track.loading, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun VehicleCardViewOnly(track: VehicleTrack) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NavyBlue.copy(0.1f)), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalShipping,
                        null,
                        tint = NavyBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.type, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Plate: ${track.plateNumber}", color = Color.Gray, fontSize = 11.sp)
                }
                Text(
                    formatDuration(track.totalSeconds),
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = NavyBlue.copy(0.05f))
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhaseViewOnly("Waiting", track.waiting, Modifier.weight(1f))
                PhaseViewOnly("Offload", track.offloading, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PhaseViewOnly(
    label: String,
    data: PhaseData,
    modifier: Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (data.isActive) GreenColor else NavyBlue.copy(0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (data.isActive) "ACTIVE" else "IDLE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (data.isActive) Color.White else NavyBlue.copy(0.6f)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            formatDuration(data.accumulatedSeconds),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = NavyBlue
        )
    }
}

@Composable
fun SummaryCardViewOnly(
    branchName: String,
    employees: List<Employee>,
    tracks: Map<String, EmployeeTrack>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyBlue),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "OUTBOUND SUMMARY - ${branchName.uppercase()}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 100.dp)
                    .verticalScroll(scrollState)
            ) {
                employees.forEach { emp ->
                    val t = tracks[emp.id] ?: return@forEach
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            emp.name,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1.5f),
                            maxLines = 1
                        )
                        Text(
                            formatDuration(t.totalWHSeconds),
                            color = AmberColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
            HorizontalDivider(
                color = Color.White.copy(0.1f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            val totalWH = employees.sumOf { tracks[it.id]?.totalWHSeconds ?: 0 }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "TOTAL TIME",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    formatDuration(totalWH),
                    color = AmberColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun VehicleSummaryCardViewOnly(
    branchName: String,
    vehicleTracks: List<VehicleTrack>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyBlue),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "INBOUND SUMMARY - ${branchName.uppercase()}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 100.dp)
                    .verticalScroll(scrollState)
            ) {
                vehicleTracks.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${t.type} (${t.plateNumber})",
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1.5f),
                            maxLines = 1
                        )
                        Text(
                            formatDuration(t.totalSeconds),
                            color = AmberColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
