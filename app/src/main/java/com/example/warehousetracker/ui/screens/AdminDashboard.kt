package com.example.warehousetracker.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehousetracker.AmberColor
import com.example.warehousetracker.GreenColor
import com.example.warehousetracker.NavyBlue
import com.example.warehousetracker.RedColor
import com.example.warehousetracker.data.model.Branch
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
fun AdminDashboardScreen(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by dashboardViewModel.state.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { 2 })

    // Initialize with admin's assigned branch
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

    var branchDropdownExpanded by remember { mutableStateOf(false) }
    var showAddBranchDialog by remember { mutableStateOf(false) }
    var branchToDelete by remember { mutableStateOf<Branch?>(null) }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }
    var vehicleTrackToDelete by remember { mutableStateOf<VehicleTrack?>(null) }
    var manualTimeDialogData by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showRegisterUserDialog by remember { mutableStateOf(false) }
    var showImportScreen by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showExportScreen by remember { mutableStateOf(false) }

    // ── Handle Back Button ──
    BackHandler(enabled = showImportScreen || showExportScreen || showProfile || branchDropdownExpanded) {
        when {
            showImportScreen -> showImportScreen = false
            showExportScreen -> showExportScreen = false
            showProfile -> showProfile = false
            branchDropdownExpanded -> branchDropdownExpanded = false
        }
    }

    if (showProfile) {
        ProfileScreen(
            authViewModel = authViewModel,
            onBack = { showProfile = false },
            onImportClick = { showImportScreen = true },
            onRegisterUserClick = { showRegisterUserDialog = true }
        )
        if (showRegisterUserDialog) {
            RegisterUserDialog(
                onDismiss = { showRegisterUserDialog = false },
                onRegister = { email, pass, name, role, branchId ->
                    authViewModel.registerByAdmin(
                        context,
                        email,
                        pass,
                        name,
                        role,
                        branchId
                    ) { success, error ->
                        if (success) {
                            Toast.makeText(context, "User created successfully!", Toast.LENGTH_LONG)
                                .show()
                            showRegisterUserDialog = false
                        } else {
                            Toast.makeText(
                                context,
                                error ?: "Failed to create user",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                availableBranches = state.branches,
                currentBranch = state.selectedBranch
            )
        }
        if (showImportScreen) {
            ImportEmployeesScreen(
                onBack = { showImportScreen = false },
                onImportDone = {
                    showImportScreen = false
                    state.selectedBranch?.let { dashboardViewModel.loadEmployees(it.id) }
                }
            )
        }
        return
    }

    if (showImportScreen) {
        ImportEmployeesScreen(
            onBack = { showImportScreen = false },
            onImportDone = {
                showImportScreen = false
                state.selectedBranch?.let { dashboardViewModel.loadEmployees(it.id) }
            }
        )
        return
    }

    if (showExportScreen) {
        ExportScreen(
            dashboardViewModel = dashboardViewModel,
            onBack = { showExportScreen = false }
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
                    SummaryCard(
                        state.selectedBranch?.name ?: "",
                        state.employees,
                        state.tracks,
                        { showExportScreen = true },
                        isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                    )
                } else {
                    VehicleSummaryCard(
                        state.selectedBranch?.name ?: "",
                        state.vehicleTracks,
                        { showExportScreen = true },
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
                    title = "Warehouse Manager",
                    subtitle = "${state.selectedBranch?.name ?: "No branch selected"} - ${state.date}",
                    eyebrow = "Operations overview",
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
                    OutlinedButton(
                        onClick = { showAddBranchDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AddHome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New branch", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            if (state.activeTab == "outbound") showAddEmployeeDialog = true
                            else showAddVehicleDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            if (state.activeTab == "outbound") Icons.Default.PersonAdd else Icons.Default.LocalShipping,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (state.activeTab == "outbound") "Add employee" else "Add visit",
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WarehouseMetricTile(
                        title = if (pagerState.currentPage == 0) "Employees" else "Visits",
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { branchDropdownExpanded = true },
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
                                text = "Current location",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.selectedBranch?.name ?: "Select branch",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        WarehouseTag(text = "${state.branches.size} branches")
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (branchDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = branchDropdownExpanded,
                        onDismissRequest = { branchDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        state.branches.forEach { branch ->
                            val empCount = state.branchEmployeeCounts[branch.id] ?: 0
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                branch.name,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                "$empCount employees",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                branchToDelete = branch
                                                branchDropdownExpanded = false
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                null,
                                                tint = RedColor.copy(alpha = 0.75f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    dashboardViewModel.selectBranch(branch)
                                    branchDropdownExpanded = false
                                }
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
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
                                EmployeeCard(
                                    emp.name,
                                    emp.code,
                                    track,
                                    { phase -> dashboardViewModel.togglePhase(emp.id, phase) },
                                    { phase ->
                                        manualTimeDialogData = Triple(emp.id, phase, false)
                                    },
                                    { employeeToDelete = emp }
                                )
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
                                VehicleCard(
                                    track,
                                    { phase ->
                                        dashboardViewModel.toggleVehiclePhase(
                                            track.vehicleId,
                                            phase
                                        )
                                    },
                                    { phase ->
                                        manualTimeDialogData = Triple(track.vehicleId, phase, true)
                                    },
                                    { vehicleTrackToDelete = track }
                                )
                            }
                        }
                    }
                }
            }
        }
    }



    // ── DIALOGS ──
    if (showAddBranchDialog) {
        var branchName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBranchDialog = false },
            title = { Text("Add Branch") },
            text = {
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (branchName.isNotBlank()) {
                        dashboardViewModel.addBranch(branchName, context)
                        showAddBranchDialog = false
                    }
                }) { Text("Add") }
            }
        )
    }

    if (branchToDelete != null) {
        AlertDialog(
            onDismissRequest = { branchToDelete = null },
            title = { Text("Delete Branch") },
            text = { Text("Are you sure you want to delete branch \"${branchToDelete?.name}\"? This will remove the branch and all its data from the system.") },
            confirmButton = {
                Button(
                    onClick = {
                        branchToDelete?.let { dashboardViewModel.deleteBranch(it.id) }
                        branchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedColor)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { branchToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddEmployeeDialog) {
        var empName by remember { mutableStateOf("") }
        var empCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            title = { Text("Add Employee") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = empName,
                        onValueChange = { empName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    ); OutlinedTextField(
                    value = empCode,
                    onValueChange = { empCode = it },
                    label = { Text("Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (empName.isNotBlank() && empCode.isNotBlank()) {
                        state.selectedBranch?.let {
                            dashboardViewModel.addEmployee(
                                empName,
                                empCode,
                                it.id,
                                context
                            )
                        }
                        showAddEmployeeDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddEmployeeDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (employeeToDelete != null) {
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            title = { Text("Delete Employee") },
            text = { Text("Remove \"${employeeToDelete?.name}\" from this branch?") },
            confirmButton = {
                Button(
                    onClick = {
                        employeeToDelete?.let {
                            dashboardViewModel.deleteEmployee(
                                it.id
                            )
                        }
                        employeeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedColor)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddVehicleDialog) {
        var vType by remember { mutableStateOf("Actros") }
        var vPlate by remember { mutableStateOf("") }
        val carTypes = listOf("Actros", "Axor", "NQR")

        AlertDialog(
            onDismissRequest = { showAddVehicleDialog = false },
            title = { Text("Add Vehicle Visit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Vehicle Type:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        carTypes.forEach { type ->
                            Button(
                                onClick = { vType = type },
                                colors = ButtonDefaults.buttonColors(containerColor = if (vType == type) NavyBlue else Color.LightGray)
                            ) { Text(type, fontSize = 11.sp) }
                        }
                    }

                    OutlinedTextField(
                        value = vPlate,
                        onValueChange = { vPlate = it },
                        label = { Text("Plate Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (vType.isNotBlank() && vPlate.isNotBlank()) {
                        state.selectedBranch?.let {
                            dashboardViewModel.addVehicle(
                                vType,
                                vPlate,
                                context
                            )
                        }
                        showAddVehicleDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddVehicleDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (vehicleTrackToDelete != null) {
        AlertDialog(
            onDismissRequest = { vehicleTrackToDelete = null },
            title = { Text("Delete Visit") },
            text = { Text("Remove this visit for \"${vehicleTrackToDelete?.plateNumber}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        vehicleTrackToDelete?.let {
                            dashboardViewModel.deleteVehicle(
                                it.vehicleId
                            )
                        }
                        vehicleTrackToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedColor)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { vehicleTrackToDelete = null }) { Text("Cancel") }
            }
        )
    }

    manualTimeDialogData?.let { (id, phase, isVehicle) ->
        var manualHours by remember { mutableStateOf("") }
        var manualMinutes by remember { mutableStateOf("") }
        var manualSeconds by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { manualTimeDialogData = null },
            title = { Text("Manual Time Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter accumulated time for ${phase.uppercase()}:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualHours,
                            onValueChange = { if (it.length <= 2) manualHours = it },
                            label = { Text("HH") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        ); OutlinedTextField(
                        value = manualMinutes,
                        onValueChange = { if (it.length <= 2) manualMinutes = it },
                        label = { Text("MM") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    ); OutlinedTextField(
                        value = manualSeconds,
                        onValueChange = { if (it.length <= 2) manualSeconds = it },
                        label = { Text("SS") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val h = manualHours.toIntOrNull() ?: 0
                    val m = manualMinutes.toIntOrNull() ?: 0
                    val s = manualSeconds.toIntOrNull() ?: 0
                    val total = (h * 3600) + (m * 60) + s
                    dashboardViewModel.applyManualTime(
                        id,
                        phase,
                        total,
                        null,
                        null,
                        isVehicle
                    )
                    manualTimeDialogData = null
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { manualTimeDialogData = null }) { Text("Cancel") }
            }
        )
    }
}

// ── UI Components ──────────────────

@Composable
fun SummaryCard(
    branchName: String,
    employees: List<Employee>,
    tracks: Map<String, EmployeeTrack>,
    onExportRange: () -> Unit,
    isExpanded: Boolean = false
) {
    Column(modifier = Modifier
        .padding(16.dp)
        .animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "OUTBOUND SUMMARY - ${branchName.uppercase()}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            IconButton(
                onClick = onExportRange,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                "Name",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1.2f)
            )
            Text(
                "Prep",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                "Cycle",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                "Load",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                "Total",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1.1f),
                textAlign = TextAlign.Center
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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
                        modifier = Modifier.weight(1.2f),
                        maxLines = 1
                    )
                    Text(
                        formatDuration(t.preparation.accumulatedSeconds),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        formatDuration(t.cycleCount.accumulatedSeconds),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        formatDuration(t.loading.accumulatedSeconds),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        formatDuration(t.totalWHSeconds),
                        color = AmberColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        val totalWH = employees.sumOf { tracks[it.id]?.totalWHSeconds ?: 0 }
        if (employees.isNotEmpty()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 6.dp)
            )
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
}

@Composable
fun VehicleSummaryCard(
    branchName: String,
    vehicleTracks: List<VehicleTrack>,
    onExportRange: () -> Unit,
    isExpanded: Boolean = false
) {
    Column(modifier = Modifier
        .padding(16.dp)
        .animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "INBOUND SUMMARY - ${branchName.uppercase()}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            IconButton(onClick = onExportRange, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Share,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                "Vehicle",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1.2f)
            )
            Text(
                "Waiting",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                "Offload",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                "Total",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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
                        modifier = Modifier.weight(1.2f),
                        maxLines = 1
                    )
                    Text(
                        formatDuration(t.waiting.accumulatedSeconds),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        formatDuration(t.offloading.accumulatedSeconds),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        formatDuration(t.totalSeconds),
                        color = AmberColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterUserDialog(
    onDismiss: () -> Unit,
    onRegister: (String, String, String, String, String) -> Unit,
    availableBranches: List<Branch>,
    currentBranch: Branch?
) {
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf("user") }
    var regBranch by remember { mutableStateOf(currentBranch) }
    var roleExpanded by remember { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }
    var showRegPassword by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = regName,
                    onValueChange = { regName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                ); OutlinedTextField(
                value = regEmail,
                onValueChange = { regEmail = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            ); OutlinedTextField(
                value = regPassword,
                onValueChange = { regPassword = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = {
                        showRegPassword = !showRegPassword
                    }) {
                        Icon(
                            if (showRegPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }); Box {
                OutlinedButton(
                    onClick = { roleExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (regRole == "admin") "Admin" else "Employee"); Spacer(Modifier.weight(1f)); Icon(
                    Icons.Default.ArrowDropDown,
                    null
                )
                }; DropdownMenu(
                expanded = roleExpanded,
                onDismissRequest = { roleExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Employee") },
                    onClick = { regRole = "user"; roleExpanded = false }); DropdownMenuItem(
                text = { Text("Admin") },
                onClick = { regRole = "admin"; roleExpanded = false })
            }
            }; Box {
                OutlinedButton(
                    onClick = { branchExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        regBranch?.name ?: "Select Branch"
                    ); Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null)
                }; DropdownMenu(
                expanded = branchExpanded,
                onDismissRequest = { branchExpanded = false }) {
                availableBranches.forEach { b ->
                    DropdownMenuItem(
                        text = { Text(b.name) },
                        onClick = { regBranch = b; branchExpanded = false })
                }
            }
            }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (regName.isNotBlank() && regEmail.isNotBlank() && regPassword.isNotBlank() && regBranch != null) onRegister(
                    regEmail,
                    regPassword,
                    regName,
                    regRole,
                    regBranch!!.id
                )
            },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun EmployeeCard(
    employeeName: String,
    employeeCode: String,
    track: EmployeeTrack,
    onToggle: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit
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
                    val initials = employeeName.split(" ").filter { it.isNotBlank() }.take(2)
                        .map { it.firstOrNull()?.uppercase() ?: "" }.joinToString(""); Text(
                    initials,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                }; Spacer(Modifier.width(10.dp)); Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        employeeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    ); IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = RedColor.copy(0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                }; Text(
                "Code: $employeeCode",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            }; Text(
                formatDuration(track.totalWHSeconds),
                fontWeight = FontWeight.Bold,
                color = AmberColor,
                fontSize = 13.sp
            )
            }; Spacer(Modifier.height(6.dp)); HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(
                0.05f
            )
        ); Spacer(
            Modifier.height(6.dp)
        ); Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PhaseBtn(
                "Prep",
                track.preparation,
                Modifier.weight(1f),
                { onToggle("preparation") },
                { onEdit("preparation") }); PhaseBtn(
            "Cycle",
            track.cycleCount,
            Modifier.weight(1f),
            { onToggle("cycleCount") },
            { onEdit("cycleCount") }); PhaseBtn(
            "Load",
            track.loading,
            Modifier.weight(1f),
            { onToggle("loading") },
            { onEdit("loading") })
        }
        }
    }
}

@Composable
fun VehicleCard(
    track: VehicleTrack,
    onToggle: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit
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
                    Icon(
                        Icons.Default.LocalShipping,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            track.type,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = RedColor.copy(0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VPhaseBtn(
                    "Waiting",
                    track.waiting,
                    Modifier.weight(1f),
                    { onToggle("waiting") },
                    { onEdit("waiting") })
                VPhaseBtn(
                    "Offload",
                    track.offloading,
                    Modifier.weight(1f),
                    { onToggle("offloading") },
                    { onEdit("offloading") })
            }
        }
    }
}

@Composable
fun VPhaseBtn(
    label: String,
    data: PhaseData,
    modifier: Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (data.isActive) GreenColor else MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                if (data.isActive) "Stop" else "Start",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(2.dp))
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onEdit() },
            color = Color.Transparent
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatDuration(data.accumulatedSeconds),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (data.isActive) {
                    Text("Started ${data.currentStartTime}", fontSize = 8.sp, color = GreenColor)
                } else {
                    data.history.lastOrNull()?.let {
                        Text(
                            "Last end ${it.endTime}",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Tap to edit",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun PhaseBtn(
    label: String,
    data: PhaseData,
    modifier: Modifier,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (data.isActive) GreenColor else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                if (data.isActive) "Stop" else "Start",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(2.dp))
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onEdit() },
            color = Color.Transparent
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatDuration(data.accumulatedSeconds),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (data.isActive) {
                    Text("Started ${data.currentStartTime}", fontSize = 8.sp, color = GreenColor)
                } else {
                    data.history.lastOrNull()?.let {
                        Text(
                            "Last end ${it.endTime}",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Tap to edit",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                }
            }
        }
    }
}
