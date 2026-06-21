package com.example.data.util

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class GuestModeManager(
    private val firebaseAuth: FirebaseAuth
) {
    // Check if user is guest (not authenticated or anonymously signed in)
    fun isGuestMode(): Boolean {
        val user = firebaseAuth.currentUser
        return user == null || user.isAnonymous
    }
    
    // Get user ID (null or "guest" for guests)
    fun getUserId(): String? {
        val user = firebaseAuth.currentUser
        return if (user == null || user.isAnonymous) null else user.uid
    }
    
    // Get display name
    fun getDisplayName(): String {
        return if (isGuestMode()) {
            "গেস্ট ইউজার"
        } else {
            val name = firebaseAuth.currentUser?.displayName
            if (name.isNullOrBlank()) "ব্যবহারকারী" else name
        }
    }
    
    // Flow to observe auth state changes
    fun observeAuthState(): Flow<Boolean> {
        return callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                val user = auth.currentUser
                val isLoggedInAndNotGuest = user != null && !user.isAnonymous
                trySend(isLoggedInAndNotGuest)
            }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }
    }
}
