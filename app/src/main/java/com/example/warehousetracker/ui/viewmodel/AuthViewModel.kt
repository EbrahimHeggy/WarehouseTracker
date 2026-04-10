package com.example.warehousetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousetracker.data.model.UserProfile
import com.example.warehousetracker.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = true,
    val error: String = "",
    val profile: UserProfile? = null
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
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = "")
            val result = repo.login(email, password)
            _state.value = if (result.isSuccess) {
                AuthState(isLoggedIn = true, isLoading = false, profile = result.getOrNull())
            } else {
                _state.value.copy(isLoading = false, error = "Wrong email or password")
            }
        }
    }

    fun register(email: String, password: String, name: String, role: String, branchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = "")
            val result = repo.register(email, password, name, role, branchId)
            _state.value = if (result.isSuccess) {
                AuthState(isLoggedIn = true, isLoading = false, profile = result.getOrNull())
            } else {
                _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Error"
                )
            }
        }
    }

    fun logout() {
        repo.logout()
        _state.value = AuthState(isLoggedIn = false, isLoading = false)
    }
}