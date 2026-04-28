package com.example.warehousetracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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
import com.example.warehousetracker.data.model.Employee
import com.example.warehousetracker.data.model.EmployeeTrack
import com.example.warehousetracker.data.model.PhaseData
import com.example.warehousetracker.data.model.VehicleTrack
import com.example.warehousetracker.formatDuration
import com.example.warehousetracker.ui.components.WarehouseActionIcon
import com.example.warehousetracker.ui.components.WarehouseHeaderCard
import com.example.warehousetracker.ui.components.WarehouseMetricTile
import com.example.warehousetracker.ui.components.WarehouseSearchField
import com.example.warehousetracker.ui.components.WarehouseSectionCard
import com.example.warehousetracker.ui.components.WarehouseTag
import com.example.warehousetracker.ui.viewmodel.AuthViewModel
import com.example.warehousetracker.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    // Handle Back Button
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

    val activeEmployeeCount = state.tracks.values.count {
        it.preparation.isActive || it.cycleCount.isActive || it.loading.isActive
    }
    val activeVehicleCount = state.vehicleTracks.count {
        it.waiting.isActive || it.offloading.isActive
    }
    val totalTrackedSeconds = if (pagerState.currentPage == 0) {
        state.tracks.values.sumOf { it.totalWHSeconds }
    } else {
        state.vehicleTracks.sumOf { it.totalSeconds }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 160.dp,
        sheetContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .padding(bottom = 16.dp)
            ) {
                if (pagerState.currentPage == 0) {
                    SummaryCardViewOnly(
                        state.selectedBranch?.name ?: "",
                        state.employees,
                        state.tracks,
                        isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                    )
                } else {
                    VehicleSummaryCardViewOnly(
                        state.selectedBranch?.name ?: "",
                        state.vehicleTracks,
                        isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                    )
                }
            }
        },
        sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetSwipeEnabled = true,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WarehouseHeaderCard(
                    title = "Warehouse Viewer",
                    subtitle = "${state.selectedBranch?.name ?: "No branch assigned"} - ${state.date}",
                    eyebrow = "Live branch activity",
                    actions = {
                        WarehouseActionIcon(
                            icon = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            onClick = { showProfile = true }
                        )
                        WarehouseActionIcon(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            onClick = { authViewModel.logout() }
                        )
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WarehouseMetricTile(
                        title = if (pagerState.currentPage == 0) "Employees" else "Vehicles",
                        value = if (pagerState.currentPage == 0) {
                            state.employees.size.toString()
                        } else {
                            state.vehicleTracks.size.toString()
                        },
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    WarehouseMetricTile(
                        title = "Active now",
                        value = if (pagerState.currentPage == 0) {
                            activeEmployeeCount.toString()
                        } else {
                            activeVehicleCount.toString()
                        },
                        modifier = Modifier.weight(1f),
                        accentColor = GreenColor
                    )
                    WarehouseMetricTile(
                        title = "Tracked today",
                        value = formatDuration(totalTrackedSeconds),
                        modifier = Modifier.weight(1f),
                        accentColor = AmberColor
                    )
                }

                WarehouseSectionCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Assigned branch",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.selectedBranch?.name ?: "No branch assigned",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        WarehouseTag(text = if (pagerState.currentPage == 0) "Outbound" else "Inbound")
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            divider = {},
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            Tab(
                                selected = pagerState.currentPage == 0,
                                onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                            ) {
                                Text(
                                    "OUTBOUND",
                                    modifier = Modifier.padding(10.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Tab(
                                selected = pagerState.currentPage == 1,
                                onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                            ) {
                                Text(
                                    "INBOUND",
                                    modifier = Modifier.padding(10.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    WarehouseSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = if (pagerState.currentPage == 0) {
                            "Search employee or code"
                        } else {
                            "Search vehicle or plate number"
                        },
                        leadingIcon = Icons.Default.Search
                    )
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    if (page == 0) {
                        val filteredEmps = if (searchQuery.isBlank()) {
                            state.employees
                        } else {
                            state.employees.filter {
                                it.name.contains(searchQuery, true) || it.code.contains(
                                    searchQuery,
                                    true
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 200.dp)
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
                        val filteredVTracks = if (searchQuery.isBlank()) {
                            state.vehicleTracks
                        } else {
                            state.vehicleTracks.filter {
                                it.type.contains(searchQuery, true) || it.plateNumber.contains(
                                    searchQuery,
                                    true
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 200.dp)
                        ) {
                            items(filteredVTracks, key = { it.vehicleId }) { track ->
                                VehicleCardViewOnly(track)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = name.split(" ").filter { it.isNotBlank() }.take(2)
                        .map { it.firstOrNull()?.uppercase() ?: "" }.joinToString("")
                    Text(
                        initials,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        "Code: $code",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Text(
                    formatDuration(track.totalWHSeconds),
                    fontWeight = FontWeight.Bold,
                    color = AmberColor,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.05f))
            Spacer(Modifier.height(6.dp))
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalShipping,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track.type,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        "Plate: ${track.plateNumber}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Text(
                    formatDuration(track.totalSeconds),
                    fontWeight = FontWeight.Bold,
                    color = AmberColor,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.05f))
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (data.isActive) GreenColor else MaterialTheme.colorScheme.primary.copy(
                        0.15f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (data.isActive) "ACTIVE" else "IDLE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (data.isActive) Color.White else MaterialTheme.colorScheme.primary.copy(
                    0.6f
                )
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            formatDuration(data.accumulatedSeconds),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SummaryCardViewOnly(
    branchName: String,
    employees: List<Employee>,
    tracks: Map<String, EmployeeTrack>,
    isExpanded: Boolean = false
) {
    Column(modifier = Modifier
        .padding(16.dp)
        .animateContentSize()) {
        Text(
            "OUTBOUND SUMMARY - ${branchName.uppercase()}",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(12.dp))
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isExpanded) 400.dp else 90.dp)
                .verticalScroll(scrollState)
        ) {
            employees.forEach { emp ->
                val t = tracks[emp.id] ?: return@forEach
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        emp.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1.5f),
                        maxLines = 1
                    )
                    Text(
                        formatDuration(t.totalWHSeconds),
                        color = AmberColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            modifier = Modifier.padding(vertical = 6.dp)
        )
        val totalWH = employees.sumOf { tracks[it.id]?.totalWHSeconds ?: 0 }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "TOTAL BRANCH TIME",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                formatDuration(totalWH),
                color = AmberColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun VehicleSummaryCardViewOnly(
    branchName: String,
    vehicleTracks: List<VehicleTrack>,
    isExpanded: Boolean = false
) {
    Column(modifier = Modifier
        .padding(16.dp)
        .animateContentSize()) {
        Text(
            "INBOUND SUMMARY - ${branchName.uppercase()}",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(12.dp))
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isExpanded) 400.dp else 90.dp)
                .verticalScroll(scrollState)
        ) {
            vehicleTracks.forEach { t ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${t.type} (${t.plateNumber})",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1.5f),
                        maxLines = 1
                    )
                    Text(
                        formatDuration(t.totalSeconds),
                        color = AmberColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
