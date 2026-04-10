package com.example.warehousetracker.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.warehousetracker.data.model.Employee
import com.example.warehousetracker.data.model.EmployeeTrack
import com.example.warehousetracker.data.model.PhaseData
import com.example.warehousetracker.formatDuration
import com.example.warehousetracker.ui.viewmodel.AuthViewModel
import com.example.warehousetracker.ui.viewmodel.DashboardViewModel

@Composable
fun AdminDashboardScreen(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by dashboardViewModel.state.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    var branchDropdownExpanded by remember { mutableStateOf(false) }
    var showAddBranchDialog by remember { mutableStateOf(false) }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var manualTimeDialogData by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlue)
            .statusBarsPadding()
    ) {
        // ── Header ──────────────────────────
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
                    "${state.selectedBranch?.name ?: ""} Branch — ${state.date}",
                    color = Color.White.copy(0.7f),
                    fontSize = 12.sp
                )
            }
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { showAddBranchDialog = true }) {
                    Icon(
                        Icons.Default.AddHome,
                        null,
                        tint = Color.White
                    )
                }
                IconButton(onClick = {
                    showAddEmployeeDialog = true
                }) { Icon(Icons.Default.PersonAdd, null, tint = Color.White) }
                IconButton(onClick = { authViewModel.logout() }) {
                    Icon(
                        Icons.Default.Logout,
                        null,
                        tint = Color.White
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = LightBg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Branch Selector ──────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { branchDropdownExpanded = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(NavyBlue.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    null,
                                    tint = NavyBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Current Location", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    state.selectedBranch?.name ?: "Select Branch",
                                    fontSize = 18.sp,
                                    color = NavyBlue,
                                    fontWeight = FontWeight.ExtraBold
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
                            Text(
                                "SWITCH BRANCH",
                                modifier = Modifier.padding(16.dp, 8.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            HorizontalDivider(color = NavyBlue.copy(alpha = 0.05f))
                            state.branches.forEach { branch ->
                                val isSelected = state.selectedBranch?.id == branch.id
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            branch.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (isSelected) Icons.Default.CheckCircle else Icons.Default.Store,
                                            null,
                                            tint = if (isSelected) GreenColor else Color.Gray
                                        )
                                    },
                                    onClick = {
                                        dashboardViewModel.selectBranch(branch); branchDropdownExpanded =
                                        false
                                    },
                                    modifier = Modifier.background(
                                        if (isSelected) NavyBlue.copy(
                                            alpha = 0.05f
                                        ) else Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NavyBlue)
                    }
                } else {
                    val branchEmployees = state.employees
                    val activeCount = branchEmployees.count { emp ->
                        val t = state.tracks[emp.id] ?: return@count false
                        t.preparation.isActive || t.cycleCount.isActive || t.loading.isActive
                    }
                    val totalSeconds =
                        branchEmployees.sumOf { state.tracks[it.id]?.totalWHSeconds ?: 0 }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Metrics
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricCard(
                                    Modifier.weight(1f),
                                    "${branchEmployees.size}",
                                    "Employees",
                                    NavyBlue
                                )
                                MetricCard(
                                    Modifier.weight(1f),
                                    "$activeCount",
                                    "Active",
                                    GreenColor
                                )
                                MetricCard(
                                    Modifier.weight(1f),
                                    formatDuration(totalSeconds),
                                    "Total Time",
                                    AmberColor
                                )
                            }
                        }

                        // Employee Cards
                        items(branchEmployees, key = { it.id }) { emp ->
                            val track = state.tracks[emp.id] ?: EmployeeTrack(
                                employeeId = emp.id,
                                date = state.date
                            )
                            EmployeeCard(
                                employeeName = emp.name,
                                employeeCode = emp.code,
                                track = track,
                                onToggle = { phase ->
                                    dashboardViewModel.togglePhase(
                                        emp.id,
                                        phase
                                    )
                                },
                                onEdit = { phase -> manualTimeDialogData = emp.id to phase }
                            )
                        }

                        // Summary
                        item {
                            SummaryCard(
                                branchName = state.selectedBranch?.name ?: "",
                                employees = branchEmployees,
                                tracks = state.tracks,
                                onExport = { dashboardViewModel.exportCSV(context) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Dialogs ──────────────────────────
    if (showAddBranchDialog) {
        var branchName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBranchDialog = false },
            title = { Text("Add New Branch") },
            text = {
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = { Text("Branch Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (branchName.isNotBlank()) {
                        dashboardViewModel.addBranch(branchName); showAddBranchDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddBranchDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    if (showAddEmployeeDialog) {
        var empName by remember { mutableStateOf("") }
        var empCode by remember { mutableStateOf("") }
        var empBranch by remember { mutableStateOf(state.selectedBranch) }
        var empExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            title = { Text("Add New Employee") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = empName,
                        onValueChange = { empName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = empCode,
                        onValueChange = { empCode = it },
                        label = { Text("Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box {
                        OutlinedButton(
                            onClick = { empExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(empBranch?.name ?: "Select Branch")
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(
                            expanded = empExpanded,
                            onDismissRequest = { empExpanded = false }) {
                            state.branches.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name) },
                                    onClick = { empBranch = b; empExpanded = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (empName.isNotBlank() && empBranch != null) {
                        dashboardViewModel.addEmployee(empName, empCode, empBranch!!.id)
                        showAddEmployeeDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddEmployeeDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    manualTimeDialogData?.let { (empId, phase) ->
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
                        dashboardViewModel.resetPhase(
                            empId,
                            phase
                        ); manualTimeDialogData = null
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
                        dashboardViewModel.applyManualTime(empId, phase, mAdd, mStart, mEnd)
                        manualTimeDialogData = null
                    }) { Text("Apply") }
                }
            }
        )
    }
}

// ── Employee Card ────────────────────────
@Composable
fun EmployeeCard(
    employeeName: String,
    employeeCode: String,
    track: EmployeeTrack,
    onToggle: (String) -> Unit,
    onEdit: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NavyBlue.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = employeeName.split(" ").filter { it.isNotBlank() }.take(2)
                        .map { it.firstOrNull()?.uppercase() ?: "" }.joinToString("")
                    Text(initials, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(employeeName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Code: $employeeCode", color = Color.Gray, fontSize = 12.sp)
                }
                Text(
                    "Total: ${formatDuration(track.totalWHSeconds)}",
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = NavyBlue.copy(0.05f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhaseBtn(
                    "preparation",
                    "Prep & Check",
                    track.preparation,
                    Modifier.weight(1f),
                    { onToggle("preparation") },
                    { onEdit("preparation") })
                PhaseBtn(
                    "cycleCount",
                    "Cycle Count",
                    track.cycleCount,
                    Modifier.weight(1f),
                    { onToggle("cycleCount") },
                    { onEdit("cycleCount") })
                PhaseBtn(
                    "loading",
                    "Loading",
                    track.loading,
                    Modifier.weight(1f),
                    { onToggle("loading") },
                    { onEdit("loading") })
            }
        }
    }
}

// ── Phase Button ─────────────────────────
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
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (data.isActive) GreenColor else NavyBlue.copy(
                    0.8f
                )
            )
        ) {
            Text(if (data.isActive) "OUT" else "IN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
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
                    color = NavyBlue
                )
                if (data.isActive) Text(
                    "Start: ${data.currentStartTime}",
                    fontSize = 9.sp,
                    color = GreenColor
                )
                else Text("Tap to edit", fontSize = 8.sp, color = Color.LightGray)
            }
        }
    }
}

// ── Summary Card ─────────────────────────
@Composable
fun SummaryCard(
    branchName: String,
    employees: List<Employee>,
    tracks: Map<String, EmployeeTrack>,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyBlue),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "BRANCH SUMMARY - ${branchName.uppercase()}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                IconButton(onClick = onExport, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Share,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)) {
                Text(
                    "Name",
                    color = Color.White.copy(0.6f),
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    "Prep",
                    color = Color.White.copy(0.6f),
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Cycle",
                    color = Color.White.copy(0.6f),
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Load",
                    color = Color.White.copy(0.6f),
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Total",
                    color = Color.White.copy(0.6f),
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center
                )
            }
            HorizontalDivider(color = Color.White.copy(0.1f))
            employees.forEach { emp ->
                val t = tracks[emp.id] ?: return@forEach
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        emp.name,
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1.2f),
                        maxLines = 1
                    )
                    Text(
                        formatDuration(t.preparation.accumulatedSeconds),
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        formatDuration(t.cycleCount.accumulatedSeconds),
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        formatDuration(t.loading.accumulatedSeconds),
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        formatDuration(t.totalWHSeconds),
                        color = AmberColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.2f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Metric Card ──────────────────────────
@Composable
fun MetricCard(modifier: Modifier, value: String, label: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}