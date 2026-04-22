package com.example.warehousetracker.data.repository

import com.example.warehousetracker.data.model.Vehicle
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class VehicleRepository {
    private val db = Firebase.firestore

    suspend fun getVehiclesByBranch(branchId: String): List<Vehicle> {
        return try {
            db.collection("vehicles")
                .whereEqualTo("branchId", branchId)
                .get().await()
                .documents.map { doc ->
                    Vehicle(
                        id = doc.id,
                        type = doc.getString("type") ?: "",
                        plateNumber = doc.getString("plateNumber") ?: "",
                        branchId = doc.getString("branchId") ?: ""
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addVehicle(type: String, plateNumber: String, branchId: String): Result<Vehicle> {
        return try {
            val data = mapOf(
                "type" to type,
                "plateNumber" to plateNumber,
                "branchId" to branchId
            )
            val ref = db.collection("vehicles").add(data).await()
            Result.success(
                Vehicle(
                    id = ref.id,
                    type = type,
                    plateNumber = plateNumber,
                    branchId = branchId
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVehicle(vehicleId: String): Result<Unit> {
        return try {
            db.collection("vehicles").document(vehicleId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
