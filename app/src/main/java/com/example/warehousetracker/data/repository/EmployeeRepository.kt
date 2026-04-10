package com.example.warehousetracker.data.repository

import com.example.warehousetracker.data.model.Employee
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class EmployeeRepository {
    private val db = Firebase.firestore

    suspend fun getEmployeesByBranch(branchId: String): List<Employee> {
        return try {
            db.collection("employees")
                .whereEqualTo("branchId", branchId)
                .get().await()
                .documents.map { doc ->
                    Employee(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        code = doc.getString("code") ?: "",
                        branchId = doc.getString("branchId") ?: ""
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllEmployees(): List<Employee> {
        return try {
            db.collection("employees").get().await().documents.map { doc ->
                Employee(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    code = doc.getString("code") ?: "",
                    branchId = doc.getString("branchId") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addEmployee(name: String, code: String, branchId: String): Result<Employee> {
        return try {
            val data = mapOf("name" to name, "code" to code, "branchId" to branchId)
            val ref = db.collection("employees").add(data).await()
            Result.success(Employee(id = ref.id, name = name, code = code, branchId = branchId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEmployee(empId: String): Result<Unit> {
        return try {
            db.collection("employees").document(empId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}