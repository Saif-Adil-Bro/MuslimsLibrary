package com.example.ui.viewmodel

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsViewModel(
    private val context: Context
) : ViewModel() {

    // Theme
    private val _theme = MutableStateFlow("system") // light, dark, system
    val theme: StateFlow<String> = _theme.asStateFlow()

    // Language
    private val _language = MutableStateFlow("bn") // bn, en, ar, ur
    val language: StateFlow<String> = _language.asStateFlow()

    // Font Size
    private val _fontSize = MutableStateFlow("medium") // small, medium, large
    val fontSize: StateFlow<String> = _fontSize.asStateFlow()

    // Auto-save progress
    private val _autoSaveProgress = MutableStateFlow(true)
    val autoSaveProgress: StateFlow<Boolean> = _autoSaveProgress.asStateFlow()
    
    // App Lock
    private val _appLockEnabled = MutableStateFlow(false)
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    // Cache size
    private val _cacheSize = MutableStateFlow("0 MB")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    // Dialog visibility states
    private val _showThemeDialog = MutableStateFlow(false)
    val showThemeDialog: StateFlow<Boolean> = _showThemeDialog.asStateFlow()

    private val _showLanguageDialog = MutableStateFlow(false)
    val showLanguageDialog: StateFlow<Boolean> = _showLanguageDialog.asStateFlow()

    private val _showFontSizeDialog = MutableStateFlow(false)
    val showFontSizeDialog: StateFlow<Boolean> = _showFontSizeDialog.asStateFlow()

    fun openThemeDialog() {
        _showThemeDialog.value = true
    }

    fun closeThemeDialog() {
        _showThemeDialog.value = false
    }

    fun openLanguageDialog() {
        _showLanguageDialog.value = true
    }

    fun closeLanguageDialog() {
        _showLanguageDialog.value = false
    }

    fun openFontSizeDialog() {
        _showFontSizeDialog.value = true
    }

    fun closeFontSizeDialog() {
        _showFontSizeDialog.value = false
    }

    init {
        loadSettings()
        calculateCacheSize()
    }

    private fun loadSettings() {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        _theme.value = prefs.getString("theme", "system") ?: "system"
        _language.value = prefs.getString("language", "bn") ?: "bn"
        _fontSize.value = prefs.getString("font_size", "medium") ?: "medium"
        _autoSaveProgress.value = prefs.getBoolean("auto_save_progress", true)
        _appLockEnabled.value = prefs.getBoolean("app_lock_enabled", false)
    }

    fun updateTheme(theme: String) {
        _theme.value = theme
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit {putString("theme", theme)}
    }

    fun updateLanguage(language: String) {
        _language.value = language
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit {putString("language", language)}
        applyLanguage(language)
    }

    fun updateFontSize(size: String) {
        _fontSize.value = size
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit {putString("font_size", size)}
    }

    fun toggleAutoSaveProgress(enabled: Boolean) {
        _autoSaveProgress.value = enabled
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit {putBoolean("auto_save_progress", enabled)}
    }

    fun toggleAppLock(enabled: Boolean) {
        _appLockEnabled.value = enabled
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit {putBoolean("app_lock_enabled", enabled)}
    }

    private fun applyLanguage(language: String) {
        val locale = when (language) {
            "en" -> Locale.ENGLISH
            "ar" -> Locale("ar")
            "ur" -> Locale("ur")
            else -> Locale("bn")
        }
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun calculateCacheSize() {
        viewModelScope.launch {
            try {
                val cacheDir = context.cacheDir
                val size = calculateDirSize(cacheDir)
                _cacheSize.value = formatSize(size)
            } catch (e: Exception) {
                _cacheSize.value = "0 MB"
            }
        }
    }

    private fun calculateDirSize(dir: java.io.File): Long {
        var size = 0L
        if (dir.exists()) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    size += if (file.isDirectory) calculateDirSize(file) else file.length()
                }
            }
        }
        return size
    }

    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    fun clearCache(): Boolean {
        return try {
            val cacheDir = context.cacheDir
            deleteDir(cacheDir)
            _cacheSize.value = "0 MB"
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun deleteDir(dir: java.io.File): Boolean {
        if (dir.exists()) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        deleteDir(file)
                    } else {
                        file.delete()
                    }
                }
            }
        }
        return dir.delete()
    }

    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
