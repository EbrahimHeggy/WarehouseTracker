package com.example.warehousetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Colors ───────────────────────────────
val NavyBlue = Color(0xFF1a3a5c)
val LightBg = Color(0xFFF4F6F9)
val GreenColor = Color(0xFF4CAF50)
val RedColor = Color(0xFFE53935)
val AmberColor = Color(0xFFFFA000)

// ── Helper Functions ─────────────────────
fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

// ── Models ───────────────────────────────
data class Branch(val id: Int, val name: String)
data class Employee(val id: Int, val name: String, val code: String, val branchId: Int)

data class WorkSession(
    val startTime: String,
    val endTime: String? = null,
    val durationSeconds: Int = 0
)

data class PhaseData(
    val history: List<WorkSession> = listOf(),
    val currentStartTime: String? = null
) {
    val isActive: Boolean get() = currentStartTime != null
    val accumulatedSeconds: Int get() = history.sumOf { it.durationSeconds }
}

data class EmployeeTrack(
    val employee: Employee,
    val preparation: PhaseData = PhaseData(),
    val cycleCount: PhaseData = PhaseData(),
    val loading: PhaseData = PhaseData()
) {
    val totalWHSeconds: Int get() = preparation.accumulatedSeconds + cycleCount.accumulatedSeconds + loading.accumulatedSeconds
}

enum class PhaseType(val label: String) {
    PREPARATION("Prep & Check"),
    CYCLE_COUNT("Cycle Count"),
    LOADING("Loading")
}

// ── Initial Mock Data ────────────────────
val initialBranches = listOf(Branch(1, "Luxor"), Branch(2, "Cairo"), Branch(3, "Aswan"))
val initialEmployees = listOf(
    Employee(1, "Ibrahim Desouky", "1046", 1),
    Employee(2, "Ali El Noby", "2352", 1),
    Employee(3, "George Zakaria", "4068", 1),
    Employee(4, "Hassan Mohamed", "2011", 2),
    Employee(5, "Nour Mahmoud", "3021", 2),
    Employee(6, "Youssef Ali", "1533", 2),
    Employee(7, "Mona Samir", "4401", 3),
    Employee(8, "Khaled Omar", "5512", 3),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WarehouseTrackerApp() }
    }
}

@Composable
fun WarehouseTrackerApp() {
    val context = LocalContext.current
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    
    val branches = remember { mutableStateListOf<Branch>().apply { addAll(initialBranches) } }
    val employees = remember { mutableStateListOf<Employee>().apply { addAll(initialEmployees) } }
    
    var selectedBranch by remember { mutableStateOf(branches[0]) }
    var branchDropdownExpanded by remember { mutableStateOf(false) }

    val trackingData = remember {
        mutableStateMapOf<Int, EmployeeTrack>().apply {
            employees.forEach { put(it.id, EmployeeTrack(it)) }
        }
    }
    var sessionCounter by remember { mutableIntStateOf(1) }

    var showAddBranchDialog by remember { mutableStateOf(false) }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var manualTimeDialogData by remember { mutableStateOf<Pair<Int, PhaseType>?>(null) }

    val timeFormatter = SimpleDateFormat("hh:mm:ss a", Locale.ENGLISH)

    fun nowTime() = timeFormatter.format(Date())

    fun togglePhase(empId: Int, type: PhaseType) {
        val track = trackingData[empId] ?: return
        val now = nowTime()

        val currentPhase = when (type) {
            PhaseType.PREPARATION -> track.preparation
            PhaseType.CYCLE_COUNT -> track.cycleCount
            PhaseType.LOADING -> track.loading
        }

        val newPhase = if (!currentPhase.isActive) {
            currentPhase.copy(currentStartTime = now)
        } else {
            val startTime = currentPhase.currentStartTime!!
            try {
                val startParsed = timeFormatter.parse(startTime)
                val nowParsed = timeFormatter.parse(now)
                val diffSeconds = ((nowParsed!!.time - startParsed!!.time) / 1000).toInt()
                val newSession = WorkSession(startTime, now, diffSeconds)
                currentPhase.copy(
                    currentStartTime = null,
                    history = currentPhase.history + newSession
                )
            } catch (e: Exception) {
                currentPhase.copy(currentStartTime = null)
            }
        }

        trackingData[empId] = when (type) {
            PhaseType.PREPARATION -> track.copy(preparation = newPhase)
            PhaseType.CYCLE_COUNT -> track.copy(cycleCount = newPhase)
            PhaseType.LOADING -> track.copy(loading = newPhase)
        }
    }

    fun applyManualAdjustment(
        empId: Int,
        type: PhaseType,
        secondsToAdd: Int,
        manualStart: String?,
        manualEnd: String?,
        reset: Boolean
    ) {
        val track = trackingData[empId] ?: return
        if (reset) {
            trackingData[empId] = when (type) {
                PhaseType.PREPARATION -> track.copy(preparation = PhaseData())
                PhaseType.CYCLE_COUNT -> track.copy(cycleCount = PhaseData())
                PhaseType.LOADING -> track.copy(loading = PhaseData())
            }
            return
        }

        val currentPhase = when (type) {
            PhaseType.PREPARATION -> track.preparation
            PhaseType.CYCLE_COUNT -> track.cycleCount
            PhaseType.LOADING -> track.loading
        }

        val manualSessions = mutableListOf<WorkSession>()
        if (manualStart != null && manualEnd != null) {
            try {
                val diff =
                    ((timeFormatter.parse(manualEnd)!!.time - timeFormatter.parse(manualStart)!!.time) / 1000).toInt()
                manualSessions.add(WorkSession(manualStart, manualEnd, diff))
            } catch (e: Exception) {
            }
        } else if (secondsToAdd > 0) {
            manualSessions.add(WorkSession("Manual", "Manual", secondsToAdd))
        }

        val updatedPhase = currentPhase.copy(
            history = currentPhase.history + manualSessions
        )

        trackingData[empId] = when (type) {
            PhaseType.PREPARATION -> track.copy(preparation = updatedPhase)
            PhaseType.CYCLE_COUNT -> track.copy(cycleCount = updatedPhase)
            PhaseType.LOADING -> track.copy(loading = updatedPhase)
        }
    }

    val branchEmployees = employees.filter { it.branchId == selectedBranch.id }
    val activeCount = branchEmployees.count {
        val t = trackingData[it.id] ?: return@count false
        t.preparation.isActive || t.cycleCount.isActive || t.loading.isActive
    }
    val totalSeconds = branchEmployees.sumOf { trackingData[it.id]?.totalWHSeconds ?: 0 }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(NavyBlue)
        .statusBarsPadding()) {
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
                Text("${selectedBranch.name} Branch — $today", color = Color.White.copy(0.7f), fontSize = 12.sp)
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
                // Beautiful Styled Branch Selector
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
                                    selectedBranch.name,
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
                            branches.forEach { branch ->
                                val isSelected = selectedBranch.id == branch.id
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
                                        selectedBranch = branch; branchDropdownExpanded = false
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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
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
                            MetricCard(Modifier.weight(1f), "$activeCount", "Active", GreenColor)
                            MetricCard(
                                Modifier.weight(1f),
                                formatDuration(totalSeconds),
                                "Total Time",
                                AmberColor
                            )
                        }
                    }
                    items(branchEmployees, key = { it.id }) { emp ->
                        EmployeeTrackingCard(
                            track = trackingData[emp.id] ?: EmployeeTrack(emp),
                            onToggle = { type -> togglePhase(emp.id, type) },
                            onEdit = { type -> manualTimeDialogData = emp.id to type })
                    }
                    item {
                        BranchSummaryTable(
                            branchName = selectedBranch.name,
                            employees = branchEmployees,
                            trackingData = trackingData,
                            onExport = {
                                exportDetailedCSV(
                                    context,
                                    branches,
                                    employees,
                                    trackingData
                                )
                            })
                    }
                }
            }
        }
    }

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
                        val newId = (branches.maxOfOrNull { it.id } ?: 0) + 1; branches.add(
                            Branch(
                                newId,
                                branchName
                            )
                        ); showAddBranchDialog = false
                    }
                }) { Text("Add") }
            })
    }

    if (showAddEmployeeDialog) {
        var empName by remember { mutableStateOf("") }
        var empCode by remember { mutableStateOf("") }
        var empBranch by remember { mutableStateOf(selectedBranch) }
        var empExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            title = { Text("Add New Employee") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    Column {
                        Text(
                            "Select Branch",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { empExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                        ) {
                            Box {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Store,
                                        null,
                                        tint = NavyBlue,
                                        modifier = Modifier.size(20.dp)
                                    ); Spacer(Modifier.width(12.dp))
                                    Text(
                                        empBranch.name,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyBlue
                                    ); Spacer(Modifier.weight(1f)); Icon(
                                    Icons.Default.ArrowDropDown,
                                    null,
                                    tint = NavyBlue
                                )
                                }
                                DropdownMenu(
                                    expanded = empExpanded,
                                    onDismissRequest = { empExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .background(Color.White)
                                ) {
                                    branches.forEach { b ->
                                        DropdownMenuItem(
                                            text = { Text(b.name) },
                                            onClick = { empBranch = b; empExpanded = false })
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (empName.isNotBlank()) {
                        val id = (employees.maxOfOrNull { it.id } ?: 0) + 1;
                        val e = Employee(
                            id,
                            empName,
                            empCode,
                            empBranch.id
                        ); employees.add(e); trackingData[id] =
                            EmployeeTrack(e); showAddEmployeeDialog = false
                    }
                }) { Text("Add") }
            })
    }

    manualTimeDialogData?.let { (empId, phaseType) ->
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
            title = { Text("Adjust Time (12h format)") },
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
                        applyManualAdjustment(
                            empId,
                            phaseType,
                            0,
                            null,
                            null,
                            true
                        ); manualTimeDialogData = null
                    }) { Text("Reset", color = RedColor) }
                    Button(onClick = {
                        val mAdd = (addMin.toIntOrNull() ?: 0) * 60 + (addSec.toIntOrNull() ?: 0)
                        var mStart: String? = null;
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
                        applyManualAdjustment(
                            empId,
                            phaseType,
                            mAdd,
                            mStart,
                            mEnd,
                            false
                        ); manualTimeDialogData = null
                    }) { Text("Apply") }
                }
            })
    }
}

@Composable
fun EmployeeTrackingCard(
    track: EmployeeTrack,
    onToggle: (PhaseType) -> Unit,
    onEdit: (PhaseType) -> Unit
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
                        .background(NavyBlue.copy(0.1f)), contentAlignment = Alignment.Center
                ) {
                    val initials = track.employee.name.split(" ").filter { it.isNotBlank() }.take(2)
                        .map { it.firstOrNull()?.uppercase() ?: "" }.joinToString("")
                    Text(initials, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.employee.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Code: ${track.employee.code}", color = Color.Gray, fontSize = 12.sp)
                }
                Text("Total: ${formatDuration(track.totalWHSeconds)}", fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp)); HorizontalDivider(color = NavyBlue.copy(0.05f)); Spacer(
            Modifier.height(12.dp)
        )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhaseControl(
                    PhaseType.PREPARATION,
                    track.preparation,
                    Modifier.weight(1f),
                    onToggle = { onToggle(PhaseType.PREPARATION) },
                    onEdit = { onEdit(PhaseType.PREPARATION) })
                PhaseControl(
                    PhaseType.CYCLE_COUNT,
                    track.cycleCount,
                    Modifier.weight(1f),
                    onToggle = { onToggle(PhaseType.CYCLE_COUNT) },
                    onEdit = { onEdit(PhaseType.CYCLE_COUNT) })
                PhaseControl(
                    PhaseType.LOADING,
                    track.loading,
                    Modifier.weight(1f),
                    onToggle = { onToggle(PhaseType.LOADING) },
                    onEdit = { onEdit(PhaseType.LOADING) })
            }
        }
    }
}

@Composable
fun PhaseControl(
    type: PhaseType,
    data: PhaseData,
    modifier: Modifier,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val isActive = data.isActive
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(type.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) GreenColor else NavyBlue.copy(0.8f)
            )
        ) {
            Text(
                text = if (isActive) "OUT" else "IN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
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
                if (isActive) Text(
                    "Start: ${data.currentStartTime}",
                    fontSize = 9.sp,
                    color = GreenColor
                ) else Text("Tap to edit", fontSize = 8.sp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun BranchSummaryTable(branchName: String, employees: List<Employee>, trackingData: Map<Int, EmployeeTrack>, onExport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyBlue),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("BRANCH SUMMARY - ${branchName.uppercase()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                IconButton(
                    onClick = onExport,
                    modifier = Modifier.size(24.dp)
                ) {
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
                Text("Name", color = Color.White.copy(0.6f), fontSize = 10.sp, modifier = Modifier.weight(1.2f))
                Text("Prep", color = Color.White.copy(0.6f), fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("Cycle", color = Color.White.copy(0.6f), fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("Load", color = Color.White.copy(0.6f), fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("Total", color = Color.White.copy(0.6f), fontSize = 10.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
            }
            HorizontalDivider(color = Color.White.copy(0.1f))
            employees.forEach { emp ->
                val t = trackingData[emp.id] ?: return@forEach
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
                    Text(formatDuration(t.preparation.accumulatedSeconds), color = Color.White, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(formatDuration(t.cycleCount.accumulatedSeconds), color = Color.White, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(formatDuration(t.loading.accumulatedSeconds), color = Color.White, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
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
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

fun exportDetailedCSV(
    context: Context,
    branches: List<Branch>,
    employees: List<Employee>,
    trackingData: Map<Int, EmployeeTrack>
) {
    val sb = StringBuilder()

    // 1. SUMMARY TABLE (Aggregated totals)
    sb.append("--- SUMMARY REPORT ---\n")
    sb.append("Branch,Name,Code,Prep&Check Total,Cycle Count Total,Loading Total,Total WH Time\n")
    
    employees.forEach { emp ->
        val branchName = branches.find { it.id == emp.branchId }?.name ?: ""
        val track = trackingData[emp.id] ?: return@forEach

        sb.append("${branchName},${emp.name},${emp.code},")
        sb.append("${formatDuration(track.preparation.accumulatedSeconds).replace(",", " ")},")
        sb.append("${formatDuration(track.cycleCount.accumulatedSeconds).replace(",", " ")},")
        sb.append("${formatDuration(track.loading.accumulatedSeconds).replace(",", " ")},")
        sb.append("${formatDuration(track.totalWHSeconds).replace(",", " ")}\n")
    }

    sb.append("\n\n") // Gap between tables

    // 2. DETAILED LOG TABLE (Every session's In/Out time)
    sb.append("--- DETAILED LOG (IN/OUT TIMES) ---\n")
    sb.append("Branch,Name,Code,Phase,In Time,Out Time,Duration\n")

    employees.forEach { emp ->
        val branchName = branches.find { it.id == emp.branchId }?.name ?: ""
        val track = trackingData[emp.id] ?: return@forEach

        // List of all phases for this employee
        val phases = listOf(
            "Prep & Check" to track.preparation,
            "Cycle Count" to track.cycleCount,
            "Loading" to track.loading
        )

        phases.forEach { (phaseName, data) ->
            // Past sessions
            data.history.forEach { session ->
                sb.append("${branchName},${emp.name},${emp.code},${phaseName},")
                sb.append(
                    "${session.startTime},${session.endTime ?: "-"},${
                        formatDuration(session.durationSeconds).replace(
                            ",",
                            " "
                        )
                    }\n"
                )
            }
            // Currently active session (not finished yet)
            if (data.isActive) {
                sb.append("${branchName},${emp.name},${emp.code},${phaseName},")
                sb.append("${data.currentStartTime},STILL IN,In Progress\n")
            }
        }
    }

    try {
        val fileName = "Warehouse_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText(sb.toString())
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    } catch (e: Exception) { e.printStackTrace() }
}
