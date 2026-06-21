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
    private val context: Context
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
        _uiState.value = NotificationUiState.Loading
        viewModelScope.launch {
            try {
                if (guestModeManager.isGuestMode()) {
                    // Guests only have reading reminder, return empty notification center
                    _uiState.value = NotificationUiState.Success(emptyList())
                    _unreadCount.value = 0
                } else {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == null) {
                        _uiState.value = NotificationUiState.Success(emptyList())
                        _unreadCount.value = 0
                    } else {
                        val list = supabaseService.fetchNotifications(uid)
                        _uiState.value = NotificationUiState.Success(list)
                        _unreadCount.value = list.count { !it.isRead }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = NotificationUiState.Error(e.message ?: "নোটিফিকেশন লোড করতে সমস্যা হয়েছে।")
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            supabaseService.markNotificationAsRead(id)
            // local update
            val currentState = _uiState.value
            if (currentState is NotificationUiState.Success) {
                val updated = currentState.notifications.map {
                    if (it.id == id) it.copy(isRead = true) else it
                }
                _uiState.value = NotificationUiState.Success(updated)
                _unreadCount.value = updated.count { !it.isRead }
            }
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            supabaseService.deleteNotification(id)
            val currentState = _uiState.value
            if (currentState is NotificationUiState.Success) {
                val updated = currentState.notifications.filter { it.id != id }
                _uiState.value = NotificationUiState.Success(updated)
                _unreadCount.value = updated.count { !it.isRead }
            }
        }
    }

    fun clearAllNotifications() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            supabaseService.clearAllNotifications(uid)
            _uiState.value = NotificationUiState.Success(emptyList())
            _unreadCount.value = 0
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
        private val context: Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NotificationViewModel(supabaseService, guestModeManager, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
