package com.example.warehousetracker.data.repository

import com.example.warehousetracker.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    val isLoggedIn get() = auth.currentUser != null
    val currentUid get() = auth.currentUser?.uid

    suspend fun login(email: String, password: String): Result<UserProfile> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val profile = getUserProfile(auth.currentUser!!.uid)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        email: String,
        password: String,
        name: String,
        role: String,
        branchId: String
    ): Result<UserProfile> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user!!.uid
            val profile = UserProfile(
                uid = uid,
                email = email,
                name = name,
                role = role,
                branchId = branchId
            )
            db.collection("users").document(uid).set(profile).await()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            UserProfile(
                uid = uid,
                email = doc.getString("email") ?: "",
                name = doc.getString("name") ?: "",
                role = doc.getString("role") ?: "user",
                branchId = doc.getString("branchId") ?: ""
            )
        } catch (e: Exception) {
            UserProfile(uid = uid)
        }
    }

    suspend fun getCurrentProfile(): UserProfile? {
        val uid = currentUid ?: return null
        return getUserProfile(uid)
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()
}