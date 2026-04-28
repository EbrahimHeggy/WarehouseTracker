package com.example.warehousetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.warehousetracker.navigation.AppNavigation
import com.example.warehousetracker.ui.theme.WarehouseTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WarehouseTrackerTheme {
                AppNavigation()
            }
        }
    }
}