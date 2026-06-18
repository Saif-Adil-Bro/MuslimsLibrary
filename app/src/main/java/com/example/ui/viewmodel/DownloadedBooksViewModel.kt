package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SupabaseService
import com.example.data.download.DownloadManager
import com.example.data.local.AppDatabase
import com.example.data.local.entities.DownloadedBook
import com.example.data.model.Book
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DownloadedBooksViewModel(
    private val appDatabase: AppDatabase,
    val downloadManager: DownloadManager,
    private val supabaseService: SupabaseService
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val downloadProgress: StateFlow<Map<String, Int>> = downloadManager.downloadStates
        .map { states ->
            states.mapValues { it.value.progress }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val downloadStatesFlow = downloadManager.downloadStates

    val downloadedBooks: StateFlow<List<DownloadedBook>> = combine(
        appDatabase.downloadDao().getAllDownloadedBooksFlow(),
        _searchQuery
    ) { books, query ->
        if (query.isBlank()) {
            books
        } else {
            books.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        private val supabaseService: SupabaseService
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DownloadedBooksViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DownloadedBooksViewModel(appDatabase, downloadManager, supabaseService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
