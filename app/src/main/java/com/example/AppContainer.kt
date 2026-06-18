package com.example

import android.content.Context
import com.example.data.SupabaseService
import com.example.data.local.AppDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.BookRepository
import com.example.data.repository.BookRepositoryImpl
import com.example.data.repository.LocalSyncRepository
import com.example.data.repository.LocalSyncRepositoryImpl
import com.example.data.sync.SyncManager
import com.example.data.util.LiveNetworkMonitor
import com.example.data.util.NetworkMonitor
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Dependency Injection container for managing application-scoped dependencies.
 * Follows a strict dependency declaration order:
 * 1. Initialize SupabaseClient (Third-party Core Client)
 * 2. Instantiate SupabaseService (Wrapper for Supabase database / RPC operations)
 * 3. Instantiate AuthRepositoryImpl (Business logic layer consuming Core Services)
 */
class AppContainer(val context: Context) {
    init {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY.ifEmpty { "dummy-api-key-for-local-compatibility" })
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID.ifEmpty { "com.aistudio.muslimslibrary.kuytw" })
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID.ifEmpty { "muslimslibrary-kuytw" })
                    .setGcmSenderId(BuildConfig.FIREBASE_MESSAGING_SENDER_ID.ifEmpty { "12345678" })
                    .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET.ifEmpty { "muslimslibrary-kuytw.appspot.com" })
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Firebase Authentication ---
    val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // --- 1. Supabase Client Initialization ---
    // Configured using environment variables injected by BuildConfig
    val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Storage)
            install(Auth)
        }
    }

    // --- 2. Supabase Service Wrapper ---
    // Instantiated using the previously initialized Supabase Client
    val supabaseService: SupabaseService by lazy {
        SupabaseService(supabaseClient, context)
    }

    // --- 3. Authentication & User Profile Repository ---
    // Combines Firebase authentication and Supabase services for synchronized flows
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseAuth, supabaseClient, supabaseService)
    }

    // --- Book Repository ---
    val bookRepository: BookRepository by lazy {
        BookRepositoryImpl(supabaseClient)
    }

    // --- 4. Room Database and Offline-First Synchronization Wrapper ---
    val appDatabase: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val networkMonitor: NetworkMonitor by lazy {
        LiveNetworkMonitor(context)
    }

    val syncManager: SyncManager by lazy {
        SyncManager(context, appDatabase, supabaseService, networkMonitor)
    }

    val localSyncRepository: LocalSyncRepository by lazy {
        LocalSyncRepositoryImpl(appDatabase, syncManager)
    }
}
