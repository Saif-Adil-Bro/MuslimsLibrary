package com.example.data.repository

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.data.SupabaseUser
import com.example.data.SupabaseService
import com.example.BuildConfig
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.FirebaseNetworkException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// A clean extension function to await a Firebase style Task using native Cancellable Coroutines
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("An error occurred in Firebase Auth"))
        }
    }
}

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val supabaseClient: SupabaseClient,
    private val supabaseService: SupabaseService
) : AuthRepository {

    override fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    override fun getCurrentUserUid(): String? {
        return firebaseAuth.currentUser?.uid
    }

    override fun getCurrentUser(): com.google.firebase.auth.FirebaseUser? {
        return firebaseAuth.currentUser
    }

    override fun getSupabaseUid(): String? {
        return try {
            supabaseClient.auth.currentUserOrNull()?.id ?: supabaseClient.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            null
        }
    }

    private var lastQueryJsonResult: String = "No query run yet"

    override fun getLastQueryJson(): String = lastQueryJsonResult

    override suspend fun getUserRole(uid: String): String {
        android.util.Log.d("AuthRepo", "getUserRole called for uid: $uid")
        var resultRole: String? = null
        val logBuilder = StringBuilder()
        logBuilder.append("getUserRole UID: $uid\n")

        // 1. Try Query by id = uid
        try {
            logBuilder.append("[UID Query] Attempting id = '$uid'...\n")
            val response = supabaseClient.postgrest["users"].select {
                filter {
                    eq("id", uid)
                }
            }
            val dataStr = response.data
            lastQueryJsonResult = dataStr
            logBuilder.append("[UID Query] Response JSON: $dataStr\n")
            
            val users = response.decodeList<SupabaseUser>()
            logBuilder.append("[UID Query] Decoded list size: ${users.size}\n")
            val user = users.firstOrNull()
            if (user != null) {
                resultRole = user.role
                logBuilder.append("[UID Query] Success! Role = '$resultRole'\n")
            } else {
                logBuilder.append("[UID Query] Returned empty list.\n")
            }
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: e.message ?: "Unknown error"
            android.util.Log.e("AuthRepo", "getUserRole ID query failed: $errMsg", e)
            logBuilder.append("[UID Query] Failed: $errMsg\n")
            
            // Extract response if available in the exception (sometimes available in HTTP exceptions)
            try {
                lastQueryJsonResult = "ID query failed: $errMsg"
            } catch (ex: Exception) {}
        }

        // 2. Fallback: Query by email
        if (resultRole == null) {
            val email = getCurrentUserEmail()
            if (!email.isNullOrEmpty()) {
                try {
                    logBuilder.append("[Email Query] Attempting email = '$email'...\n")
                    val response = supabaseClient.postgrest["users"].select {
                        filter {
                            eq("email", email)
                        }
                    }
                    val dataStr = response.data
                    lastQueryJsonResult = dataStr
                    logBuilder.append("[Email Query] Response JSON: $dataStr\n")
                    
                    val users = response.decodeList<SupabaseUser>()
                    logBuilder.append("[Email Query] Decoded list size: ${users.size}\n")
                    val user = users.firstOrNull()
                    if (user != null) {
                        resultRole = user.role
                        logBuilder.append("[Email Query] Success! Role = '$resultRole'\n")
                    } else {
                        logBuilder.append("[Email Query] Returned empty list.\n")
                    }
                } catch (e: Exception) {
                    val errMsg = e.localizedMessage ?: e.message ?: "Unknown error"
                    android.util.Log.e("AuthRepo", "getUserRole Email query failed: $errMsg", e)
                    logBuilder.append("[Email Query] Failed: $errMsg\n")
                    
                    try {
                        lastQueryJsonResult = "Email query failed: $errMsg\nPrefix ID query failed too."
                    } catch (ex: Exception) {}
                }
            } else {
                logBuilder.append("[Email Query] Skipped (email is null or empty)\n")
            }
        }

        // 3. Fallback: Check raw Json for role if deserialization was weird
        if (resultRole == null && lastQueryJsonResult.isNotEmpty()) {
            try {
                if (lastQueryJsonResult.contains("\"role\": \"admin\"", ignoreCase = true) || 
                    lastQueryJsonResult.contains("\"role\":\"admin\"", ignoreCase = true)) {
                    resultRole = "admin"
                    logBuilder.append("[Regex Fallback] Detected role 'admin' in raw JSON!\n")
                } else if (lastQueryJsonResult.contains("\"role\": \"user\"", ignoreCase = true) || 
                           lastQueryJsonResult.contains("\"role\":\"user\"", ignoreCase = true)) {
                    resultRole = "user"
                    logBuilder.append("[Regex Fallback] Detected role 'user' in raw JSON!\n")
                }
            } catch (e: Exception) {
                logBuilder.append("[Regex Fallback] Regex check failed: ${e.message}\n")
            }
        }

        val finalRole = resultRole ?: "user"
        logBuilder.append("Final selected role: '$finalRole'")
        
        lastQueryJsonResult = logBuilder.toString()
        android.util.Log.d("AuthRepo", "getUserRole final summary:\n$lastQueryJsonResult")
        
        return finalRole
    }

    override fun isUserLoggedIn(): Flow<Boolean> = flow {
        emit(firebaseAuth.currentUser != null)
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        var lastException: Exception? = null
        for (attempt in 1..3) {
            try {
                withTimeout(60_000) { // 60 seconds for slow networks
                    firebaseAuth.signInWithEmailAndPassword(email, password).awaitTask()
                }
                return Result.success(Unit)
            } catch (e: TimeoutCancellationException) {
                lastException = Exception("Connection timeout. Your internet seems slow. Please try again or switch to WiFi.")
                if (attempt < 3) {
                    delay(2000) // Wait 2 seconds before retry
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                return Result.failure(Exception("Invalid email or password"))
            } catch (e: FirebaseAuthInvalidUserException) {
                return Result.failure(Exception("No account found with this email"))
            } catch (e: FirebaseNetworkException) {
                lastException = Exception("Network error. Please check your internet connection and try again.")
                if (attempt < 3) {
                    delay(2000)
                }
            } catch (e: Exception) {
                var cause: Throwable? = e
                var matched = false
                while (cause != null) {
                    if (cause is FirebaseAuthInvalidCredentialsException) {
                        return Result.failure(Exception("Invalid email or password"))
                    }
                    if (cause is FirebaseAuthInvalidUserException) {
                        return Result.failure(Exception("No account found with this email"))
                    }
                    if (cause is FirebaseNetworkException) {
                        lastException = Exception("Network error. Please check your internet connection and try again.")
                        matched = true
                        break
                    }
                    cause = cause.cause
                }
                if (!matched) {
                    return Result.failure(Exception("Login failed: ${e.localizedMessage}"))
                } else {
                    if (attempt < 3) {
                        delay(2000)
                    }
                }
            }
        }
        return Result.failure(lastException ?: Exception("Login failed after 3 attempts"))
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> {
        var firebaseCreatedUser = false
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).awaitTask()
            val user = authResult.user
            if (user != null) {
                firebaseCreatedUser = true
                try {
                    supabaseService.createUserProfile(user.uid, user.email ?: email)
                } catch (syncError: Exception) {
                    throw Exception("Profile sync failed: ${syncError.message}", syncError)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Registration failed: ${e.localizedMessage}"))
        }
    }

    override suspend fun signInWithGoogle(context: Context, activity: ComponentActivity): Result<Unit> {
        return try {
            if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty()) {
                return Result.failure(Exception("Google Sign-In is not configured. Please add 'GOOGLE_WEB_CLIENT_ID' in AI Studio Secrets."))
            }

            val credentialManager = CredentialManager.create(context)
            
            // Build google credentials request cleanly
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).awaitTask()
                
                authResult.user?.let { user ->
                    // Bug 2 Fix: Call the RPC function which uses SECURITY DEFINER to bypass RLS,
                    // ensuring that OAuth user profiles are synced perfectly to the database without RLS bypass failures.
                    supabaseService.createUserProfile(user.uid, user.email ?: "")
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Could not retrieve a Google ID token from response credentials"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInAnonymously(): Result<Unit> {
        return try {
            val authResult = firebaseAuth.signInAnonymously().awaitTask()
            val user = authResult.user
            if (user != null) {
                // Bug 2 Fix: Call the RPC function which uses SECURITY DEFINER to bypass RLS,
                // ensuring that guest user profiles are created correctly in the database.
                supabaseService.createUserProfile(user.uid, "guest_${user.uid}@muslimslibrary.org")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        try {
            firebaseAuth.signOut()
            supabaseClient.auth.clearSession()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                val uid = user.uid
                try {
                    supabaseClient.postgrest["users"].delete {
                        filter {
                            eq("id", uid)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                user.delete().awaitTask()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ensureProfileSynced(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                supabaseService.createUserProfile(user.uid, user.email ?: "")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
