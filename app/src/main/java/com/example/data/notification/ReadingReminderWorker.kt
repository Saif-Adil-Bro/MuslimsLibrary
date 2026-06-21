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
            
            // Randomly select an inspiring message in Bengali
            val messages = listOf(
                "পবিত্র কুরআন এবং হাদিস রিডিং সময় হয়েছে। জ্ঞান অর্জনের সফর শুরু করুন!",
                "আজকে কি বই পড়েছেন? চলুন মুসলমানদের লাইব্রেরিতে নতুন কিছু শিখি।",
                "জ্ঞান অর্জন করা প্রত্যেক মুসলমানের উপর ফরজ। মুসলমানদের লাইব্রেরিতে আপনার পড়াশোনা বজায় রাখুন।"
            )
            val selectedMessage = messages.random()

            notificationManager.showSimpleNotification(
                id = LocalNotificationManager.READING_NOTIFICATION_ID,
                title = "ইসলামিক রিডিং রিমাইন্ডার",
                body = selectedMessage,
                channelId = LocalNotificationManager.CHANNEL_READING_ID
            )
        }

        return Result.success()
    }
}
