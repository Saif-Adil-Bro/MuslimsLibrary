package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SupabaseService
import com.example.data.SupabaseAuthor
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private val _adminBooks = MutableStateFlow<List<com.example.data.SupabaseBook>>(emptyList())
    val adminBooks: StateFlow<List<com.example.data.SupabaseBook>> = _adminBooks.asStateFlow()

    private val _adminAuthors = MutableStateFlow<List<com.example.data.SupabaseAuthor>>(emptyList())
    val adminAuthors: StateFlow<List<com.example.data.SupabaseAuthor>> = _adminAuthors.asStateFlow()

    private val _adminCategories = MutableStateFlow<List<com.example.data.SupabaseCategory>>(emptyList())
    val adminCategories: StateFlow<List<com.example.data.SupabaseCategory>> = _adminCategories.asStateFlow()

    private val localCategoriesOverride = mutableListOf<com.example.data.SupabaseCategory>()

    fun loadAdminData() {
        viewModelScope.launch {
            try {
                _adminBooks.value = supabaseService.fetchPublicBooks().sortedBy { it.title }
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error loading admin books: ${e.message}")
            }
            try {
                _adminAuthors.value = supabaseService.getAllAuthors()
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error loading admin authors: ${e.message}")
            }
            try {
                val dbCategories = supabaseService.getCategories()
                val merged = mutableListOf<com.example.data.SupabaseCategory>()
                
                dbCategories.forEach { dbCat ->
                    val override = localCategoriesOverride.find { it.id == dbCat.id || it.name.trim().lowercase() == dbCat.name.trim().lowercase() }
                    if (override != null) {
                        if (!merged.any { it.name.trim().lowercase() == override.name.trim().lowercase() }) {
                            merged.add(override)
                        }
                    } else {
                        if (!merged.any { it.name.trim().lowercase() == dbCat.name.trim().lowercase() }) {
                            merged.add(dbCat)
                        }
                    }
                }
                
                localCategoriesOverride.forEach { locCat ->
                    if (!merged.any { it.id == locCat.id || it.name.trim().lowercase() == locCat.name.trim().lowercase() }) {
                        merged.add(locCat)
                    }
                }
                _adminCategories.value = merged.sortedBy { it.name }
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error loading admin categories: ${e.message}")
            }
        }
    }

    fun updateBook(id: String, title: String, author: String, category: String, coverImageUrl: String?, fileUrl: String?) {
        viewModelScope.launch {
            supabaseService.updateBook(id, title, author, category, coverImageUrl, fileUrl)
            loadAdminData()
            getTotalBooksCount()
        }
    }

    fun updateBookDetailed(id: String, data: BookUploadData, authorBio: String? = null) {
        _uiState.value = UploadState.Uploading(10)
        android.util.Log.d("AdminViewModel", "Starting update for $id with data: $data")
        viewModelScope.launch {
            try {
                // Ensure the author exists first
                ensureAuthorExists(data.author, authorBio)

                val timestamp = System.currentTimeMillis()
                val ext = data.fileType.lowercase()

                // 1. Resolve Cover Image URL
                _uiState.value = UploadState.Uploading(30)
                val coverUrl = if (data.coverSourceType == SourceType.FILE) {
                    if (data.coverImageUri != null) {
                        android.util.Log.d("AdminViewModel", "Uploading new cover image file...")
                        val coverPath = "covers/${timestamp}_$id.jpg"
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
                            android.util.Log.d("AdminViewModel", "Uploading new book file to Supabase storage...")
                            val bookPath = "files/${timestamp}_$id.$ext"
                            supabaseService.uploadToStorage("books", bookPath, data.bookFileUri)
                        } else {
                            null
                        }
                    }
                    SourceType.GDRIVE -> {
                        val rawUrl = data.bookFileUrl
                        if (!rawUrl.isNullOrBlank()) {
                            val converted = supabaseService.convertGoogleDriveLink(rawUrl)
                            android.util.Log.d("AdminViewModel", "Converting GDrive link: $rawUrl -> $converted")
                            converted
                        } else {
                            null
                        }
                    }
                    SourceType.CDN -> {
                        val rawUrl = data.bookFileUrl
                        if (!rawUrl.isNullOrBlank()) {
                            android.util.Log.d("AdminViewModel", "Using CDN URL direct: $rawUrl")
                            rawUrl
                        } else {
                            null
                        }
                    }
                    else -> null
                }

                // 3. Update metadata in public.books table
                _uiState.value = UploadState.Uploading(90)
                android.util.Log.d("AdminViewModel", "Updating book $id in database: coverUrl=$coverUrl, bookUrl=$bookUrl")
                supabaseService.updateBook(
                    id = id,
                    title = data.title,
                    author = data.author,
                    category = data.category,
                    coverImageUrl = coverUrl,
                    fileUrl = bookUrl,
                    fileType = ext
                )

                _uiState.value = UploadState.Success
                loadAdminData()
                getTotalBooksCount()
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Update failed", e)
                _uiState.value = UploadState.Error(e.localizedMessage ?: "Update failed")
            }
        }
    }

    fun deleteBook(id: String) {
        viewModelScope.launch {
            supabaseService.deleteBook(id)
            loadAdminData()
            getTotalBooksCount()
        }
    }

    fun saveAuthor(name: String, bio: String?) {
        viewModelScope.launch {
            supabaseService.upsertAuthor(name, bio)
            loadAdminData()
        }
    }

    fun deleteAuthor(id: String) {
        viewModelScope.launch {
            supabaseService.deleteAuthor(id)
            loadAdminData()
        }
    }

    fun saveCategory(id: String?, name: String) {
        viewModelScope.launch {
            if (id == null) {
                val newId = "local_${java.util.UUID.randomUUID()}"
                val newCat = com.example.data.SupabaseCategory(id = newId, name = name)
                localCategoriesOverride.add(newCat)
                supabaseService.addCategory(name)
            } else {
                val index = localCategoriesOverride.indexOfFirst { it.id == id }
                if (index != -1) {
                    localCategoriesOverride[index] = localCategoriesOverride[index].copy(name = name)
                } else {
                    localCategoriesOverride.add(com.example.data.SupabaseCategory(id = id, name = name))
                }
                if (!id.startsWith("local_")) {
                    supabaseService.updateCategory(id, name)
                }
            }
            loadAdminData()
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            localCategoriesOverride.removeAll { it.id == id }
            if (!id.startsWith("local_")) {
                supabaseService.deleteCategory(id)
            }
            loadAdminData()
        }
    }

    // NEW: Author suggestion states
    private val _authorSuggestions = MutableStateFlow<List<AuthorSuggestion>>(emptyList())
    val authorSuggestions: StateFlow<List<AuthorSuggestion>> = _authorSuggestions.asStateFlow()

    private val _showAuthorSuggestions = MutableStateFlow(false)
    val showAuthorSuggestions: StateFlow<Boolean> = _showAuthorSuggestions.asStateFlow()

    data class AuthorSuggestion(
        val author: SupabaseAuthor,
        val bookCount: Int
    )

    private var searchJob: Job? = null

    init {
        getTotalBooksCount()
    }

    fun onAuthorNameChanged(query: String) {
        searchJob?.cancel()

        if (query.length < 2) {
            _authorSuggestions.value = emptyList()
            _showAuthorSuggestions.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            try {
                val authors = supabaseService.searchAuthors(query, limit = 5)
                val suggestions = withContext(Dispatchers.IO) {
                    authors.map { author ->
                        async {
                            val bookCount = supabaseService.getAuthorBookCount(author.name)
                            AuthorSuggestion(author, bookCount)
                        }
                    }.awaitAll()
                }
                _authorSuggestions.value = suggestions
                _showAuthorSuggestions.value = true
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error searching authors: ${e.message}")
                _authorSuggestions.value = emptyList()
                _showAuthorSuggestions.value = true
            }
        }
    }

    fun selectAuthor(author: SupabaseAuthor) {
        _showAuthorSuggestions.value = false
        _authorSuggestions.value = emptyList()
    }

    fun hideAuthorSuggestions() {
        _showAuthorSuggestions.value = false
    }

    suspend fun ensureAuthorExists(authorName: String, authorBio: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val exists = supabaseService.checkAuthorExistsInAuthorsTable(authorName)
                if (!exists) {
                    supabaseService.addAuthor(authorName, authorBio)
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error ensuring author exists: ${e.message}")
                false
            }
        }
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

    fun uploadBook(data: BookUploadData, authorBio: String? = null) {
        _uiState.value = UploadState.Uploading(10)
        android.util.Log.d("AdminViewModel", "Starting upload with data: $data")
        viewModelScope.launch {
            try {
                // Ensure the author exists first
                ensureAuthorExists(data.author, authorBio)

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
