package com.example.warehousetracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.warehousetracker.AmberColor
import com.example.warehousetracker.GreenColor
import com.example.warehousetracker.RedColor
import com.example.warehousetracker.data.model.Branch
import com.example.warehousetracker.data.model.Employee
import com.example.warehousetracker.data.repository.BranchRepository
import com.example.warehousetracker.data.repository.EmployeeRepository
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory

data class ImportedEmployee(
    val name: String,
    val code: String,
    val branchName: String,
    var resolvedBranchId: String = "",
    var isSelected: Boolean = true,
    var status: String = "pending"  // pending / success / error / no_branch / exists
)

@Composable
fun ImportEmployeesScreen(
    onBack: () -> Unit,
    onImportDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val empRepo = remember { EmployeeRepository() }
    val branchRepo = remember { BranchRepository() }

    var importedList by remember { mutableStateOf<List<ImportedEmployee>>(emptyList()) }
    var availableBranches by remember { mutableStateOf<List<Branch>>(emptyList()) }
    var allExistingEmployees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var isImporting by remember { mutableStateOf(false) }
    var importDone by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var fileSelected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        availableBranches = branchRepo.getBranches()
        allExistingEmployees = empRepo.getAllEmployees()
    }

    // File Picker
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            errorMsg = ""
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName =
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex =
                            cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        cursor.getString(nameIndex)
                    } ?: ""

                val mimeType = context.contentResolver.getType(uri) ?: ""
                val lowerName = fileName.lowercase()

                val isExcel = lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") ||
                        mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
                        mimeType == "application/vnd.ms-excel"

                val isCsv =
                    lowerName.endsWith(".csv") || mimeType == "text/csv" || mimeType == "text/comma-separated-values"

                val employees = mutableListOf<ImportedEmployee>()

                when {
                    isExcel -> {
                        val workbook = WorkbookFactory.create(inputStream)
                        val formatter = DataFormatter()
                        val sheet = workbook.getSheetAt(0)
                        sheet.rowIterator().forEach { row ->
                            if (row.rowNum == 0) return@forEach
                            val name = formatter.formatCellValue(row.getCell(0)).trim()
                            val code = formatter.formatCellValue(row.getCell(1)).trim()
                            val branchName = formatter.formatCellValue(row.getCell(2)).trim()

                            if (name.isNotBlank()) {
                                val matchedBranch = availableBranches.find {
                                    it.name.equals(branchName, ignoreCase = true)
                                }

                                val alreadyExists =
                                    matchedBranch != null && allExistingEmployees.any {
                                        it.branchId == matchedBranch.id && (it.code == code || it.name.equals(
                                            name,
                                            ignoreCase = true
                                        ))
                                    }

                                employees.add(
                                    ImportedEmployee(
                                        name = name,
                                        code = code,
                                        branchName = branchName,
                                        resolvedBranchId = matchedBranch?.id ?: "",
                                        isSelected = matchedBranch != null && !alreadyExists,
                                        status = when {
                                            matchedBranch == null -> "no_branch"
                                            alreadyExists -> "exists"
                                            else -> "pending"
                                        }
                                    )
                                )
                            }
                        }
                        workbook.close()
                    }

                    isCsv -> {
                        inputStream?.bufferedReader()?.useLines { lines ->
                            lines.forEachIndexed { index, line ->
                                if (index == 0) return@forEachIndexed
                                val parts = line.split(",")
                                val name = parts.getOrNull(0)?.trim() ?: ""
                                val code = parts.getOrNull(1)?.trim() ?: ""
                                val branchName = parts.getOrNull(2)?.trim() ?: ""
                                if (name.isNotBlank()) {
                                    val matchedBranch = availableBranches.find {
                                        it.name.equals(branchName, ignoreCase = true)
                                    }

                                    val alreadyExists =
                                        matchedBranch != null && allExistingEmployees.any {
                                            it.branchId == matchedBranch.id && (it.code == code || it.name.equals(
                                                name,
                                                ignoreCase = true
                                            ))
                                        }

                                    employees.add(
                                        ImportedEmployee(
                                            name = name,
                                            code = code,
                                            branchName = branchName,
                                            resolvedBranchId = matchedBranch?.id ?: "",
                                            isSelected = matchedBranch != null && !alreadyExists,
                                            status = when {
                                                matchedBranch == null -> "no_branch"
                                                alreadyExists -> "exists"
                                                else -> "pending"
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        errorMsg = "Please select .xlsx or .csv file (Detected: $fileName)"
                        return@let
                    }
                }

                importedList = employees
                fileSelected = employees.isNotEmpty()
                if (employees.isEmpty()) errorMsg = "No employees found in file"

            } catch (e: Exception) {
                errorMsg = "Error reading file: ${e.message}"
            }
        }
    }

    val matchedCount =
        importedList.count { it.resolvedBranchId.isNotBlank() && it.status != "exists" }
    val existsCount = importedList.count { it.status == "exists" }
    val unmatchedCount = importedList.count { it.status == "no_branch" }
    val selectedCount = importedList.count { it.isSelected && it.resolvedBranchId.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Header
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Import Employees",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "All Branches",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)) {

                Spacer(Modifier.height(8.dp))

                // Instructions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(
                            0.05f
                        )
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "File Format",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "• Column A: Name  |  Column B: Code  |  Column C: Branch",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "• Branch name must match exactly (e.g. Luxor, Cairo)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "• First row = header (skipped automatically)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (availableBranches.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Available branches: ${availableBranches.joinToString(", ") { it.name }}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Pick File
                Button(
                    onClick = {
                        fileLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "text/csv",
                                "text/comma-separated-values"
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.UploadFile,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (fileSelected) "Change File" else "Select Excel / CSV",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Stats Row
                if (importedList.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(
                            Modifier.weight(1f),
                            "${importedList.size}",
                            "Total",
                            MaterialTheme.colorScheme.primary
                        )
                        StatChip(Modifier.weight(1f), "$matchedCount", "New", GreenColor)
                        if (existsCount > 0)
                            StatChip(
                                Modifier.weight(1f),
                                "$existsCount",
                                "Exists",
                                MaterialTheme.colorScheme.primary
                            )
                        if (unmatchedCount > 0)
                            StatChip(Modifier.weight(1f), "$unmatchedCount", "No Branch", RedColor)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Select All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$selectedCount ready to import",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = {
                            val allSelected =
                                importedList.filter { it.resolvedBranchId.isNotBlank() && it.status != "exists" }
                                    .all { it.isSelected }
                            importedList = importedList.map {
                                if (it.resolvedBranchId.isNotBlank() && it.status != "exists") it.copy(
                                    isSelected = !allSelected
                                ) else it
                            }
                        }) {
                            Text(
                                if (importedList.filter { it.resolvedBranchId.isNotBlank() && it.status != "exists" }
                                        .all { it.isSelected }) "Deselect All" else "Select All",
                                color = MaterialTheme.colorScheme.primary, fontSize = 12.sp
                            )
                        }
                    }

                    // List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        val grouped = importedList.groupBy { it.branchName.ifBlank { "No Branch" } }
                        grouped.forEach { (branchName, emps) ->
                            item {
                                Text(
                                    branchName.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(emps) { emp ->
                                val index = importedList.indexOf(emp)
                                ImportRowWithBranch(
                                    emp = emp,
                                    onToggle = {
                                        if (emp.resolvedBranchId.isNotBlank() && emp.status != "exists") {
                                            importedList = importedList.toMutableList().also {
                                                it[index] =
                                                    it[index].copy(isSelected = !it[index].isSelected)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Import Button
                    if (!importDone) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isImporting = true
                                    val updatedList = importedList.toMutableList()
                                    importedList.forEachIndexed { index, emp ->
                                        if (!emp.isSelected || emp.resolvedBranchId.isBlank() || emp.status == "exists") return@forEachIndexed
                                        try {
                                            val result = empRepo.addEmployee(
                                                emp.name,
                                                emp.code,
                                                emp.resolvedBranchId
                                            )
                                            updatedList[index] = updatedList[index].copy(
                                                status = if (result.isSuccess) "success" else "error"
                                            )
                                        } catch (e: Exception) {
                                            updatedList[index] =
                                                updatedList[index].copy(status = "error")
                                        }
                                        importedList = updatedList.toList()
                                    }
                                    isImporting = false
                                    importDone = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenColor),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isImporting && selectedCount > 0
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Importing...", fontSize = 15.sp, color = Color.White)
                            } else {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Import $selectedCount Employees",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        val successCount = importedList.count { it.status == "success" }
                        val errorCount = importedList.count { it.status == "error" }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = GreenColor.copy(0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = GreenColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    "Import Complete!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GreenColor
                                )
                                Text(
                                    "$successCount imported${if (errorCount > 0) " • $errorCount failed" else ""}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onImportDone,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Back to Dashboard",
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                } else if (!fileSelected) {
                    Spacer(Modifier.weight(1f))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.UploadFile,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "No file selected",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Text(
                            "Select an Excel or CSV file",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ImportRowWithBranch(emp: ImportedEmployee, onToggle: () -> Unit) {
    val hasBranch = emp.resolvedBranchId.isNotBlank()
    val alreadyExists = emp.status == "exists"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (emp.status) {
                "success" -> GreenColor.copy(0.08f)
                "error" -> MaterialTheme.colorScheme.error.copy(0.08f)
                "no_branch" -> MaterialTheme.colorScheme.error.copy(0.05f)
                "exists" -> MaterialTheme.colorScheme.primary.copy(0.05f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (emp.status) {
                "success" -> Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = GreenColor,
                    modifier = Modifier.size(22.dp)
                )

                "error" -> Icon(
                    Icons.Default.Error,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )

                "no_branch" -> Icon(
                    Icons.Default.Warning,
                    null,
                    tint = AmberColor,
                    modifier = Modifier.size(22.dp)
                )

                "exists" -> Icon(
                    Icons.Default.Info,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )

                else -> Checkbox(
                    checked = emp.isSelected && hasBranch && !alreadyExists,
                    onCheckedChange = { onToggle() },
                    enabled = hasBranch && !alreadyExists,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasBranch && !alreadyExists) MaterialTheme.colorScheme.primary.copy(0.1f)
                        else if (alreadyExists) MaterialTheme.colorScheme.primary.copy(0.05f)
                        else MaterialTheme.colorScheme.error.copy(0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    emp.name.split(" ").take(2).map { it.firstOrNull()?.uppercase() ?: "" }
                        .joinToString(""),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (hasBranch && !alreadyExists) MaterialTheme.colorScheme.primary
                    else if (alreadyExists) MaterialTheme.colorScheme.primary.copy(0.4f)
                    else MaterialTheme.colorScheme.error
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    emp.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (alreadyExists) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Code: ${emp.code.ifBlank { "—" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                when (emp.status) {
                    "success" -> Text(
                        "Added ✓",
                        fontSize = 11.sp,
                        color = GreenColor,
                        fontWeight = FontWeight.Medium
                    )

                    "error" -> Text(
                        "Failed ✗",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )

                    "no_branch" -> Text(
                        "Branch not found",
                        fontSize = 10.sp,
                        color = AmberColor,
                        fontWeight = FontWeight.Medium
                    )

                    "exists" -> Text(
                        "Already in system",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary.copy(0.6f),
                        fontWeight = FontWeight.Medium
                    )

                    else -> if (hasBranch && emp.isSelected) Text(
                        "Ready",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatChip(modifier: Modifier, value: String, label: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.1f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = color.copy(0.7f))
        }
    }
}
