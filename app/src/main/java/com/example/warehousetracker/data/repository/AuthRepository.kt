package com.example.warehousetracker.data.repository

import android.content.Context
import com.example.warehousetracker.data.model.UserProfile
import com.google.firebase.FirebaseApp
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
            val uid = auth.currentUser?.uid ?: throw Exception("Auth failed")

            // التأكد من وجود سجل في Firestore
            val doc = db.collection("users").document(uid).get().await()
            if (!doc.exists()) {
                auth.signOut() // طرده فوراً لو مش موجود في الداتابيز
                return Result.failure(Exception("This account has been deactivated by Admin."))
            }

            val profile = UserProfile(
                uid = uid,
                email = doc.getString("email") ?: "",
                name = doc.getString("name") ?: "",
                role = doc.getString("role") ?: "user",
                branchId = doc.getString("branchId") ?: ""
            )
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerByAdmin(
        context: Context,
        email: String,
        password: String,
        name: String,
        role: String,
        branchId: String
    ): Result<Unit> {
        var secondaryApp: FirebaseApp? = null
        return try {
            val currentOptions = FirebaseApp.getInstance().options
            val secondaryAppName = "SecondaryApp_${System.currentTimeMillis()}"
            secondaryApp = FirebaseApp.initializeApp(context, currentOptions, secondaryAppName)
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

            val result = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user!!.uid

            try {
                val userMap = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "name" to name,
                    "role" to role,
                    "branchId" to branchId
                )
                db.collection("users").document(uid).set(userMap).await()
                Result.success(Unit)
            } catch (firestoreEx: Exception) {
                result.user?.delete()?.await()
                Result.failure(firestoreEx)
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            secondaryApp?.delete()
        }
    }

    suspend fun getAllUsers(): List<UserProfile> {
        return try {
            db.collection("users").get().await().documents.map { doc ->
                UserProfile(
                    uid = doc.id,
                    email = doc.getString("email") ?: "",
                    name = doc.getString("name") ?: "",
                    role = doc.getString("role") ?: "user",
                    branchId = doc.getString("branchId") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteUserRecord(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (!doc.exists()) return null
            UserProfile(
                uid = uid,
                email = doc.getString("email") ?: "",
                name = doc.getString("name") ?: "",
                role = doc.getString("role") ?: "user",
                branchId = doc.getString("branchId") ?: ""
            )
        } catch (e: Exception) {
            null
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