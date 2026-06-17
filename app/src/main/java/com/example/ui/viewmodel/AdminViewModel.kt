package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SupabaseService
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UploadState {
    object Idle : UploadState
    data class Uploading(val progress: Int) : UploadState
    object Success : UploadState
    data class Error(val message: String) : UploadState
}

enum class SourceType {
    FILE, URL, GDRIVE, CDN
}

data class BookUploadData(
    val title: String,
    val author: String,
    val category: String,
    val fileType: String,
    val coverSourceType: SourceType, // FILE or URL
    val coverImageUri: Uri? = null,
    val coverImageUrl: String? = null,
    val bookSourceType: SourceType, // FILE, GDRIVE, or CDN
    val bookFileUri: Uri? = null,
    val bookFileUrl: String? = null
)

class AdminViewModel(
    private val supabaseClient: SupabaseClient,
    val supabaseService: SupabaseService
) : ViewModel() {

    private val _uiState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uiState: StateFlow<UploadState> = _uiState.asStateFlow()

    private val _totalBooksCount = MutableStateFlow(0)
    val totalBooksCount: StateFlow<Int> = _totalBooksCount.asStateFlow()

    init {
        getTotalBooksCount()
    }

    fun getTotalBooksCount() {
        viewModelScope.launch {
            try {
                val count = supabaseService.getBooksCount()
                _totalBooksCount.value = count
            } catch (e: Exception) {
                _totalBooksCount.value = 0
            }
        }
    }

    suspend fun getUserRole(uid: String): String {
        return try {
            val userProfile = supabaseService.getCurrentUserProfile(uid)
            userProfile?.role ?: "user"
        } catch (e: Exception) {
            "user"
        }
    }

    fun resetState() {
        _uiState.value = UploadState.Idle
    }

    fun uploadBook(data: BookUploadData) {
        _uiState.value = UploadState.Uploading(10)
        android.util.Log.d("AdminViewModel", "Starting upload with data: $data")
        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val uuid = UUID.randomUUID().toString()
                val ext = data.fileType.lowercase()

                // 1. Resolve Cover Image URL
                _uiState.value = UploadState.Uploading(30)
                val coverUrl = if (data.coverSourceType == SourceType.FILE) {
                    if (data.coverImageUri != null) {
                        android.util.Log.d("AdminViewModel", "Uploading cover image file...")
                        val coverPath = "covers/${timestamp}_$uuid.jpg"
                        supabaseService.uploadToStorage("books", coverPath, data.coverImageUri)
                    } else {
                        null
                    }
                } else {
                    if (!data.coverImageUrl.isNullOrBlank()) {
                        android.util.Log.d("AdminViewModel", "Using cover image URL direct: ${data.coverImageUrl}")
                        data.coverImageUrl
                    } else {
                        null
                    }
                }

                // 2. Resolve Book File URL
                _uiState.value = UploadState.Uploading(60)
                val bookUrl = when (data.bookSourceType) {
                    SourceType.FILE -> {
                        if (data.bookFileUri != null) {
                            android.util.Log.d("AdminViewModel", "Uploading book file to Supabase storage...")
                            val bookPath = "files/${timestamp}_$uuid.$ext"
                            supabaseService.uploadToStorage("books", bookPath, data.bookFileUri)
                        } else {
                            throw Exception("Missing file for direct device upload")
                        }
                    }
                    SourceType.GDRIVE -> {
                        val rawUrl = data.bookFileUrl ?: throw Exception("Google Drive link is empty")
                        val converted = supabaseService.convertGoogleDriveLink(rawUrl)
                        android.util.Log.d("AdminViewModel", "Converting GDrive link: $rawUrl -> $converted")
                        converted
                    }
                    SourceType.CDN -> {
                        val rawUrl = data.bookFileUrl ?: throw Exception("CDN file URL is empty")
                        android.util.Log.d("AdminViewModel", "Using CDN URL direct: $rawUrl")
                        rawUrl
                    }
                    else -> throw Exception("Unknown source type")
                }

                // 3. Insert metadata into public.books table
                _uiState.value = UploadState.Uploading(90)
                android.util.Log.d("AdminViewModel", "Inserting book into database: $bookUrl")
                val bookData = mapOf(
                    "id" to uuid,
                    "title" to data.title,
                    "author" to data.author,
                    "category" to data.category,
                    "cover_image_url" to coverUrl,
                    "file_url" to bookUrl,
                    "file_type" to ext,
                    "is_public" to true
                )
                supabaseService.insertBook(bookData)

                _uiState.value = UploadState.Success
                getTotalBooksCount()
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Upload failed", e)
                _uiState.value = UploadState.Error(e.localizedMessage ?: "Publication failed")
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
