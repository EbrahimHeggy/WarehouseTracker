package com.example.warehousetracker.navigation


sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object AdminDashboard : Screen("admin_dashboard")
    object UserScreen : Screen("user_screen")
}