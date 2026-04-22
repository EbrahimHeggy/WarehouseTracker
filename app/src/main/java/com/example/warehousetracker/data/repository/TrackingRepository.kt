package com.example.warehousetracker.data.repository

import com.example.warehousetracker.data.model.Employee
import com.example.warehousetracker.data.model.EmployeeTrack
import com.example.warehousetracker.data.model.PhaseData
import com.example.warehousetracker.data.model.WorkSession
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TrackingRepository {
    private val db = Firebase.firestore
    private val timeFormatter = SimpleDateFormat("hh:mm:ss a", Locale.ENGLISH)

    fun nowTime(): String = timeFormatter.format(Date())
    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun trackRef(date: String, empId: String) =
        db.collection("tracking").document(date)
            .collection("employees").document(empId)

    suspend fun getTrack(empId: String, date: String): EmployeeTrack {
        return try {
            val doc = trackRef(date, empId).get().await()
            if (!doc.exists()) return EmployeeTrack(employeeId = empId, date = date)

            fun parsePhase(map: Map<String, Any>?): PhaseData {
                if (map == null) return PhaseData()
                val history = (map["history"] as? List<Map<String, Any>>)?.map { s ->
                    WorkSession(
                        startTime = s["startTime"] as? String ?: "",
                        endTime = s["endTime"] as? String ?: "",
                        durationSeconds = (s["durationSeconds"] as? Long)?.toInt() ?: 0
                    )
                } ?: emptyList()
                return PhaseData(
                    history = history,
                    currentStartTime = map["currentStartTime"] as? String ?: "",
                    isActive = map["isActive"] as? Boolean ?: false
                )
            }

            EmployeeTrack(
                employeeId = empId,
                date = date,
                preparation = parsePhase(doc.get("preparation") as? Map<String, Any>),
                cycleCount = parsePhase(doc.get("cycleCount") as? Map<String, Any>),
                loading = parsePhase(doc.get("loading") as? Map<String, Any>)
            )
        } catch (e: Exception) {
            EmployeeTrack(employeeId = empId, date = date)
        }
    }

    suspend fun togglePhase(empId: String, date: String, phase: String): Result<Unit> {
        return try {
            val ref = trackRef(date, empId)
            val doc = ref.get().await()
            val now = nowTime()
            val phaseMap = (doc.get(phase) as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
            val isActive = phaseMap["isActive"] as? Boolean ?: false

            if (!isActive) {
                phaseMap["isActive"] = true
                phaseMap["currentStartTime"] = now
            } else {
                val startTime = phaseMap["currentStartTime"] as? String ?: ""
                val diff = try {
                    ((timeFormatter.parse(now)!!.time - timeFormatter.parse(startTime)!!.time) / 1000).toInt()
                } catch (e: Exception) {
                    0
                }

                val session =
                    mapOf("startTime" to startTime, "endTime" to now, "durationSeconds" to diff)
                val history = (phaseMap["history"] as? List<Map<String, Any>>)?.toMutableList()
                    ?: mutableListOf()
                history.add(session)

                phaseMap["isActive"] = false
                phaseMap["currentStartTime"] = ""
                phaseMap["history"] = history
            }

            ref.set(mapOf(phase to phaseMap), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPhase(empId: String, date: String, phase: String): Result<Unit> {
        return try {
            val ref = trackRef(date, empId)
            ref.set(
                mapOf(
                    phase to mapOf(
                        "history" to emptyList<Any>(),
                        "isActive" to false,
                        "currentStartTime" to ""
                    )
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyManualTime(
        empId: String, date: String, phase: String,
        secondsToAdd: Int, manualStart: String?, manualEnd: String?
    ): Result<Unit> {
        return try {
            val ref = trackRef(date, empId)
            val doc = ref.get().await()
            val phaseMap = (doc.get(phase) as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
            val history =
                (phaseMap["history"] as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()

            if (manualStart != null && manualEnd != null) {
                val diff = try {
                    ((timeFormatter.parse(manualEnd)!!.time - timeFormatter.parse(manualStart)!!.time) / 1000).toInt()
                } catch (e: Exception) {
                    0
                }
                history.add(
                    mapOf(
                        "startTime" to manualStart,
                        "endTime" to manualEnd,
                        "durationSeconds" to diff
                    )
                )
            } else if (secondsToAdd > 0) {
                history.add(
                    mapOf(
                        "startTime" to "Manual",
                        "endTime" to "Manual",
                        "durationSeconds" to secondsToAdd
                    )
                )
            }

            phaseMap["history"] = history
            ref.set(mapOf(phase to phaseMap), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTracksForBranch(
        employees: List<Employee>,
        date: String
    ): Map<String, EmployeeTrack> = coroutineScope {
        val deferredTracks = employees.map { emp ->
            async {
                emp.id to getTrack(emp.id, date)
            }
        }
        deferredTracks.awaitAll().toMap()
    }


    suspend fun getTracksForDateRange(
        employees: List<Employee>,
        startDate: String,
        endDate: String
    ): Map<String, List<Pair<String, EmployeeTrack>>> = coroutineScope {
        // بيرجع Map<employeeId, List<Pair<date, track>>>
        val dates = getDatesBetween(startDate, endDate)

        val deferredList = employees.map { emp ->
            async {
                val empTracks = dates.map { date ->
                    async { date to getTrack(emp.id, date) }
                }.awaitAll().filter {
                    it.second.totalWHSeconds > 0 ||
                            it.second.preparation.history.isNotEmpty() ||
                            it.second.cycleCount.history.isNotEmpty() ||
                            it.second.loading.history.isNotEmpty()
                }
                emp.id to empTracks
            }
        }
        deferredList.awaitAll().toMap()
    }

    private fun getDatesBetween(startDate: String, endDate: String): List<String> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val start = fmt.parse(startDate) ?: return emptyList()
        val end = fmt.parse(endDate) ?: return emptyList()

        val dates = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.time = start

        while (!cal.time.after(end)) {
            dates.add(fmt.format(cal.time))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return dates
    }
}