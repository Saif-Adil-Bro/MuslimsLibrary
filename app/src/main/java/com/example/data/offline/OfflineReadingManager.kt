package com.example.data.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class OfflineReadingManager(private val context: Context) {
    
    // নেটওয়ার্ক স্টেটাস
    private val _isOnline = MutableStateFlow(isNetworkAvailable())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    // ক্যাশ লিমিট
    private val _cacheLimit = MutableStateFlow(getCacheLimit())
    val cacheLimit: StateFlow<Long> = _cacheLimit.asStateFlow()
    
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    fun setCacheLimit(limitBytes: Long) {
        _cacheLimit.value = limitBytes
        context.getSharedPreferences("offline_settings", Context.MODE_PRIVATE)
            .edit().putLong("cache_limit", limitBytes).apply()
    }
    
    private fun getCacheLimit(): Long {
        return context.getSharedPreferences("offline_settings", Context.MODE_PRIVATE)
            .getLong("cache_limit", 100 * 1024 * 1024L) // Default 100MB
    }
    
    // স্মার্ট প্রি-লোড কাজ শিডিউল করা
    fun scheduleSmartPreload(bookId: String, nextPages: Int = 10) {
        val workRequest = OneTimeWorkRequestBuilder<SmartPreloadWorker>()
            .setInputData(workDataOf(
                "book_id" to bookId,
                "pages_to_preload" to nextPages
            ))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()        
        WorkManager.getInstance(context).enqueueUniqueWork(
            "preload_$bookId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    // অটো-সিঙ্ক কাজ শিডিউল করা
    fun scheduleAutoSync() {
        val syncRequest = PeriodicWorkRequestBuilder<AutoSyncWorker>(
            15, TimeUnit.MINUTES
        )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "auto_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }
    
    // ক্যাশ ক্লিয়ার
    fun clearUnusedCache(): Long {
        val cacheDir = context.cacheDir
        var freedSpace = 0L
        
        // লজিক: শেষ ৭ দিনে যে বই খোলা হয়নি তার ক্যাশ ডিলিট করা
        val prefs = context.getSharedPreferences("book_last_read", Context.MODE_PRIVATE)
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        
        cacheDir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.name.startsWith("book_")) {
                val bookId = file.name.removePrefix("book_")
                val lastRead = prefs.getLong(bookId, 0L)
                
                if (lastRead < sevenDaysAgo && lastRead != 0L) {
                    freedSpace += calculateDirSize(file)
                    deleteDir(file)
                }
            }
        }
        
        return freedSpace
    }    
    private fun calculateDirSize(dir: java.io.File): Long {
        var size = 0L
        dir.listFiles()?.forEach { 
            size += if (it.isDirectory) calculateDirSize(it) else it.length() 
        }
        return size
    }
    
    private fun deleteDir(dir: java.io.File): Boolean {
        dir.listFiles()?.forEach { 
            if (it.isDirectory) deleteDir(it) else it.delete() 
        }
        return dir.delete()
    }
}
