package com.example.warehousetracker.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.warehousetracker.LightBg
import com.example.warehousetracker.NavyBlue
import com.example.warehousetracker.RedColor
import com.example.warehousetracker.data.model.Branch
import com.example.warehousetracker.data.model.Employee
import com.example.warehousetracker.data.model.EmployeeTrack
import com.example.warehousetracker.data.model.PhaseData
import com.example.warehousetracker.data.model.VehicleTrack
import com.example.warehousetracker.formatDuration
import com.example.warehousetracker.ui.viewmodel.AuthViewModel
import com.example.warehousetracker.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

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
        val targetPage = if (state.activeTab == "outbound") 0 else 1 // Swapped: outbound is page 0
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Update ViewModel when pager is swiped
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val targetTab = if (page == 0) "outbound" else "inbound" // Swapped
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlue)
    ) {
        // ── Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 16.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Text(
                    "Warehouse Manager",
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
                .padding(top = 24.dp)) {
                IconButton(onClick = { showAddBranchDialog = true }) {
                    Icon(
                        Icons.Default.AddHome,
                        null,
                        tint = Color.White
                    )
                }
                IconButton(onClick = {
                    if (state.activeTab == "outbound") showAddEmployeeDialog =
                        true else showAddVehicleDialog = true
                }) {
                    Icon(
                        if (state.activeTab == "outbound") Icons.Default.PersonAdd else Icons.Default.LocalShipping,
                        null,
                        tint = Color.White
                    )
                }
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

        // ── Tab Switcher (Outbound / Inbound) ──
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
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = LightBg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── FIXED Top Section ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Branch Selector
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
                                .clickable { branchDropdownExpanded = true }
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
                                Text("Location", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    state.selectedBranch?.name ?: "Select Branch",
                                    fontSize = 16.sp,
                                    color = NavyBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                if (branchDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                null,
                                tint = NavyBlue
                            )
                        }
                        DropdownMenu(
                            expanded = branchDropdownExpanded,
                            onDismissRequest = { branchDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .background(Color.White)
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
                                                Text(branch.name, fontWeight = FontWeight.Medium)
                                                Text(
                                                    "$empCount Employees",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    branchToDelete = branch
                                                    branchDropdownExpanded = false
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    null,
                                                    tint = RedColor.copy(0.6f),
                                                    modifier = Modifier.size(18.dp)
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
                                // OUTBOUND CONTENT (Employees - Swapped from Inbound)
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
                                        EmployeeCard(
                                            emp.name,
                                            emp.code,
                                            track,
                                            { phase ->
                                                dashboardViewModel.togglePhase(
                                                    emp.id,
                                                    phase
                                                )
                                            },
                                            { p ->
                                                manualTimeDialogData = Triple(emp.id, p, false)
                                            },
                                            { employeeToDelete = emp })
                                    }
                                }
                            } else {
                                // INBOUND CONTENT (Vehicles - Swapped from Outbound)
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
                                        VehicleCard(
                                            track,
                                            { phase ->
                                                dashboardViewModel.toggleVehiclePhase(
                                                    track.vehicleId,
                                                    phase
                                                )
                                            },
                                            { phase ->
                                                manualTimeDialogData =
                                                    Triple(track.vehicleId, phase, true)
                                            },
                                            { vehicleTrackToDelete = track })
                                    }
                                }
                            }
                        }

                        // FIXED Summary Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightBg)
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            if (pagerState.currentPage == 0) {
                                SummaryCard(
                                    state.selectedBranch?.name ?: "",
                                    state.employees,
                                    state.tracks,
                                    { showExportScreen = true })
                            } else {
                                VehicleSummaryCard(
                                    state.selectedBranch?.name ?: "",
                                    state.vehicleTracks,
                                    { showExportScreen = true })
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
                        dashboardViewModel.addBranch(branchName, context); showAddBranchDialog =
                            false
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
                    if (empName.isNotBlank()) {
                        dashboardViewModel.addEmployee(
                            empName,
                            empCode,
                            state.selectedBranch!!.id,
                            context
                        ); showAddEmployeeDialog = false
                    }
                }) { Text("Add") }
            }
        )
    }

    if (showAddVehicleDialog) {
        var vType by remember { mutableStateOf("Actros") }
        var plate by remember { mutableStateOf("") }
        val types = listOf("Actros", "Axor", "NQR")
        AlertDialog(
            onDismissRequest = { showAddVehicleDialog = false },
            title = { Text("Add Vehicle Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Type:", fontSize = 12.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEach { t ->
                            Button(
                                onClick = { vType = t },
                                colors = ButtonDefaults.buttonColors(containerColor = if (vType == t) NavyBlue else Color.LightGray)
                            ) { Text(t, fontSize = 11.sp) }
                        }
                    }
                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = it },
                        label = { Text("Plate Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (plate.isNotBlank()) {
                        dashboardViewModel.addVehicle(vType, plate, context); showAddVehicleDialog =
                            false
                    }
                }) { Text("Enter Branch") }
            }
        )
    }

    if (employeeToDelete != null) {
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            title = { Text("Delete Employee") },
            text = { Text("Delete ${employeeToDelete?.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        dashboardViewModel.deleteEmployee(employeeToDelete!!.id); employeeToDelete =
                        null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedColor)
                ) { Text("Delete") }
            }
        )
    }

    if (vehicleTrackToDelete != null) {
        AlertDialog(
            onDismissRequest = { vehicleTrackToDelete = null },
            title = { Text("Delete Entry") },
            text = { Text("Delete entry for ${vehicleTrackToDelete?.type} (${vehicleTrackToDelete?.plateNumber})?") },
            confirmButton = {
                Button(
                    onClick = {
                        dashboardViewModel.deleteVehicle(vehicleTrackToDelete!!.vehicleId); vehicleTrackToDelete =
                        null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedColor)
                ) { Text("Delete") }
            }
        )
    }

    manualTimeDialogData?.let { (id, phase, isVehicle) ->
        var addMin by remember { mutableStateOf("") }
        var addSec by remember { mutableStateOf("") }
        var startHH by remember { mutableStateOf("") }
        var startMM by remember { mutableStateOf("") }
        var startAMPM by remember { mutableStateOf("AM") }
        var endHH by remember { mutableStateOf("") }
        var endMM by remember { mutableStateOf("") }
        var endAMPM by remember { mutableStateOf("PM") }

        AlertDialog(
            onDismissRequest = { manualTimeDialogData = null },
            title = { Text("Adjust Time") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Column {
                            Text("Record Session", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                OutlinedTextField(
                                    value = startHH,
                                    onValueChange = { if (it.length <= 2) startHH = it },
                                    label = { Text("Start H") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Spacer(Modifier.width(4.dp))
                                OutlinedTextField(
                                    value = startMM,
                                    onValueChange = { if (it.length <= 2) startMM = it },
                                    label = { Text("M") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Spacer(Modifier.width(4.dp))
                                TextButton(onClick = {
                                    startAMPM = if (startAMPM == "AM") "PM" else "AM"
                                }) { Text(startAMPM) }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                OutlinedTextField(
                                    value = endHH,
                                    onValueChange = { if (it.length <= 2) endHH = it },
                                    label = { Text("End H") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Spacer(Modifier.width(4.dp))
                                OutlinedTextField(
                                    value = endMM,
                                    onValueChange = { if (it.length <= 2) endMM = it },
                                    label = { Text("M") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Spacer(Modifier.width(4.dp))
                                TextButton(onClick = {
                                    endAMPM = if (endAMPM == "AM") "PM" else "AM"
                                }) { Text(endAMPM) }
                            }
                        }
                    }
                    item { HorizontalDivider(color = LightBg) }
                    item {
                        Column {
                            Text(
                                "Or Add Extra Time",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = addMin,
                                    onValueChange = { addMin = it },
                                    label = { Text("Min") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = addSec,
                                    onValueChange = { addSec = it },
                                    label = { Text("Sec") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        if (isVehicle) dashboardViewModel.resetVehiclePhase(
                            id,
                            phase
                        ) else dashboardViewModel.resetPhase(id, phase)
                        manualTimeDialogData = null
                    }) { Text("Reset", color = RedColor) }
                    Button(onClick = {
                        val mAdd = (addMin.toIntOrNull() ?: 0) * 60 + (addSec.toIntOrNull() ?: 0)
                        var mStart: String? = null
                        var mEnd: String? = null
                        if (startHH.isNotBlank() && startMM.isNotBlank() && endHH.isNotBlank() && endMM.isNotBlank()) {
                            mStart = "${startHH.padStart(2, '0')}:${
                                startMM.padStart(
                                    2,
                                    '0'
                                )
                            }:00 $startAMPM"
                            mEnd = "${endHH.padStart(2, '0')}:${endMM.padStart(2, '0')}:00 $endAMPM"
                        }
                        dashboardViewModel.applyManualTime(id, phase, mAdd, mStart, mEnd, isVehicle)
                        manualTimeDialogData = null
                    }) { Text("Apply") }
                }
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
    onExportRange: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyBlue),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "OUTBOUND SUMMARY - ${branchName.uppercase()}", // Text Swapped
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                IconButton(
                    onClick = onExportRange,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    "Name",
                    color = Color.White.copy(0.6f),
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    "Prep",
                    color = Color.White.copy(0.6f),
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Cycle",
                    color = Color.White.copy(0.6f),
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Load",
                    color = Color.White.copy(0.6f),
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Total",
                    color = Color.White.copy(0.6f),
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1.1f),
                    textAlign = TextAlign.Center
                )
            }
            HorizontalDivider(color = Color.White.copy(0.1f))
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 110.dp)
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
                            fontSize = 9.sp,
                            modifier = Modifier.weight(1.2f),
                            maxLines = 1
                        )
                        Text(
                            formatDuration(t.preparation.accumulatedSeconds),
                            color = Color.White,
                            fontSize = 8.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            formatDuration(t.cycleCount.accumulatedSeconds),
                            color = Color.White,
                            fontSize = 8.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            formatDuration(t.loading.accumulatedSeconds),
                            color = Color.White,
                            fontSize = 8.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            formatDuration(t.totalWHSeconds),
                            color = AmberColor,
                            fontSize = 9.sp,
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
                    color = Color.White.copy(0.2f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "TOTAL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        formatDuration(totalWH),
                        color = AmberColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleSummaryCard(
    branchName: String,
    vehicleTracks: List<VehicleTrack>,
    onExportRange: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyBlue),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "INBOUND SUMMARY - ${branchName.uppercase()}", // Text Swapped
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                IconButton(onClick = onExportRange, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Share,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)) {
                Text(
                    "Vehicle",
                    color = Color.White.copy(0.6f),
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    "Waiting",
                    color = Color.White.copy(0.6f),
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Offload",
                    color = Color.White.copy(0.6f),
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Total",
                    color = Color.White.copy(0.6f),
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
            HorizontalDivider(color = Color.White.copy(0.1f))
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 80.dp)
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
                            fontSize = 9.sp,
                            modifier = Modifier.weight(1.2f),
                            maxLines = 1
                        )
                        Text(
                            formatDuration(t.waiting.accumulatedSeconds),
                            color = Color.White,
                            fontSize = 8.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            formatDuration(t.offloading.accumulatedSeconds),
                            color = Color.White,
                            fontSize = 8.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            formatDuration(t.totalSeconds),
                            color = AmberColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier, value: String, label: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
            Text(label, fontSize = 9.sp, color = Color.Gray)
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
                            tint = Color.Gray
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
            }, colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)) { Text("Create") }
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
                    val initials = employeeName.split(" ").filter { it.isNotBlank() }.take(2)
                        .map { it.firstOrNull()?.uppercase() ?: "" }.joinToString(""); Text(
                    initials,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )
                }; Spacer(Modifier.width(10.dp)); Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(employeeName, fontWeight = FontWeight.Bold, fontSize = 14.sp); IconButton(
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
                }; Text("Code: $employeeCode", color = Color.Gray, fontSize = 11.sp)
            }; Text(
                formatDuration(track.totalWHSeconds),
                fontWeight = FontWeight.Bold,
                color = NavyBlue,
                fontSize = 12.sp
            )
            }; Spacer(Modifier.height(10.dp)); HorizontalDivider(color = NavyBlue.copy(0.05f)); Spacer(
            Modifier.height(10.dp)
        ); Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PhaseBtn(
                "preparation",
                "Prep",
                track.preparation,
                Modifier.weight(1f),
                { onToggle("preparation") },
                { onEdit("preparation") }); PhaseBtn(
            "cycleCount",
            "Cycle",
            track.cycleCount,
            Modifier.weight(1f),
            { onToggle("cycleCount") },
            { onEdit("cycleCount") }); PhaseBtn(
            "loading",
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(track.type, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (data.isActive) GreenColor else NavyBlue.copy(0.8f)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(if (data.isActive) "OUT" else "IN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onEdit() },
            color = Color.Transparent
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatDuration(data.accumulatedSeconds),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )
                if (data.isActive) {
                    Text("Start: ${data.currentStartTime}", fontSize = 8.sp, color = GreenColor)
                } else {
                    data.history.lastOrNull()?.let {
                        Text("End: ${it.endTime}", fontSize = 8.sp, color = Color.Gray)
                    }
                    Text("Tap to edit", fontSize = 8.sp, color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun PhaseBtn(
    key: String,
    label: String,
    data: PhaseData,
    modifier: Modifier,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (data.isActive) GreenColor else NavyBlue.copy(0.8f)
            )
        ) {
            Text(if (data.isActive) "OUT" else "IN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onEdit() },
            color = Color.Transparent
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatDuration(data.accumulatedSeconds),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )
                if (data.isActive) {
                    Text("Start: ${data.currentStartTime}", fontSize = 8.sp, color = GreenColor)
                } else {
                    data.history.lastOrNull()?.let {
                        Text("End: ${it.endTime}", fontSize = 8.sp, color = Color.Gray)
                    }
                    Text("Tap to edit", fontSize = 8.sp, color = Color.LightGray)
                }
            }
        }
    }
}
