package com.example.data.repository

import android.content.Context
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUserEmail(): String?
    fun getCurrentUserUid(): String?
    suspend fun getUserRole(uid: String): String
    fun isUserLoggedIn(): Flow<Boolean>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signInWithGoogle(context: Context, activity: ComponentActivity): Result<Unit>
    suspend fun signInAnonymously(): Result<Unit>
    suspend fun logout()
    suspend fun deleteAccount(): Result<Unit>
}
