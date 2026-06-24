package com.example.data.offline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity

class AutoSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        try {
            // ব্যাকগ্রাউন্ডে অটো-সিঙ্ক লজিক
            showSyncNotification("সিঙ্ক চলছে...", "আপনার বই এবং প্রোগ্রেস সিঙ্ক হচ্ছে")
            
            // আপনার existing backup/sync logic কল করুন
            
            showSyncNotification("সিঙ্ক সম্পূর্ণ", "সব ডাটা সফলভাবে সিঙ্ক হয়েছে")
            return Result.success()
        } catch (e: Exception) {
            showSyncNotification("সিঙ্ক ব্যর্থ", "ইন্টারনেট সংযোগ চেক করুন")
            return Result.retry()
        }
    }
    
    private fun showSyncNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sync_channel",
                "সিঙ্ক নোটিফিকেশন",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, "sync_channel")
            .setSmallIcon(android.R.drawable.ic_popup_sync) // default icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
