package com.example.warehousetracker.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousetracker.data.model.Branch
import com.example.warehousetracker.data.model.Employee
import com.example.warehousetracker.data.model.EmployeeTrack
import com.example.warehousetracker.data.model.VehicleTrack
import com.example.warehousetracker.data.repository.BranchRepository
import com.example.warehousetracker.data.repository.EmployeeRepository
import com.example.warehousetracker.data.repository.TrackingRepository
import com.example.warehousetracker.data.repository.VehicleRepository
import com.example.warehousetracker.formatDuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class DashboardState(
    val branches: List<Branch> = emptyList(),
    val branchEmployeeCounts: Map<String, Int> = emptyMap(),
    val selectedBranch: Branch? = null,
    val employees: List<Employee> = emptyList(),
    val tracks: Map<String, EmployeeTrack> = emptyMap(),

    // Inbound - Now based on visits/tracks (Swapped from Outbound)
    val vehicleTracks: List<VehicleTrack> = emptyList(),
    
    val isLoading: Boolean = false,
    val date: String = "",
    val activeTab: String = "outbound" // "outbound" (Employees) or "inbound" (Vehicles)
)

class DashboardViewModel : ViewModel() {
    private val branchRepo = BranchRepository()
    private val empRepo = EmployeeRepository()
    private val trackRepo = TrackingRepository()
    private val vehicleRepo = VehicleRepository()

    private val _state = MutableStateFlow(DashboardState(date = trackRepo.today()))
    val state = _state.asStateFlow()

    fun setTab(tab: String) {
        _state.value = _state.value.copy(activeTab = tab)
    }

    fun loadBranches(defaultBranchId: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val branches = branchRepo.getBranches()

            // Try to find the default branch from profile, otherwise take the first one
            val selected = if (!defaultBranchId.isNullOrBlank()) {
                branches.find { it.id == defaultBranchId } ?: branches.firstOrNull()
            } else {
                branches.firstOrNull()
            }

            // Load employee counts for all branches
            val counts = branches.associate { it.id to empRepo.getEmployeeCountByBranch(it.id) }
            
            _state.value =
                _state.value.copy(
                    branches = branches,
                    branchEmployeeCounts = counts,
                    selectedBranch = selected,
                    isLoading = false
                )
            selected?.let {
                loadEmployees(it.id)
                loadVehicles(it.id)
            }
        }
    }

    fun selectBranch(branch: Branch) {
        _state.value =
            _state.value.copy(
                selectedBranch = branch,
                employees = emptyList(),
                tracks = emptyMap(),
                vehicleTracks = emptyList()
            )
        loadEmployees(branch.id)
        loadVehicles(branch.id)
    }

    fun loadEmployees(branchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val employees = empRepo.getEmployeesByBranch(branchId)
            val tracks = trackRepo.getTracksForBranch(employees, _state.value.date)

            // Refresh count for current branch
            val newCounts = _state.value.branchEmployeeCounts.toMutableMap()
            newCounts[branchId] = employees.size
            
            _state.value =
                _state.value.copy(
                    employees = employees,
                    tracks = tracks,
                    branchEmployeeCounts = newCounts,
                    isLoading = false
                )
        }
    }

    fun loadVehicles(branchId: String) {
        viewModelScope.launch {
            val vTracks = trackRepo.getVehicleTracksForBranch(branchId, _state.value.date)
            _state.value = _state.value.copy(vehicleTracks = vTracks)
        }
    }

    fun togglePhase(empId: String, phase: String) {
        viewModelScope.launch {
            trackRepo.togglePhase(empId, _state.value.date, phase)
            val updated = trackRepo.getTrack(empId, _state.value.date)
            val newTracks = _state.value.tracks.toMutableMap()
            newTracks[empId] = updated
            _state.value = _state.value.copy(tracks = newTracks)
        }
    }

    // ── Vehicle Actions ──────────────────

    fun toggleVehiclePhase(trackId: String, phase: String) {
        viewModelScope.launch {
            trackRepo.toggleVehiclePhase(trackId, _state.value.date, phase)
            loadVehicles(_state.value.selectedBranch?.id ?: return@launch)
        }
    }

    fun resetVehiclePhase(trackId: String, phase: String) {
        viewModelScope.launch {
            trackRepo.resetVehiclePhase(trackId, _state.value.date, phase)
            loadVehicles(_state.value.selectedBranch?.id ?: return@launch)
        }
    }

    fun addVehicle(type: String, plateNumber: String, context: Context) {
        viewModelScope.launch {
            val branchId = _state.value.selectedBranch?.id ?: return@launch
            val result = trackRepo.addVehicleVisit(branchId, _state.value.date, type, plateNumber)
            if (result.isSuccess) {
                loadVehicles(branchId)
            } else {
                Toast.makeText(context, "Error adding vehicle visit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteVehicle(trackId: String) {
        viewModelScope.launch {
            trackRepo.deleteVehicleTrack(trackId, _state.value.date)
            _state.value.selectedBranch?.let { loadVehicles(it.id) }
        }
    }

    // ── Rest of Dashboard Actions ──────────────────

    fun resetPhase(empId: String, phase: String) {
        viewModelScope.launch {
            trackRepo.resetPhase(empId, _state.value.date, phase)
            val updated = trackRepo.getTrack(empId, _state.value.date)
            val newTracks = _state.value.tracks.toMutableMap()
            newTracks[empId] = updated
            _state.value = _state.value.copy(tracks = newTracks)
        }
    }

    fun applyManualTime(
        id: String,
        phase: String,
        secondsToAdd: Int,
        start: String?,
        end: String?,
        isVehicle: Boolean = false
    ) {
        viewModelScope.launch {
            trackRepo.applyManualTime(
                id,
                _state.value.date,
                phase,
                secondsToAdd,
                start,
                end,
                isVehicle
            )
            if (isVehicle) {
                loadVehicles(_state.value.selectedBranch?.id ?: "")
            } else {
                val updated = trackRepo.getTrack(id, _state.value.date)
                val newTracks = _state.value.tracks.toMutableMap()
                newTracks[id] = updated
                _state.value = _state.value.copy(tracks = newTracks)
            }
        }
    }

    fun addBranch(name: String, context: Context) {
        viewModelScope.launch {
            val result = branchRepo.addBranch(name)
            if (result.isSuccess) {
                loadBranches(_state.value.selectedBranch?.id)
            } else {
                Toast.makeText(
                    context,
                    result.exceptionOrNull()?.message ?: "Error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun deleteBranch(branchId: String) {
        viewModelScope.launch {
            branchRepo.deleteBranch(branchId)
            loadBranches()
        }
    }

    fun addEmployee(name: String, code: String, branchId: String, context: Context) {
        viewModelScope.launch {
            val result = empRepo.addEmployee(name, code, branchId)
            if (result.isSuccess) {
                _state.value.selectedBranch?.let { loadEmployees(it.id) }
            } else {
                Toast.makeText(
                    context,
                    result.exceptionOrNull()?.message ?: "Error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun deleteEmployee(empId: String) {
        viewModelScope.launch {
            empRepo.deleteEmployee(empId)
            _state.value.selectedBranch?.let { loadEmployees(it.id) }
        }
    }

    fun exportCSV(context: Context) {
        viewModelScope.launch {
            if (_state.value.activeTab == "outbound") exportOutboundCSV(context)
            else exportInboundCSV(context)
        }
    }

    private fun exportOutboundCSV(context: Context) { // Swapped logic: Inbound report for Outbound tab (Employees)
        val employees = _state.value.employees
        val tracks = _state.value.tracks
        val branch = _state.value.selectedBranch
        val branchName = branch?.name ?: "Branch"
        val date = _state.value.date
        val sb = StringBuilder()

        sb.append("--- OUTBOUND SUMMARY REPORT ---\n")
        sb.append("Branch,Name,Code,Prep Total,Cycle Total,Loading Total,Total WH Time\n")
        employees.forEach { emp ->
            val t = tracks[emp.id] ?: return@forEach
            sb.append("$branchName,${emp.name},${emp.code},")
            sb.append("${formatDuration(t.preparation.accumulatedSeconds)},")
            sb.append("${formatDuration(t.cycleCount.accumulatedSeconds)},")
            sb.append("${formatDuration(t.loading.accumulatedSeconds)},")
            sb.append("${formatDuration(t.totalWHSeconds)}\n")
        }

        sb.append("\n--- DETAILED LOG ---\n")
        sb.append("Branch,Name,Code,Phase,In Time,Out Time,Duration\n")
        employees.forEach { emp ->
            val t = tracks[emp.id] ?: return@forEach
            listOf(
                "Prep & Check" to t.preparation,
                "Cycle Count" to t.cycleCount,
                "Loading" to t.loading
            ).forEach { (phaseName, data) ->
                data.history.forEach { session ->
                    sb.append(
                        "$branchName,${emp.name},${emp.code},$phaseName,'${session.startTime},'${session.endTime},${
                            formatDuration(
                                session.durationSeconds
                            )
                        }\n"
                    )
                }
                if (data.isActive) sb.append("$branchName,${emp.name},${emp.code},$phaseName,'${data.currentStartTime},STILL IN,In Progress\n")
            }
        }
        shareFile(context, sb.toString(), "$branchName - Outbound - $date")
    }

    private fun exportInboundCSV(context: Context) { // Swapped logic: Outbound report for Inbound tab (Vehicles)
        val vTracks = _state.value.vehicleTracks
        val branch = _state.value.selectedBranch
        val branchName = branch?.name ?: "Branch"
        val date = _state.value.date
        val sb = StringBuilder()

        sb.append("--- INBOUND SUMMARY REPORT ---\n")
        sb.append("Branch,Type,Plate Number,Waiting Total,Offload Total,Total Time\n")
        vTracks.forEach { t ->
            sb.append("$branchName,${t.type},${t.plateNumber},")
            sb.append("${formatDuration(t.waiting.accumulatedSeconds)},")
            sb.append("${formatDuration(t.offloading.accumulatedSeconds)},")
            sb.append("${formatDuration(t.totalSeconds)}\n")
        }

        sb.append("\n--- DETAILED LOG ---\n")
        sb.append("Branch,Type,Plate Number,Phase,In Time,Out Time,Duration\n")
        vTracks.forEach { t ->
            listOf(
                "Waiting" to t.waiting,
                "Offloading" to t.offloading
            ).forEach { (phaseName, data) ->
                data.history.forEach { session ->
                    sb.append(
                        "$branchName,${t.type},${t.plateNumber},$phaseName,'${session.startTime},'${session.endTime},${
                            formatDuration(
                                session.durationSeconds
                            )
                        }\n"
                    )
                }
                if (data.isActive) sb.append("$branchName,${t.type},${t.plateNumber},$phaseName,'${data.currentStartTime},STILL IN,In Progress\n")
            }
        }
        shareFile(context, sb.toString(), "$branchName - Inbound - $date")
    }

    private fun shareFile(context: Context, content: String, fileName: String) {
        try {
            val safeName = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val file = File(context.cacheDir, "$safeName.csv")
            file.writeText(content)
            val uri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Report"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportDateRangeCSV(
        context: Context,
        startDate: String,
        endDate: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                if (_state.value.activeTab == "outbound") exportOutboundRangeCSV(
                    context,
                    startDate,
                    endDate
                )
                else exportInboundRangeCSV(context, startDate, endDate)
            } finally {
                onComplete()
            }
        }
    }

    private suspend fun exportOutboundRangeCSV( // Swapped logic
        context: Context,
        startDate: String,
        endDate: String
    ) {
        val branch = _state.value.selectedBranch ?: return
        val branchId = branch.id
        val branchName = branch.name
        val employees = empRepo.getEmployeesByBranch(branchId)
        val allTracks = trackRepo.getTracksForDateRange(employees, startDate, endDate)
        val sb = StringBuilder()

        sb.append("=== OUTBOUND SUMMARY REPORT ($startDate to $endDate) ===\n")
        sb.append("Branch,Name,Code,Total Prep,Total Cycle,Total Loading,Total WH Time\n")
        employees.forEach { emp ->
            val empTracks = allTracks[emp.id] ?: emptyList()
            val totalPrep = empTracks.sumOf { it.second.preparation.accumulatedSeconds }
            val totalCycle = empTracks.sumOf { it.second.cycleCount.accumulatedSeconds }
            val totalLoading = empTracks.sumOf { it.second.loading.accumulatedSeconds }
            val totalWH = totalPrep + totalCycle + totalLoading
            if (totalWH > 0) sb.append(
                "$branchName,${emp.name},${emp.code},${
                    formatDuration(
                        totalPrep
                    )
                },${formatDuration(totalCycle)},${formatDuration(totalLoading)},${
                    formatDuration(
                        totalWH
                    )
                }\n"
            )
        }

        sb.append("\n\n=== DAILY BREAKDOWN ===\nDate,Branch,Name,Code,Prep,Cycle Count,Loading,Total\n")
        employees.forEach { emp ->
            allTracks[emp.id]?.forEach { (date, track) ->
                if (track.totalWHSeconds > 0) sb.append(
                    "$date,$branchName,${emp.name},${emp.code},${
                        formatDuration(
                            track.preparation.accumulatedSeconds
                        )
                    },${formatDuration(track.cycleCount.accumulatedSeconds)},${formatDuration(track.loading.accumulatedSeconds)},${
                        formatDuration(
                            track.totalWHSeconds
                        )
                    }\n"
                )
            }
        }

        sb.append("\n\n=== DETAILED SESSION LOG ===\nDate,Branch,Name,Code,Phase,In Time,Out Time,Duration\n")
        employees.forEach { emp ->
            allTracks[emp.id]?.forEach { (date, track) ->
                listOf(
                    "Prep & Check" to track.preparation,
                    "Cycle Count" to track.cycleCount,
                    "Loading" to track.loading
                ).forEach { (phaseName, data) ->
                    data.history.forEach { session ->
                        sb.append(
                            "$date,$branchName,${emp.name},${emp.code},$phaseName,'${session.startTime},'${session.endTime},${
                                formatDuration(
                                    session.durationSeconds
                                )
                            }\n"
                        )
                    }
                    if (data.isActive) sb.append("$date,$branchName,${emp.name},${emp.code},$phaseName,'${data.currentStartTime},STILL IN,In Progress\n")
                }
            }
        }

        val period = if (startDate == endDate) startDate else "${startDate}_to_${endDate}"
        shareFile(context, sb.toString(), "$branchName - Outbound - $period")
    }

    private suspend fun exportInboundRangeCSV( // Swapped logic
        context: Context,
        startDate: String,
        endDate: String
    ) {
        val branch = _state.value.selectedBranch ?: return
        val branchId = branch.id
        val branchName = branch.name
        val allVTracks = trackRepo.getVehicleTracksForDateRange(branchId, startDate, endDate)
        val sb = StringBuilder()

        sb.append("=== INBOUND SUMMARY REPORT ($startDate to $endDate) ===\n")
        sb.append("Date,Branch,Type,Plate Number,Waiting,Offload,Total Time\n")

        allVTracks.forEach { t ->
            sb.append("${t.date},$branchName,${t.type},${t.plateNumber},")
            sb.append("${formatDuration(t.waiting.accumulatedSeconds)},")
            sb.append("${formatDuration(t.offloading.accumulatedSeconds)},")
            sb.append("${formatDuration(t.totalSeconds)}\n")
        }

        sb.append("\n\n=== DETAILED SESSION LOG ===\nDate,Branch,Type,Plate Number,Phase,In Time,Out Time,Duration\n")
        allVTracks.forEach { t ->
            listOf(
                "Waiting" to t.waiting,
                "Offloading" to t.offloading
            ).forEach { (phaseName, data) ->
                data.history.forEach { session ->
                    sb.append(
                        "${t.date},$branchName,${t.type},${t.plateNumber},$phaseName,'${session.startTime},'${session.endTime},${
                            formatDuration(
                                session.durationSeconds
                            )
                        }\n"
                    )
                }
                if (data.isActive) sb.append("${t.date},$branchName,${t.type},${t.plateNumber},$phaseName,'${data.currentStartTime},STILL IN,In Progress\n")
            }
        }

        val period = if (startDate == endDate) startDate else "${startDate}_to_${endDate}"
        shareFile(context, sb.toString(), "$branchName - Inbound - $period")
    }
}
