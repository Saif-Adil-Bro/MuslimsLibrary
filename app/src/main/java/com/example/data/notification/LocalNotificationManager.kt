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

/**
 * Manages local and push notifications, handles notification channels,
 * and schedules visual reminder alerts.
 */
class LocalNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_READING_ID = "reading_reminders"
        const val CHANNEL_GENERAL_ID = "general_notifications"
        const val READING_NOTIFICATION_ID = 2001
        const val GENERAL_NOTIFICATION_ID = 2002
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: Local Reading Reminders
            val readingChannel = NotificationChannel(
                CHANNEL_READING_ID,
                "রিডিং রিমাইন্ডার (Reading Reminders)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "কুরআন ও হাদিস পাঠ স্মরণ করিয়ে দেয়ার জন্য নোটিফিকেশন।"
                setShowBadge(true)
            }

            // Channel 2: General Notifications
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL_ID,
                "সাধারণ নোটিফিকেশন (General Notifications)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "লাইব্রেরির নতুন বই এবং গুরুত্বপূর্ণ আপডেট নোটিফিকেশন।"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(readingChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    /**
     * Shows a standard text notification with a redirection to MainActivity.
     */
    fun showSimpleNotification(
        id: Int,
        title: String,
        body: String,
        channelId: String = CHANNEL_GENERAL_ID
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "notification_center")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Using standard launcher icon as system notification icon fallback
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder) // Using standard system drawable as icon for crash prevention
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(id, builder.build())
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Error displaying notification", e)
        }
    }
}
