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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.UnfoldMore
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

// ── Models ───────────────────────────────
data class Branch(val id: Int, val name: String)
data class Employee(val id: Int, val name: String, val code: String, val branchId: Int)

data class PhaseData(
    val accumulatedSeconds: Int = 0,
    val currentStartTime: String? = null
) {
    val isActive: Boolean get() = currentStartTime != null
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

    fun nowTime() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    fun togglePhase(empId: Int, type: PhaseType) {
        val track = trackingData[empId] ?: return
        val now = nowTime()
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

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
                val startParsed = fmt.parse(startTime)
                val nowParsed = fmt.parse(now)
                val diffSeconds = ((nowParsed!!.time - startParsed!!.time) / 1000).toInt()
                currentPhase.copy(
                    currentStartTime = null,
                    accumulatedSeconds = currentPhase.accumulatedSeconds + diffSeconds
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

    val branchEmployees = employees.filter { it.branchId == selectedBranch.id }
    val activeCount = branchEmployees.count {
        val t = trackingData[it.id] ?: return@count false
        t.preparation.isActive || t.cycleCount.isActive || t.loading.isActive
    }
    val totalSeconds = branchEmployees.sumOf { trackingData[it.id]?.totalWHSeconds ?: 0 }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(LightBg)
        .statusBarsPadding()) {
        // ── Header ──────────────────────────
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(NavyBlue)
            .padding(16.dp)) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text("Warehouse Management", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${selectedBranch.name} Branch — $today", color = Color.White.copy(0.7f), fontSize = 12.sp)
            }
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { showAddBranchDialog = true }) {
                    Icon(Icons.Default.AddHome, contentDescription = "Add Branch", tint = Color.White)
                }
                IconButton(onClick = { showAddEmployeeDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Employee", tint = Color.White)
                }
            }
        }

        // ── Styled Branch Selector ──────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { branchDropdownExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = NavyBlue.copy(0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = NavyBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Selected Branch",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            selectedBranch.name,
                            fontSize = 16.sp,
                            color = NavyBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(Icons.Default.UnfoldMore, null, tint = Color.Gray)
                }

                DropdownMenu(
                    expanded = branchDropdownExpanded,
                    onDismissRequest = { branchDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color.White)
                ) {
                    branches.forEach { branch ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    branch.name,
                                    fontWeight = if (selectedBranch.id == branch.id) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Store,
                                    null,
                                    tint = if (selectedBranch.id == branch.id) GreenColor else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                selectedBranch = branch
                                branchDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(Modifier.weight(1f), "${branchEmployees.size}", "Employees", NavyBlue)
                    MetricCard(Modifier.weight(1f), "$activeCount", "Active", GreenColor)
                    MetricCard(Modifier.weight(1f), formatDuration(totalSeconds), "Total Time", AmberColor)
                }
            }

            items(branchEmployees, key = { it.id }) { emp ->
                EmployeeTrackingCard(
                    track = trackingData[emp.id] ?: EmployeeTrack(emp),
                    onAction = { type -> togglePhase(emp.id, type) }
                )
            }

            item { 
                BranchSummaryTable(
                    branchName = selectedBranch.name, 
                    employees = branchEmployees, 
                    trackingData = trackingData,
                    onExport = { exportToCSV(context, branches, employees, trackingData) }
                ) 
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    // ── Dialogs ──
    if (showAddBranchDialog) {
        var branchName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBranchDialog = false },
            title = { Text("Add New Branch") },
            text = { OutlinedTextField(value = branchName, onValueChange = { branchName = it }, label = { Text("Branch Name") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = { if (branchName.isNotBlank()) { val newId = (branches.maxOfOrNull { it.id } ?: 0) + 1; branches.add(Branch(newId, branchName)); showAddBranchDialog = false } }) { Text("Add") } }
        )
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = empName, onValueChange = { empName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = empCode, onValueChange = { empCode = it }, label = { Text("Code") }, modifier = Modifier.fillMaxWidth())
                    Box {
                        OutlinedButton(onClick = { empExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Branch: ${empBranch.name}")
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = empExpanded, onDismissRequest = { empExpanded = false }) {
                            branches.forEach { b -> DropdownMenuItem(text = { Text(b.name) }, onClick = { empBranch = b; empExpanded = false }) }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { if (empName.isNotBlank()) { val id = (employees.maxOfOrNull { it.id } ?: 0) + 1; val e = Employee(id, empName, empCode, empBranch.id); employees.add(e); trackingData[id] = EmployeeTrack(e); showAddEmployeeDialog = false } }) { Text("Add") } }
        )
    }
}

@Composable
fun EmployeeTrackingCard(track: EmployeeTrack, onAction: (PhaseType) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(NavyBlue.copy(0.1f)), contentAlignment = Alignment.Center) {
                    val initials = track.employee.name.split(" ").take(2).map { it.firstOrNull() ?: "" }.joinToString("")
                    Text(initials, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.employee.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Code: ${track.employee.code}", color = Color.Gray, fontSize = 12.sp)
                }
                Text("Total: ${formatDuration(track.totalWHSeconds)}", fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 13.sp)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.Gray.copy(0.1f))
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhaseControl(PhaseType.PREPARATION, track.preparation, Modifier.weight(1f)) { onAction(PhaseType.PREPARATION) }
                PhaseControl(PhaseType.CYCLE_COUNT, track.cycleCount, Modifier.weight(1f)) { onAction(PhaseType.CYCLE_COUNT) }
                PhaseControl(PhaseType.LOADING, track.loading, Modifier.weight(1f)) { onAction(PhaseType.LOADING) }
            }
        }
    }
}

@Composable
fun PhaseControl(type: PhaseType, data: PhaseData, modifier: Modifier, onClick: () -> Unit) {
    val isActive = data.isActive
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(type.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Button(onClick = onClick, modifier = Modifier
            .fillMaxWidth()
            .height(36.dp), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isActive) GreenColor else NavyBlue.copy(0.8f))) {
            Text(text = if (isActive) "OUT" else "IN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(formatDuration(data.accumulatedSeconds), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
        if (isActive) { Text("Start: ${data.currentStartTime}", fontSize = 9.sp, color = GreenColor) }
    }
}

@Composable
fun BranchSummaryTable(branchName: String, employees: List<Employee>, trackingData: Map<Int, EmployeeTrack>, onExport: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NavyBlue)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("BRANCH SUMMARY - ${branchName.uppercase()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                IconButton(onClick = onExport, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = Color.White, modifier = Modifier.size(18.dp))
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
            HorizontalDivider(color = Color.White.copy(0.2f))
            employees.forEach { emp ->
                val t = trackingData[emp.id] ?: return@forEach
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(emp.name, color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1.2f), maxLines = 1)
                    Text(formatDuration(t.preparation.accumulatedSeconds), color = Color.White, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(formatDuration(t.cycleCount.accumulatedSeconds), color = Color.White, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(formatDuration(t.loading.accumulatedSeconds), color = Color.White, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(formatDuration(t.totalWHSeconds), color = AmberColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier, value: String, label: String, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

fun exportToCSV(context: Context, branches: List<Branch>, employees: List<Employee>, trackingData: Map<Int, EmployeeTrack>) {
    val header = "Branch,Name,Code,Prep & Check (s),Cycle Count (s),Loading (s),Total (s),Formatted Total\n"
    val sb = StringBuilder(header)
    employees.forEach { emp ->
        val branchName = branches.find { it.id == emp.branchId }?.name ?: ""
        val track = trackingData[emp.id]
        if (track != null) {
            sb.append("${branchName},${emp.name},${emp.code},")
            sb.append("${track.preparation.accumulatedSeconds},")
            sb.append("${track.cycleCount.accumulatedSeconds},")
            sb.append("${track.loading.accumulatedSeconds},")
            sb.append("${track.totalWHSeconds},")
            sb.append("${formatDuration(track.totalWHSeconds)}\n")
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
