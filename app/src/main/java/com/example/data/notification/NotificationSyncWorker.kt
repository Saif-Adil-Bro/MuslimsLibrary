package com.example.data.notification

import android.content.Context
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MuslimsLibraryApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val appContainer = (applicationContext as? MuslimsLibraryApplication)?.container
            val supabaseService = appContainer?.supabaseService ?: return@withContext Result.failure()
            val authRepository = appContainer.authRepository
            
            val uid = authRepository.getSupabaseUid() ?: authRepository.getCurrentUserUid()
            if (uid == null) {
                return@withContext Result.success()
            }

            // check if pushed notifications are enabled
            val sharedPrefs = applicationContext.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            val isPushEnabled = sharedPrefs.getBoolean("general_notifications", true)
            if (!isPushEnabled) {
                return@withContext Result.success()
            }

            val appPrefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val lastSyncedTime = appPrefs.getString("last_sync_time", "0") ?: "0"

            val notifications = supabaseService.fetchNotifications(uid)
            val unreadServerNotifs = notifications.filter { !it.isRead && (it.sentAt ?: "") > lastSyncedTime }
            
            val localNotificationManager = LocalNotificationManager(applicationContext)

            var maxTime = lastSyncedTime
            for (notif in unreadServerNotifs) {
                val sentAtTime = notif.sentAt ?: ""
                localNotificationManager.showSimpleNotification(
                    id = notif.id.hashCode(),
                    title = notif.title,
                    body = notif.body,
                    type = notif.type
                )
                if (sentAtTime > maxTime) {
                    maxTime = sentAtTime
                }
            }

            if (maxTime > lastSyncedTime) {
                appPrefs.edit {putString("last_sync_time", maxTime)}
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("NotificationSync", "Error syncing notifications", e)
            Result.retry()
        }
    }
}
