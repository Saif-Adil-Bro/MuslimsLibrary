package com.example.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    private lateinit var database: AppDatabase
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
    }
    
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
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val title = message.notification?.title ?: message.data["title"] ?: "নতুন আপডেট"
        val body = message.notification?.body ?: message.data["body"] ?: "মুসলমানদের লাইব্রেরিতে নতুন জিনিস যোগ করা হয়েছে।"
        val data = message.data
        
        // If the notifications preferences screen states push general is off, block it
        val sharedPrefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val isPushEnabled = sharedPrefs.getBoolean("general_notifications", true)
        
        if (isPushEnabled) {
            // 1. Show system notification in notification drawer
            showSystemNotification(title, body, data)
            
            // 2. Save notification to Room database (for in-app notification center)
            saveNotificationToDatabase(title, body, data)
        }
    }
    
    private fun showSystemNotification(title: String, body: String, data: Map<String, String>) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "muslims_library_notifications"
        
        // Create custom notification channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MuslimsLibrary Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from MuslimsLibrary app"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Build intent redirection
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "notification_center")
            data["book_id"]?.let { putExtra("book_id", it) }
            data["post_id"]?.let { putExtra("post_id", it) }
            data["type"]?.let { putExtra("notification_type", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder) // Using crash-safe system drawable
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun saveNotificationToDatabase(title: String, body: String, data: Map<String, String>) {
        serviceScope.launch {
            try {
                // Determine user uid
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ?: getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("user_id", null)
                    ?: return@launch
                
                val notificationType = data["type"] ?: "system"
                
                // Encode Map<String, String> into JSON string
                val jsonData = try {
                    buildJsonObject {
                        data.forEach { (k, v) ->
                            put(k, v)
                        }
                    }.toString()
                } catch (e: Exception) {
                    "{}"
                }
                
                // Add Locally to database
                val notificationEntity = com.example.data.local.entities.NotificationEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    title = title,
                    message = body,
                    type = notificationType,
                    data = jsonData,
                    isRead = false,
                    createdAt = System.currentTimeMillis()
                )
                
                database.notificationDao().insertNotification(notificationEntity)
                
                // Also add remotely to Supabase synchronization to keep online history in sync
                try {
                    val appContainer = (application as? com.example.MuslimsLibraryApplication)?.container
                    appContainer?.supabaseService?.addNotificationLocallyAndRemotely(
                        userId = userId,
                        title = title,
                        body = body,
                        type = notificationType
                    )
                } catch (e: Exception) {
                    android.util.Log.e("FCM", "Failed to insert remotely: ${e.message}")
                }
                
                android.util.Log.d("FCM", "Notification saved to database: $title")
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Error saving notification: ${e.message}", e)
            }
        }
    }
}
