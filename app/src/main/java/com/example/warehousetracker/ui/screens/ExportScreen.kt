package com.example.warehousetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.warehousetracker.LightBg
import com.example.warehousetracker.NavyBlue
import com.example.warehousetracker.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportScreen(
    dashboardViewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by dashboardViewModel.state.collectAsStateWithLifecycle()

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today) }
    var isExporting by remember { mutableStateOf(false) }
    var exportMode by remember { mutableStateOf("range") } // "today" / "range"

    // Date fields
    var startDay by remember { mutableStateOf(today.substring(8, 10)) }
    var startMonth by remember { mutableStateOf(today.substring(5, 7)) }
    var startYear by remember { mutableStateOf(today.substring(0, 4)) }

    var endDay by remember { mutableStateOf(today.substring(8, 10)) }
    var endMonth by remember { mutableStateOf(today.substring(5, 7)) }
    var endYear by remember { mutableStateOf(today.substring(0, 4)) }

    fun buildDate(day: String, month: String, year: String): String {
        return "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlue)
            .statusBarsPadding()
            .navigationBarsPadding() // AVOID SYSTEM BUTTONS
    ) {
        // Header
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Export Report (${state.activeTab.uppercase()})",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    state.selectedBranch?.name ?: "",
                    color = Color.White.copy(0.7f),
                    fontSize = 12.sp
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = LightBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeCard(
                        modifier = Modifier.weight(1f),
                        title = "Today",
                        subtitle = today,
                        icon = Icons.Default.Today,
                        isSelected = exportMode == "today",
                        onClick = {
                            exportMode = "today"
                            startDate = today
                            endDate = today
                        }
                    )
                    ModeCard(
                        modifier = Modifier.weight(1f),
                        title = "Date Range",
                        subtitle = "Custom period",
                        icon = Icons.Default.DateRange,
                        isSelected = exportMode == "range",
                        onClick = { exportMode = "range" }
                    )
                }

                // Date Range Inputs
                if (exportMode == "range") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {

                            // Start Date
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "From",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = startDay,
                                        onValueChange = {
                                            if (it.length <= 2) {
                                                startDay = it
                                                startDate = buildDate(it, startMonth, startYear)
                                            }
                                        },
                                        label = { Text("Day") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = startMonth,
                                        onValueChange = {
                                            if (it.length <= 2) {
                                                startMonth = it
                                                startDate = buildDate(startDay, it, startYear)
                                            }
                                        },
                                        label = { Text("Month") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = startYear,
                                        onValueChange = {
                                            if (it.length <= 4) {
                                                startYear = it
                                                startDate = buildDate(startDay, startMonth, it)
                                            }
                                        },
                                        label = { Text("Year") },
                                        modifier = Modifier.weight(1.5f),
                                        singleLine = true
                                    )
                                }
                                Text(
                                    startDate,
                                    fontSize = 12.sp,
                                    color = NavyBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            HorizontalDivider(color = Color.Gray.copy(0.1f))

                            // End Date
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "To",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = endDay,
                                        onValueChange = {
                                            if (it.length <= 2) {
                                                endDay = it
                                                endDate = buildDate(it, endMonth, endYear)
                                            }
                                        },
                                        label = { Text("Day") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = endMonth,
                                        onValueChange = {
                                            if (it.length <= 2) {
                                                endMonth = it
                                                endDate = buildDate(endDay, it, endYear)
                                            }
                                        },
                                        label = { Text("Month") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = endYear,
                                        onValueChange = {
                                            if (it.length <= 4) {
                                                endYear = it
                                                endDate = buildDate(endDay, endMonth, it)
                                            }
                                        },
                                        label = { Text("Year") },
                                        modifier = Modifier.weight(1.5f),
                                        singleLine = true
                                    )
                                }
                                Text(
                                    endDate,
                                    fontSize = 12.sp,
                                    color = NavyBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // What's included info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyBlue.copy(0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Report includes:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyBlue
                        )
                        Text(
                            if (state.activeTab == "inbound") "✓ Summary per employee (total hours)" else "✓ Summary per vehicle (total hours)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text("✓ Daily breakdown (day by day)", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            "✓ Detailed sessions (IN/OUT times)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            if (state.activeTab == "inbound") "✓ All phases (Prep, Cycle, Loading)" else "✓ All phases (Waiting, Offloading)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Export Button
                Button(
                    onClick = {
                        isExporting = true
                        dashboardViewModel.exportDateRangeCSV(context, startDate, endDate) {
                            isExporting = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Exporting...", fontSize = 16.sp)
                    } else {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        val labelPrefix =
                            if (state.activeTab == "inbound") "Employees" else "Vehicles"
                        Text(
                            if (exportMode == "today") "Export Today's $labelPrefix" else "Export $startDate → $endDate",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NavyBlue else Color.White
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon, null,
                tint = if (isSelected) Color.White else NavyBlue,
                modifier = Modifier.size(24.dp)
            )
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else NavyBlue
            )
            Text(
                subtitle,
                fontSize = 10.sp,
                color = if (isSelected) Color.White.copy(0.7f) else Color.Gray
            )
        }
    }
}
