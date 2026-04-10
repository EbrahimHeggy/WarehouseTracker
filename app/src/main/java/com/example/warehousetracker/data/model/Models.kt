package com.example.warehousetracker.data.model

data class Branch(
    val id: String = "",
    val name: String = ""
)

data class Employee(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val branchId: String = ""
)

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: String = "user",  // "admin" or "user"
    val branchId: String = "",
    val name: String = ""
)

data class WorkSession(
    val startTime: String = "",
    val endTime: String = "",
    val durationSeconds: Int = 0
)

data class PhaseData(
    val history: List<WorkSession> = emptyList(),
    val currentStartTime: String = "",
    val isActive: Boolean = false
) {
    val accumulatedSeconds: Int get() = history.sumOf { it.durationSeconds }
}

data class EmployeeTrack(
    val employeeId: String = "",
    val date: String = "",
    val preparation: PhaseData = PhaseData(),
    val cycleCount: PhaseData = PhaseData(),
    val loading: PhaseData = PhaseData()
) {
    val totalWHSeconds: Int
        get() =
            preparation.accumulatedSeconds +
                    cycleCount.accumulatedSeconds +
                    loading.accumulatedSeconds
}