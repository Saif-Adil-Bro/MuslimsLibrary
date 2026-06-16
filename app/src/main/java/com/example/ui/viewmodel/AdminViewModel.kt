package com.example.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.SupabaseBook
import com.example.data.SupabaseService
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface AdminUiState {
    object Idle : AdminUiState
    data class Uploading(val progress: Int) : AdminUiState
    object Success : AdminUiState
    data class Error(val message: String) : AdminUiState
}

class AdminViewModel(
    private val supabaseClient: SupabaseClient,
    private val supabaseService: SupabaseService
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _totalBooksCount = MutableStateFlow(0)
    val totalBooksCount: StateFlow<Int> = _totalBooksCount.asStateFlow()

    init {
        fetchTotalBooksCount()
    }

    fun fetchTotalBooksCount() {
        viewModelScope.launch {
            try {
                val books = supabaseService.fetchPublicBooks()
                _totalBooksCount.value = books.size
            } catch (e: Exception) {
                _totalBooksCount.value = 0
            }
        }
    }

    fun resetState() {
        _uiState.value = AdminUiState.Idle
    }

    fun uploadBook(
        contentResolver: ContentResolver,
        title: String,
        author: String,
        category: String,
        coverImageUri: Uri?,
        bookFileUri: Uri,
        fileType: String
    ) {
        _uiState.value = AdminUiState.Uploading(10)
        viewModelScope.launch {
            try {
                val uuid = UUID.randomUUID().toString()

                // Step 1: Upload the cover image to Supabase Storage bucket 'books' with path 'covers/{uuid}.jpg'
                var generatedCoverUrl: String? = null
                if (coverImageUri != null) {
                    val coverBytes = withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(coverImageUri)?.use { it.readBytes() }
                    }
                    if (coverBytes != null && coverBytes.isNotEmpty()) {
                        val coverPath = "covers/$uuid.jpg"
                        _uiState.value = AdminUiState.Uploading(30)
                        supabaseClient.storage.from("books").upload(coverPath, coverBytes)
                        generatedCoverUrl = "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/books/$coverPath"
                    }
                }

                if (generatedCoverUrl == null) {
                    generatedCoverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400"
                }

                // Step 2: Upload the book file (PDF/EPUB) to Supabase Storage bucket 'books' with path 'files/{uuid}.{ext}'
                _uiState.value = AdminUiState.Uploading(50)
                val ext = fileType.lowercase()
                val fileBytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(bookFileUri)?.use { it.readBytes() }
                }
                if (fileBytes == null || fileBytes.isEmpty()) {
                    throw Exception("Could not retrieve book file contents.")
                }

                val bookPath = "files/$uuid.$ext"
                _uiState.value = AdminUiState.Uploading(75)
                supabaseClient.storage.from("books").upload(bookPath, fileBytes)
                val generatedBookUrl = "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/books/$bookPath"

                // Step 3: Insert metadata to supabase public.books table
                _uiState.value = AdminUiState.Uploading(90)
                val newBook = SupabaseBook(
                    id = uuid,
                    title = title,
                    author = author,
                    category = category,
                    coverImageUrl = generatedCoverUrl,
                    fileUrl = generatedBookUrl,
                    fileType = ext,
                    isPublic = true
                )
                supabaseService.publishBook(newBook)

                _uiState.value = AdminUiState.Success
                fetchTotalBooksCount()
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error(e.localizedMessage ?: "Publication failed")
            }
        }
    }

    class Factory(
        private val supabaseClient: SupabaseClient,
        private val supabaseService: SupabaseService
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminViewModel(supabaseClient, supabaseService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
