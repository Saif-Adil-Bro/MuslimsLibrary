package com.example.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * WorkManager worker scheduled to show periodic local reading reminders.
 */
class ReadingReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val sharedPrefs = applicationContext.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val isReadingReminderEnabled = sharedPrefs.getBoolean("reading_reminders", true)

        if (isReadingReminderEnabled) {
            val notificationManager = LocalNotificationManager(applicationContext)
            
            val appName = applicationContext.getString(com.example.R.string.app_name)
            // Randomly select an inspiring message in Bengali
            val messages = listOf(
                "পবিত্র কুরআন এবং হাদিস রিডিং সময় হয়েছে। জ্ঞান অর্জনের সফর শুরু করুন!",
                "আজকে কি বই পড়েছেন? চলুন $appName-এ নতুন কিছু শিখি।",
                "জ্ঞান অর্জন করা প্রত্যেক মুসলমানের উপর ফরজ। $appName-এ আপনার পড়াশোনা বজায় রাখুন।"
            )
            val selectedMessage = messages.random()
            val title = "ইসলামিক রিডিং রিমাইন্ডার"

            notificationManager.showSimpleNotification(
                id = LocalNotificationManager.READING_NOTIFICATION_ID,
                title = title,
                body = selectedMessage,
                channelId = LocalNotificationManager.CHANNEL_READING_ID
            )

            // Save to internal app Notification Center
            val appContainer = (applicationContext as? com.example.MuslimsLibraryApplication)?.container
            val userId = appContainer?.authRepository?.let { it.getSupabaseUid() ?: it.getCurrentUserUid() } 
                ?: applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("user_id", null)

            if (userId != null) {
                try {
                    val db = com.example.data.local.AppDatabase.getDatabase(applicationContext)
                    val notificationEntity = com.example.data.local.entities.NotificationEntity(
                        id = "local_reading_" + System.currentTimeMillis().toString(),
                        userId = userId,
                        title = title,
                        message = selectedMessage,
                        type = "reminder",
                        data = "{}",
                        isRead = false,
                        createdAt = System.currentTimeMillis()
                    )
                    db.notificationDao().insertNotification(notificationEntity)
                    
                    appContainer?.supabaseService?.addNotificationLocallyAndRemotely(
                        userId = userId,
                        title = title,
                        body = selectedMessage,
                        type = "reminder"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ReadingWorker", "Failed to insert local notification", e)
                }
            }
        }

        return Result.success()
    }
}
