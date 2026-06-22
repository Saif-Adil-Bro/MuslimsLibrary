package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.SupabaseNotification
import com.example.data.SupabaseNotificationPrefs
import com.example.data.SupabaseService
import com.example.data.local.AppDatabase
import com.example.data.local.entities.NotificationEntity
import com.example.data.notification.ReadingReminderWorker
import com.example.data.util.GuestModeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

sealed class NotificationUiState {
    object Loading : NotificationUiState()
    data class Success(val notifications: List<SupabaseNotification>) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}

class NotificationViewModel(
    private val supabaseService: SupabaseService,
    private val guestModeManager: GuestModeManager,
    private val context: Context,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // Preferences states
    private val _pushedNotificationsEnabled = MutableStateFlow(true)
    val pushedNotificationsEnabled: StateFlow<Boolean> = _pushedNotificationsEnabled.asStateFlow()

    private val _readingRemindersEnabled = MutableStateFlow(true)
    val readingRemindersEnabled: StateFlow<Boolean> = _readingRemindersEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(20) // default 8 PM
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(0)
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    private val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    private var isObserving = false

    init {
        loadPreferences()
        refreshNotifications()
    }

    private fun loadPreferences() {
        val fcmEnabled = sharedPrefs.getBoolean("general_notifications", true)
        val readEnabled = sharedPrefs.getBoolean("reading_reminders", true)
        val hour = sharedPrefs.getInt("reminder_hour", 20)
        val minute = sharedPrefs.getInt("reminder_minute", 0)

        _pushedNotificationsEnabled.value = fcmEnabled
        _readingRemindersEnabled.value = readEnabled
        _reminderHour.value = hour
        _reminderMinute.value = minute

        // If not a guest user, pull cloud prefs
        viewModelScope.launch {
            if (!guestModeManager.isGuestMode()) {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val prefs = supabaseService.fetchNotificationPrefs(uid)
                if (prefs != null) {
                    _pushedNotificationsEnabled.value = prefs.prayerReminders // using prayerReminders field as general push placeholder
                    _readingRemindersEnabled.value = prefs.readingReminders
                }
            }
        }
    }

    fun refreshNotifications() {
        if (guestModeManager.isGuestMode()) {
            _uiState.value = NotificationUiState.Success(emptyList())
            _unreadCount.value = 0
            return
        }

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _uiState.value = NotificationUiState.Success(emptyList())
            _unreadCount.value = 0
            return
        }

        // 1. Observe local notifications
        observeLocalNotifications(uid)

        // 2. Fetch from cloud in background and sync to local database
        viewModelScope.launch {
            try {
                val list = supabaseService.fetchNotifications(uid)
                for (item in list) {
                    val entity = NotificationEntity(
                        id = item.id,
                        userId = item.userId,
                        title = item.title,
                        message = item.body,
                        type = item.type,
                        data = "{}",
                        isRead = item.isRead,
                        createdAt = try {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                                timeZone = java.util.TimeZone.getTimeZone("UTC")
                            }
                            item.sentAt?.let { sdf.parse(it)?.time } ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                    )
                    appDatabase.notificationDao().insertNotification(entity)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Cloud fetch / sync failed: ${e.message}")
            }
        }
    }

    private fun observeLocalNotifications(uid: String) {
        if (isObserving) return
        isObserving = true

        viewModelScope.launch {
            appDatabase.notificationDao().getNotificationsForUser(uid).collect { entities ->
                val list = entities.map { entity ->
                    SupabaseNotification(
                        id = entity.id,
                        userId = entity.userId,
                        title = entity.title,
                        body = entity.message,
                        type = entity.type,
                        isRead = entity.isRead,
                        sentAt = try {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                                timeZone = java.util.TimeZone.getTimeZone("UTC")
                            }
                            sdf.format(java.util.Date(entity.createdAt))
                        } catch (e: Exception) {
                            null
                        }
                    )
                }
                _uiState.value = NotificationUiState.Success(list)
                _unreadCount.value = list.count { !it.isRead }
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            try {
                appDatabase.notificationDao().markAsRead(id)
                supabaseService.markNotificationAsRead(id)
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Error marking read: ${e.message}")
            }
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            try {
                appDatabase.notificationDao().deleteNotification(id)
                supabaseService.deleteNotification(id)
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Error deleting notification: ${e.message}")
            }
        }
    }

    fun clearAllNotifications() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                appDatabase.notificationDao().deleteAllNotifications(uid)
                supabaseService.clearAllNotifications(uid)
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Error clearing notifications: ${e.message}")
            }
        }
    }

    fun setPushNotificationsEnabled(enabled: Boolean) {
        _pushedNotificationsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("general_notifications", enabled).apply()

        viewModelScope.launch {
            if (!guestModeManager.isGuestMode()) {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                supabaseService.saveNotificationPrefs(
                    SupabaseNotificationPrefs(
                        userId = uid,
                        prayerReminders = enabled,
                        readingReminders = _readingRemindersEnabled.value,
                        dailyHadith = true
                    )
                )
            }
        }
    }

    fun setReadingRemindersEnabled(enabled: Boolean) {
        _readingRemindersEnabled.value = enabled
        sharedPrefs.edit().putBoolean("reading_reminders", enabled).apply()

        if (enabled) {
            scheduleWorkManagerReminder(_reminderHour.value, _reminderMinute.value)
        } else {
            cancelWorkManagerReminder()
        }

        viewModelScope.launch {
            if (!guestModeManager.isGuestMode()) {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                supabaseService.saveNotificationPrefs(
                    SupabaseNotificationPrefs(
                        userId = uid,
                        prayerReminders = _pushedNotificationsEnabled.value,
                        readingReminders = enabled,
                        dailyHadith = true
                    )
                )
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        _reminderHour.value = hour
        _reminderMinute.value = minute
        sharedPrefs.edit()
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()

        if (_readingRemindersEnabled.value) {
            scheduleWorkManagerReminder(hour, minute)
        }
    }

    private fun scheduleWorkManagerReminder(hour: Int, minute: Int) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<ReadingReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "reading_reminder_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun cancelWorkManagerReminder() {
        WorkManager.getInstance(context).cancelUniqueWork("reading_reminder_work")
    }

    class Factory(
        private val supabaseService: SupabaseService,
        private val guestModeManager: GuestModeManager,
        private val context: Context,
        private val appDatabase: AppDatabase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NotificationViewModel(supabaseService, guestModeManager, context, appDatabase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
