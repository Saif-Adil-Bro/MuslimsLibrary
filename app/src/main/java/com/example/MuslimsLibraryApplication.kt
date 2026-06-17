package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MuslimsLibraryApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        
        // Ensure Firebase check is performed at start of application
        val apiKey = BuildConfig.FIREBASE_API_KEY
        if (apiKey.isBlank() || apiKey.contains("placeholder") || apiKey == "dummy-api-key-for-local-compatibility") {
            Log.e("MuslimsLibraryApp", "⚠️ CRITICAL CONFIGURATION ERROR: Firebase API Key is empty or placeholder!")
        } else {
            Log.d("MuslimsLibraryApp", "Firebase API Key check passed successfully.")
        }

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY.ifEmpty { "dummy-api-key-for-local-compatibility" })
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID.ifEmpty { "com.aistudio.muslimslibrary.kuytw" })
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID.ifEmpty { "muslimslibrary-kuytw" })
                    .setGcmSenderId(BuildConfig.FIREBASE_MESSAGING_SENDER_ID.ifEmpty { "12345678" })
                    .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET.ifEmpty { "muslimslibrary-kuytw.appspot.com" })
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("MuslimsLibraryApp", "Firebase initialized successfully in Application onCreate.")
            }
        } catch (e: Exception) {
            Log.e("MuslimsLibraryApp", "Failed to initialize Firebase: ${e.message}", e)
        }

        container = AppContainer(this)
    }
}

