package com.example.warehousetracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousetracker.data.model.UserProfile
import com.example.warehousetracker.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = true,
    val error: String = "",
    val profile: UserProfile? = null,
    val resetEmailSent: Boolean = false,
    val allUsers: List<UserProfile> = emptyList()
)


class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            val profile = repo.getCurrentProfile()
            _state.value = AuthState(
                isLoggedIn = profile != null,
                isLoading = false,
                profile = profile
            )
            if (profile?.role == "admin") {
                loadAllUsers()
            }
        }
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            val users = repo.getAllUsers()
            _state.value = _state.value.copy(allUsers = users)
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = "")
            val result = repo.login(email, password)
            if (result.isSuccess) {
                val profile = result.getOrNull()
                _state.value = AuthState(isLoggedIn = true, isLoading = false, profile = profile)
                if (profile?.role == "admin") {
                    loadAllUsers()
                }
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Wrong email or password"
                )
            }
        }
    }


    fun registerByAdmin(
        context: Context,
        email: String,
        password: String,
        name: String,
        role: String,
        branchId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repo.registerByAdmin(context, email, password, name, role, branchId)
            if (result.isSuccess) {
                loadAllUsers()
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }


    fun deleteUser(uid: String) {
        viewModelScope.launch {
            val result = repo.deleteUserRecord(uid)
            if (result.isSuccess) {
                loadAllUsers() // تحديث القائمة بعد الحذف
            }
        }
    }


    fun resetPassword(email: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = "")
            val result = repo.resetPassword(email)
            _state.value = if (result.isSuccess) {
                _state.value.copy(isLoading = false, resetEmailSent = true)
            } else {
                _state.value.copy(isLoading = false, error = "Email not found")
            }
        }
    }

    fun changePassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
                    .currentUser
                    ?.updatePassword(newPassword)
                    ?.await()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun clearResetState() {
        _state.value = _state.value.copy(resetEmailSent = false, error = "")
    }

    fun logout() {
        repo.logout()
        _state.value = AuthState(isLoggedIn = false, isLoading = false)
    }
}