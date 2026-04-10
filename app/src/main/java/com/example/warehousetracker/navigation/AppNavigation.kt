package com.example.warehousetracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehousetracker.ui.screens.AdminDashboardScreen
import com.example.warehousetracker.ui.screens.LoadingScreen
import com.example.warehousetracker.ui.screens.LoginScreen
import com.example.warehousetracker.ui.screens.RegisterScreen
import com.example.warehousetracker.ui.screens.UserScreen
import com.example.warehousetracker.ui.viewmodel.AuthViewModel

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.state.collectAsState()

    when {
        authState.isLoading -> LoadingScreen()
        !authState.isLoggedIn -> {
            var showRegister by remember { mutableStateOf(false) }
            if (showRegister) {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = { showRegister = false }
                )
            } else {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { showRegister = true }
                )
            }
        }

        authState.profile?.role == "admin" -> AdminDashboardScreen(
            authViewModel = authViewModel
        )

        else -> UserScreen(
            authViewModel = authViewModel
        )
    }
}