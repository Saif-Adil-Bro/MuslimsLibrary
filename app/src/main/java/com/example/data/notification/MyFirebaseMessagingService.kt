package com.example.data.notification

import android.content.Context
import com.example.AppContainer
import com.example.data.SupabaseService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Service to handle incoming Firebase Cloud Messaging push notifications
 * and token updates, routing them to local notifications and Supabase database.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Save token locally in SharedPreferences
        val sharedPrefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("fcm_token", token).apply()

        // Inject container and register token to Supabase if logged in
        try {
            val appContainer = (application as? com.example.MuslimsLibraryApplication)?.container
            if (appContainer != null) {
                val isGuest = appContainer.guestModeManager.isGuestMode()
                val userId = appContainer.authRepository.getCurrentUserUid()
                
                if (!isGuest && userId != null) {
                    serviceScope.launch {
                        try {
                            appContainer.supabaseService.saveDeviceToken(userId, token)
                        } catch (e: Exception) {
                            android.util.Log.e("FCM_Service", "Failed to register fcm token: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FCM_Service", "Error during token generation registration: ${e.message}")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val notificationTitle = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "নতুন আপডেট"
        val notificationBody = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "মুসলমানদের লাইব্রেরিতে নতুন জিনিস যোগ করা হয়েছে।"

        // If the notifications preferences screen states push general is off, block it
        val sharedPrefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val isPushEnabled = sharedPrefs.getBoolean("general_notifications", true)

        if (isPushEnabled) {
            val localManager = LocalNotificationManager(applicationContext)
            localManager.showSimpleNotification(
                id = LocalNotificationManager.GENERAL_NOTIFICATION_ID,
                title = notificationTitle,
                body = notificationBody,
                channelId = LocalNotificationManager.CHANNEL_GENERAL_ID
            )

            // Optionally, we can also insert this notification into local DB if we wish, or just rely
            // on Supabase sync. Let's insert into local DB if available.
            try {
                val appContainer = (application as? com.example.MuslimsLibraryApplication)?.container
                if (appContainer != null) {
                    val isGuest = appContainer.guestModeManager.isGuestMode()
                    val userId = appContainer.authRepository.getCurrentUserUid()
                    if (!isGuest && userId != null) {
                        serviceScope.launch {
                            try {
                                appContainer.supabaseService.addNotificationLocallyAndRemotely(
                                    userId = userId,
                                    title = notificationTitle,
                                    body = notificationBody,
                                    type = "push"
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("FCM_Service", "Error saving received notification locally: ${e.message}")
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                android.util.Log.e("FCM_Service", "Could not insert notification into db: ${ex.message}")
            }
        }
    }
}
