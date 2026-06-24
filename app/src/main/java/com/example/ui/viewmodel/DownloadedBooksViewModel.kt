package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SupabaseService
import com.example.data.download.DownloadManager
import com.example.data.local.AppDatabase
import com.example.data.local.entities.DownloadedBook
import com.example.data.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class DownloadedBooksViewModel(
    private val appDatabase: AppDatabase,
    val downloadManager: DownloadManager,
    private val supabaseService: SupabaseService,
    private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showOfflineOnly = MutableStateFlow(false)
    val showOfflineOnly: StateFlow<Boolean> = _showOfflineOnly.asStateFlow()

    private val _storageUsage = MutableStateFlow(0L)
    val storageUsage: StateFlow<Long> = _storageUsage.asStateFlow()

    val downloadProgress: StateFlow<Map<String, Int>> = downloadManager.downloadStates
        .map { states ->
            states.mapValues { it.value.progress }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val downloadStatesFlow = downloadManager.downloadStates

    val downloadedBooks: StateFlow<List<DownloadedBook>> = combine(
        appDatabase.downloadDao().getAllBooksFlow(), // Update to get all books
        _searchQuery,
        _showOfflineOnly
    ) { books, query, offlineOnly ->
        var filteredBooks = books
        if (offlineOnly) {
            filteredBooks = filteredBooks.filter { it.downloadStatus == "completed" }
        }
        if (query.isNotBlank()) {
            filteredBooks = filteredBooks.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true)
            }
        }
        filteredBooks
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        calculateStorageUsage()
    }

    fun toggleOfflineFilter() {
        _showOfflineOnly.value = !_showOfflineOnly.value
    }

    fun calculateStorageUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            var totalSize = 0L
            cacheDir.listFiles()?.forEach { 
                totalSize += if (it.isDirectory) calculateDirSize(it) else it.length() 
            }
            // Add books dir size too
            val booksDir = downloadManager.getBooksDir()
            if (booksDir.exists()) {
                booksDir.listFiles()?.forEach {
                    totalSize += if (it.isDirectory) calculateDirSize(it) else it.length()
                }
            }
            _storageUsage.value = totalSize
        }
    }
    
    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { 
            size += if (it.isDirectory) calculateDirSize(it) else it.length() 
        }
        return size
    }

    fun clearAllOfflineBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name.startsWith("book_")) {
                    deleteDir(file)
                }
            }
            
            // Delete all books
            val booksDir = downloadManager.getBooksDir()
            if (booksDir.exists()) {
                booksDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            }

            // Database থেকেও ডাউনলোড স্ট্যাটাস রিসেট করুন
            appDatabase.downloadDao().clearAllDownloads()
            calculateStorageUsage()
        }
    }
    
    private fun deleteDir(dir: File): Boolean {
        dir.listFiles()?.forEach { 
            if (it.isDirectory) deleteDir(it) else it.delete() 
        }
        return dir.delete()
    }

    fun loadDownloadedBooks() {
        // Reactively connected, we don't need manual reloading, but we can log for tracing.
    }

    fun searchBooks(query: String) {
        _searchQuery.value = query
    }

    fun deleteDownload(bookId: String) {
        viewModelScope.launch {
            downloadManager.deleteDownload(bookId)
        }
    }

    fun isBookDownloaded(bookId: String): Boolean {
        return downloadedBooks.value.any { it.bookId == bookId && it.downloadStatus == "completed" }
    }

    fun getDownloadedFilePath(bookId: String): String? {
        val book = downloadedBooks.value.firstOrNull { it.bookId == bookId }
        return book?.localFilePath
    }

    fun startDownload(book: Book) {
        downloadManager.startDownload(book)
    }

    fun pauseDownload(bookId: String) {
        downloadManager.pauseDownload(bookId)
    }

    fun resumeDownload(book: Book) {
        downloadManager.resumeDownload(book)
    }

    class Factory(
        private val appDatabase: AppDatabase,
        private val downloadManager: DownloadManager,
        private val supabaseService: SupabaseService,
        private val context: Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DownloadedBooksViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DownloadedBooksViewModel(appDatabase, downloadManager, supabaseService, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
