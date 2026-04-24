package com.example.warehousetracker.data.repository

import com.example.warehousetracker.data.model.Employee
import com.google.firebase.firestore.AggregateSource
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

    suspend fun getEmployeeCountByBranch(branchId: String): Int {
        return try {
            db.collection("employees")
                .whereEqualTo("branchId", branchId)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count.toInt()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun addEmployee(name: String, code: String, branchId: String): Result<Employee> {
        return try {
            // التحقق من وجود موظف بنفس الكود في نفس الفرع
            val existingCode = db.collection("employees")
                .whereEqualTo("branchId", branchId)
                .whereEqualTo("code", code)
                .get().await()

            if (!existingCode.isEmpty) {
                return Result.failure(Exception("Employee with this code already exists in this branch"))
            }

            // التحقق من وجود موظف بنفس الاسم في نفس الفرع
            val existingName = db.collection("employees")
                .whereEqualTo("branchId", branchId)
                .whereEqualTo("name", name)
                .get().await()

            if (!existingName.isEmpty) {
                return Result.failure(Exception("Employee with this name already exists in this branch"))
            }

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