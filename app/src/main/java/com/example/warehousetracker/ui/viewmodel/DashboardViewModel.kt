package com.example.warehousetracker.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousetracker.data.model.Branch
import com.example.warehousetracker.data.model.Employee
import com.example.warehousetracker.data.model.EmployeeTrack
import com.example.warehousetracker.data.repository.BranchRepository
import com.example.warehousetracker.data.repository.EmployeeRepository
import com.example.warehousetracker.data.repository.TrackingRepository
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
    val isLoading: Boolean = false,
    val date: String = ""
)

class DashboardViewModel : ViewModel() {
    private val branchRepo = BranchRepository()
    private val empRepo = EmployeeRepository()
    private val trackRepo = TrackingRepository()

    private val _state = MutableStateFlow(DashboardState(date = trackRepo.today()))
    val state = _state.asStateFlow()

    init {
        loadBranches()
    }

    fun loadBranches() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val branches = branchRepo.getBranches()
            val selected = branches.firstOrNull()
            _state.value =
                _state.value.copy(branches = branches, selectedBranch = selected, isLoading = false)
            selected?.let { loadEmployees(it.id) }
        }
    }

    fun selectBranch(branch: Branch) {
        _state.value =
            _state.value.copy(selectedBranch = branch, employees = emptyList(), tracks = emptyMap())
        loadEmployees(branch.id)
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

    fun togglePhase(empId: String, phase: String) {
        viewModelScope.launch {
            trackRepo.togglePhase(empId, _state.value.date, phase)
            val updated = trackRepo.getTrack(empId, _state.value.date)
            val newTracks = _state.value.tracks.toMutableMap()
            newTracks[empId] = updated
            _state.value = _state.value.copy(tracks = newTracks)
        }
    }

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
        empId: String,
        phase: String,
        secondsToAdd: Int,
        start: String?,
        end: String?
    ) {
        viewModelScope.launch {
            trackRepo.applyManualTime(empId, _state.value.date, phase, secondsToAdd, start, end)
            val updated = trackRepo.getTrack(empId, _state.value.date)
            val newTracks = _state.value.tracks.toMutableMap()
            newTracks[empId] = updated
            _state.value = _state.value.copy(tracks = newTracks)
        }
    }

    fun addBranch(name: String) {
        viewModelScope.launch {
            branchRepo.addBranch(name)
            loadBranches()
        }
    }

    fun addEmployee(name: String, code: String, branchId: String) {
        viewModelScope.launch {
            empRepo.addEmployee(name, code, branchId)
            _state.value.selectedBranch?.let { loadEmployees(it.id) }
        }
    }

    fun exportCSV(context: Context) {
        viewModelScope.launch {
            val employees = _state.value.employees
            val tracks = _state.value.tracks
            val branches = _state.value.branches
            val sb = StringBuilder()

            sb.append("--- SUMMARY REPORT ---\n")
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
                )
                    .forEach { (phaseName, data) ->
                        data.history.forEach { session ->
                            sb.append("$branchName,${emp.name},${emp.code},$phaseName,")
                            sb.append(
                                "${session.startTime},${session.endTime},${
                                    formatDuration(
                                        session.durationSeconds
                                    )
                                }\n"
                            )
                        }
                        if (data.isActive) {
                            sb.append("$branchName,${emp.name},${emp.code},$phaseName,")
                            sb.append("${data.currentStartTime},STILL IN,In Progress\n")
                        }
                    }
            }

            try {
                val file =
                    File(context.cacheDir, "Warehouse_Report_${System.currentTimeMillis()}.csv")
                file.writeText(sb.toString())
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
    }
}