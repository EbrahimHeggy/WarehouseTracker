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
import com.example.warehousetracker.data.model.Vehicle
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
    val selectedBranch: Branch? = null,
    val employees: List<Employee> = emptyList(),
    val tracks: Map<String, EmployeeTrack> = emptyMap(),

    // Outbound
    val vehicles: List<Vehicle> = emptyList(),
    val vehicleTracks: Map<String, VehicleTrack> = emptyMap(),
    
    val isLoading: Boolean = false,
    val date: String = "",
    val activeTab: String = "inbound" // "inbound" or "outbound"
)

class DashboardViewModel : ViewModel() {
    private val branchRepo = BranchRepository()
    private val empRepo = EmployeeRepository()
    private val trackRepo = TrackingRepository()
    private val vehicleRepo = VehicleRepository()

    private val _state = MutableStateFlow(DashboardState(date = trackRepo.today()))
    val state = _state.asStateFlow()

    init {
        loadBranches()
    }

    fun setTab(tab: String) {
        _state.value = _state.value.copy(activeTab = tab)
    }

    fun loadBranches() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val branches = branchRepo.getBranches()
            val selected = branches.firstOrNull()
            _state.value =
                _state.value.copy(branches = branches, selectedBranch = selected, isLoading = false)
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
                vehicles = emptyList(),
                vehicleTracks = emptyMap()
            )
        loadEmployees(branch.id)
        loadVehicles(branch.id)
    }

    fun loadEmployees(branchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val employees = empRepo.getEmployeesByBranch(branchId)
            val tracks = trackRepo.getTracksForBranch(employees, _state.value.date)
            _state.value =
                _state.value.copy(employees = employees, tracks = tracks, isLoading = false)
        }
    }

    fun loadVehicles(branchId: String) {
        viewModelScope.launch {
            val vehicles = vehicleRepo.getVehiclesByBranch(branchId)
            val vTracks = trackRepo.getVehicleTracksForBranch(vehicles, _state.value.date)
            _state.value = _state.value.copy(vehicles = vehicles, vehicleTracks = vTracks)
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

    fun toggleVehiclePhase(vehicleId: String, phase: String) {
        viewModelScope.launch {
            trackRepo.toggleVehiclePhase(vehicleId, _state.value.date, phase)
            val updated = trackRepo.getVehicleTrack(vehicleId, _state.value.date)
            val newTracks = _state.value.vehicleTracks.toMutableMap()
            newTracks[vehicleId] = updated
            _state.value = _state.value.copy(vehicleTracks = newTracks)
        }
    }

    fun resetVehiclePhase(vehicleId: String, phase: String) {
        viewModelScope.launch {
            trackRepo.resetVehiclePhase(vehicleId, _state.value.date, phase)
            val updated = trackRepo.getVehicleTrack(vehicleId, _state.value.date)
            val newTracks = _state.value.vehicleTracks.toMutableMap()
            newTracks[vehicleId] = updated
            _state.value = _state.value.copy(vehicleTracks = newTracks)
        }
    }

    fun addVehicle(type: String, plateNumber: String, context: Context) {
        viewModelScope.launch {
            val branchId = _state.value.selectedBranch?.id ?: return@launch
            val result = vehicleRepo.addVehicle(type, plateNumber, branchId)
            if (result.isSuccess) {
                loadVehicles(branchId)
            } else {
                Toast.makeText(context, "Error adding vehicle", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteVehicle(vehicleId: String) {
        viewModelScope.launch {
            vehicleRepo.deleteVehicle(vehicleId)
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
                val updated = trackRepo.getVehicleTrack(id, _state.value.date)
                val newTracks = _state.value.vehicleTracks.toMutableMap()
                newTracks[id] = updated
                _state.value = _state.value.copy(vehicleTracks = newTracks)
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
                loadBranches()
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
            if (_state.value.activeTab == "inbound") exportInboundCSV(context)
            else exportOutboundCSV(context)
        }
    }

    private fun exportInboundCSV(context: Context) {
        val employees = _state.value.employees
        val tracks = _state.value.tracks
        val branches = _state.value.branches
        val sb = StringBuilder()

        sb.append("--- INBOUND SUMMARY REPORT ---\n")
        sb.append("Branch,Name,Code,Prep Total,Cycle Total,Loading Total,Total WH Time\n")
        employees.forEach { emp ->
            val branchName = branches.find { it.id == emp.branchId }?.name ?: ""
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
            val branchName = branches.find { it.id == emp.branchId }?.name ?: ""
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
        shareFile(context, sb.toString(), "Inbound_Report")
    }

    private fun exportOutboundCSV(context: Context) {
        val vehicles = _state.value.vehicles
        val tracks = _state.value.vehicleTracks
        val branchName = _state.value.selectedBranch?.name ?: ""
        val sb = StringBuilder()

        sb.append("--- OUTBOUND SUMMARY REPORT ---\n")
        sb.append("Branch,Type,Plate Number,Waiting Total,Offload Total,Total Time\n")
        vehicles.forEach { v ->
            val t = tracks[v.id] ?: return@forEach
            sb.append("$branchName,${v.type},${v.plateNumber},")
            sb.append("${formatDuration(t.waiting.accumulatedSeconds)},")
            sb.append("${formatDuration(t.offloading.accumulatedSeconds)},")
            sb.append("${formatDuration(t.totalSeconds)}\n")
        }

        sb.append("\n--- DETAILED LOG ---\n")
        sb.append("Branch,Type,Plate Number,Phase,In Time,Out Time,Duration\n")
        vehicles.forEach { v ->
            val t = tracks[v.id] ?: return@forEach
            listOf(
                "Waiting" to t.waiting,
                "Offloading" to t.offloading
            ).forEach { (phaseName, data) ->
                data.history.forEach { session ->
                    sb.append(
                        "$branchName,${v.type},${v.plateNumber},$phaseName,'${session.startTime},'${session.endTime},${
                            formatDuration(
                                session.durationSeconds
                            )
                        }\n"
                    )
                }
                if (data.isActive) sb.append("$branchName,${v.type},${v.plateNumber},$phaseName,'${data.currentStartTime},STILL IN,In Progress\n")
            }
        }
        shareFile(context, sb.toString(), "Outbound_Report")
    }

    private fun shareFile(context: Context, content: String, prefix: String) {
        try {
            val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.csv")
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

    fun exportDateRangeCSV(context: Context, startDate: String, endDate: String) {
        viewModelScope.launch {
            if (_state.value.activeTab == "inbound") exportInboundRangeCSV(
                context,
                startDate,
                endDate
            )
            else exportOutboundRangeCSV(context, startDate, endDate)
        }
    }

    private suspend fun exportInboundRangeCSV(
        context: Context,
        startDate: String,
        endDate: String
    ) {
        val employees = _state.value.employees
        val branches = _state.value.branches
        val allTracks = trackRepo.getTracksForDateRange(employees, startDate, endDate)
        val sb = StringBuilder()

        sb.append("=== INBOUND SUMMARY REPORT ($startDate to $endDate) ===\n")
        sb.append("Branch,Name,Code,Total Prep,Total Cycle,Total Loading,Total WH Time\n")
        employees.forEach { emp ->
            val branchName = branches.find { it.id == emp.branchId }?.name ?: ""
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
            val branchName = branches.find { it.id == emp.branchId }?.name ?: ""
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
            val branchName = branches.find { it.id == emp.branchId }?.name ?: ""
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

        shareFile(context, sb.toString(), "Inbound_Range")
    }

    private suspend fun exportOutboundRangeCSV(
        context: Context,
        startDate: String,
        endDate: String
    ) {
        val vehicles = _state.value.vehicles
        val branchName = _state.value.selectedBranch?.name ?: ""
        val allVTracks = trackRepo.getVehicleTracksForDateRange(vehicles, startDate, endDate)
        val sb = StringBuilder()

        sb.append("=== OUTBOUND SUMMARY REPORT ($startDate to $endDate) ===\n")
        sb.append("Branch,Type,Plate Number,Total Waiting,Total Offload,Total Time\n")

        vehicles.forEach { v ->
            val vTracks = allVTracks[v.id] ?: emptyList()
            val totalW = vTracks.sumOf { it.second.waiting.accumulatedSeconds }
            val totalO = vTracks.sumOf { it.second.offloading.accumulatedSeconds }
            val total = totalW + totalO
            if (total > 0) sb.append(
                "$branchName,${v.type},${v.plateNumber},${formatDuration(totalW)},${
                    formatDuration(
                        totalO
                    )
                },${formatDuration(total)}\n"
            )
        }

        sb.append("\n\n=== DAILY BREAKDOWN ===\nDate,Branch,Type,Plate Number,Waiting,Offload,Total\n")
        vehicles.forEach { v ->
            allVTracks[v.id]?.forEach { (date, track) ->
                if (track.totalSeconds > 0) sb.append(
                    "$date,$branchName,${v.type},${v.plateNumber},${
                        formatDuration(
                            track.waiting.accumulatedSeconds
                        )
                    },${formatDuration(track.offloading.accumulatedSeconds)},${formatDuration(track.totalSeconds)}\n"
                )
            }
        }

        sb.append("\n\n=== DETAILED SESSION LOG ===\nDate,Branch,Type,Plate Number,Phase,In Time,Out Time,Duration\n")
        vehicles.forEach { v ->
            allVTracks[v.id]?.forEach { (date, track) ->
                listOf(
                    "Waiting" to track.waiting,
                    "Offloading" to track.offloading
                ).forEach { (phaseName, data) ->
                    data.history.forEach { session ->
                        sb.append(
                            "$date,$branchName,${v.type},${v.plateNumber},$phaseName,'${session.startTime},'${session.endTime},${
                                formatDuration(
                                    session.durationSeconds
                                )
                            }\n"
                        )
                    }
                    if (data.isActive) sb.append("$date,$branchName,${v.type},${v.plateNumber},$phaseName,'${data.currentStartTime},STILL IN,In Progress\n")
                }
            }
        }

        shareFile(context, sb.toString(), "Outbound_Range")
    }
}
