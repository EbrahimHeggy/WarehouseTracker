package com.example.warehousetracker.data.repository

import com.example.warehousetracker.data.model.Branch
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class BranchRepository {
    private val db = Firebase.firestore

    suspend fun getBranches(): List<Branch> {
        return try {
            db.collection("branches").get().await().documents.map { doc ->
                Branch(id = doc.id, name = doc.getString("name") ?: "")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addBranch(name: String): Result<Branch> {
        return try {
            val ref = db.collection("branches").add(mapOf("name" to name)).await()
            Result.success(Branch(id = ref.id, name = name))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBranch(branchId: String): Result<Unit> {
        return try {
            db.collection("branches").document(branchId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}