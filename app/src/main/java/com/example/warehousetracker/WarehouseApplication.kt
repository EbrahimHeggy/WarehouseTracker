package com.example.warehousetracker

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class WarehouseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable Firestore Offline Persistence
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        Firebase.firestore.setFirestoreSettings(settings)
    }
}
