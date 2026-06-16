package com.example

import android.app.Application
import android.content.Context
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.BookRepository
import com.example.data.repository.BookRepositoryImpl
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

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

    val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    
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

    val bookRepository: BookRepository by lazy {
        BookRepositoryImpl(supabaseClient)
    }

    val supabaseService: com.example.data.SupabaseService by lazy {
        com.example.data.SupabaseService(supabaseClient)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseAuth, supabaseClient, supabaseService)
    }
}

class MuslimsLibraryApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
