package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.LocalSyncRepository
import com.example.data.repository.BookRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookReaderState(
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val progressPercent: Float = 0f,
    val status: String = "reading",
    val isLoading: Boolean = true,
    val hasError: Boolean = false
)

class BookReaderViewModel(
    private val localSyncRepository: LocalSyncRepository,
    private val bookRepository: BookRepository,
    private val bookId: String,
    private val userId: String
) : ViewModel() {

    private val _currentState = MutableStateFlow(BookReaderState())
    val currentState: StateFlow<BookReaderState> = _currentState.asStateFlow()

    private var saveJob: Job? = null
    private val saveDebounceTime = 2000L
    private var lastSavedPage = -1
    private var lastSavedTime = System.currentTimeMillis()

    init {
        Log.d("PDF_Reader", "Initializing BookReaderViewModel for bookId: $bookId, userId: $userId")
        loadInitialProgress()
    }

    private fun loadInitialProgress() {
        viewModelScope.launch {
            // First load book details to get the baseline total page count
            try {
                val book = bookRepository.getBookById(bookId)
                if (book != null && book.pages > 0) {
                    _currentState.update { it.copy(totalPages = book.pages) }
                    Log.d("PDF_Reader", "Loaded book page count baseline from database copy: ${book.pages}")
                }
            } catch (e: Exception) {
                Log.e("PDF_Reader", "Error fetching baseline book details: ${e.message}")
            }

            try {
                localSyncRepository.getBookProgressFlow(userId, bookId).collect { progress ->
                    if (progress != null && _currentState.value.isLoading) {
                        _currentState.update {
                            it.copy(
                                currentPage = progress.currentPage,
                                totalPages = progress.totalPages,
                                progressPercent = if (progress.totalPages > 0) (progress.currentPage.toFloat() / progress.totalPages.toFloat()) * 100f else 0f,
                                status = progress.status,
                                isLoading = false
                            )
                        }
                        lastSavedPage = progress.currentPage
                        Log.d("PDF_Reader", "Loaded initial progress: Page ${progress.currentPage}/${progress.totalPages}")
                    } else if (progress == null && _currentState.value.isLoading) {
                        _currentState.update { it.copy(isLoading = false) }
                        Log.d("PDF_Reader", "No initial progress found, using default state")
                    }
                }
            } catch (e: Exception) {
                Log.e("PDF_Reader", "Failed to load book progress: ${e.message}")
                _currentState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onPageChanged(page: Int, total: Int) {
        if (page <= 0 || total <= 0) return
        Log.d("PDF_Reader", "Page changed: $page")
        
        _currentState.update { it.copy(
            currentPage = page,
            totalPages = total,
            progressPercent = (page.toFloat() / total.toFloat()) * 100f
        )}
        Log.d("PDF_Reader", "Current state: currentPage=${_currentState.value.currentPage}")

        // Debug logging requested
        Log.d("PDF_Debug", "Total pages from PDF: $total")
        Log.d("PDF_Debug", "Current page: $page")
        Log.d("PDF_Debug", "Progress: $page / $total")

        // Trigger debounced save
        onProgressChanged(page, total)
    }

    private fun onProgressChanged(currentPage: Int, totalPages: Int) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(saveDebounceTime)
            saveProgressToRoom(currentPage, totalPages, false)
        }
    }

    fun saveFinalProgress() {
        Log.d("PDF_Reader", "Screen disposed - saving final state")
        viewModelScope.launch {
            val state = _currentState.value
            saveProgressToRoom(state.currentPage, state.totalPages, true)
        }
    }

    private suspend fun saveProgressToRoom(currentPage: Int, totalPages: Int, isFinal: Boolean) {
        if (currentPage <= 0 || totalPages <= 0) return
        
        val now = System.currentTimeMillis()
        val timeDiff = now - lastSavedTime
        val additionalSeconds = timeDiff / 1000L

        val isCompleted = currentPage >= totalPages
        val targetStatus = if (isCompleted) "completed" else "reading"
        val targetPage = if (isCompleted) totalPages else currentPage

        Log.d("PDF_Reader", "Saving progress to Room... BookId=$bookId, Page=$targetPage/$totalPages, status=$targetStatus, duration=$additionalSeconds s")
        try {
            localSyncRepository.updateBookProgress(
                userId = userId,
                bookId = bookId,
                currentPage = targetPage,
                totalPages = totalPages,
                status = targetStatus,
                additionalReadingTimeSeconds = additionalSeconds
            )
            lastSavedPage = targetPage
            lastSavedTime = now
            Log.d("PDF_Reader", "Progress saved successfully to Room: Page $targetPage/$totalPages")
        } catch (e: Exception) {
            Log.e("PDF_Reader", "Failed to save progress to Room: ${e.message}")
        }
    }

    fun setTotalPages(total: Int) {
        if (total > 0 && _currentState.value.totalPages != total) {
            _currentState.update { it.copy(totalPages = total) }
            onProgressChanged(_currentState.value.currentPage, total)
        }
    }

    class Factory(
        private val localSyncRepository: LocalSyncRepository,
        private val bookRepository: BookRepository,
        private val bookId: String,
        private val userId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BookReaderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BookReaderViewModel(localSyncRepository, bookRepository, bookId, userId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
