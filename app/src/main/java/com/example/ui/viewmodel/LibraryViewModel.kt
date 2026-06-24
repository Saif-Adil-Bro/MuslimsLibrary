package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SupabaseBook
import com.example.data.SupabaseService
import com.example.data.local.AppDatabase
import com.example.data.local.entities.DownloadedBook
import com.example.data.local.entities.LocalBookProgress
import com.example.data.local.entities.LocalFavoriteBook
import com.example.data.local.entities.LocalPinnedBook
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val appDatabase: AppDatabase,
    private val supabaseService: SupabaseService
) : ViewModel() {

    // Current UserId holder
    private val _userId = MutableStateFlow<String>("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    // Tab selection
    private val _selectedTab = MutableStateFlow(LibraryTab.ALL)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    // View mode (grid/list)
    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Online books cache catalog
    private val _publicBooks = MutableStateFlow<List<SupabaseBook>>(emptyList())

    enum class LibraryTab {
        ALL, DOWNLOADS, FAVORITES, READING, COMPLETED, PINNED
    }

    data class LibraryStats(
        val totalBooks: Int = 0,
        val completedBooks: Int = 0,
        val readingBooks: Int = 0,
        val favoriteBooks: Int = 0
    )

    data class LibraryBook(
        val bookId: String,
        val title: String,
        val author: String,
        val coverUrl: String?,
        val status: String, // "reading", "completed", "downloaded", "none"
        val progress: Int, // 0-100
        val isFavorite: Boolean,
        val isPinned: Boolean,
        val isDownloaded: Boolean,
        val lastReadAt: Long?
    )

    // Intermediate holder for all local user's library interactions
    data class UserLibraryData(
        val progressList: List<LocalBookProgress> = emptyList(),
        val favoritesList: List<LocalFavoriteBook> = emptyList(),
        val pinsList: List<LocalPinnedBook> = emptyList(),
        val downloadsList: List<DownloadedBook> = emptyList()
    )

    init {
        loadOnlineBooksCatalog()
    }

    fun setUserId(id: String) {
        if (_userId.value != id) {
            _userId.value = id
        }
    }

    private var lastLoadTime = 0L
    private val COOLDOWN_MS = 60_000L // ৬০ সেকেন্ড

    fun loadOnlineBooksCatalog(forceRefresh: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (!forceRefresh && (currentTime - lastLoadTime < COOLDOWN_MS) && _publicBooks.value.isNotEmpty()) {
            android.util.Log.d("LibraryViewModel", "Skipping load, data is fresh")
            return
        }

        viewModelScope.launch {
            try {
                val books = supabaseService.fetchPublicBooks()
                _publicBooks.value = books
                lastLoadTime = System.currentTimeMillis()
            } catch (e: Exception) {
                // Keep empty, local or download fallbacks will cover
            }
        }
    }

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Step 1: Combine only local Room data streams reactively
    private val userLibraryDataFlow: Flow<UserLibraryData> = _userId.flatMapLatest { uid ->
        if (uid.isEmpty()) {
            flowOf(UserLibraryData())
        } else {
            combine(
                appDatabase.progressDao().getAllProgressFlow(uid),
                appDatabase.favoriteDao().getFavoritesFlow(uid),
                appDatabase.pinDao().getPinnedBooksFlow(uid),
                appDatabase.downloadDao().getAllDownloadedBooksFlow()
            ) { progress, favorites, pins, downloads ->
                UserLibraryData(
                    progressList = progress,
                    favoritesList = favorites,
                    pinsList = pins,
                    downloadsList = downloads
                )
            }
        }
    }

    // Step 2: Combine filters and catalogs with the grouped User Library Data
    val libraryBooks: StateFlow<List<LibraryBook>> = combine(
        _selectedTab,
        _searchQuery,
        _publicBooks,
        userLibraryDataFlow
    ) { tab, query, publicBooks, userData ->
        val progressMap = userData.progressList.associateBy { it.bookId }
        val favoritesSet = userData.favoritesList.map { it.bookId }.toSet()
        val pinsSet = userData.pinsList.map { it.bookId }.toSet()
        val downloadsMap = userData.downloadsList.associateBy { it.bookId }

        // Take the absolute union of all personal library book IDs
        val allLibraryBookIds = (progressMap.keys + favoritesSet + pinsSet + downloadsMap.keys)
        val publicBooksMap = publicBooks.associateBy { it.id }

        val books = allLibraryBookIds.map { bookId ->
            val publicBook = publicBooksMap[bookId]
            val downloadedBook = downloadsMap[bookId]

            val title = publicBook?.title ?: downloadedBook?.title ?: "অজানা বই"
            val author = publicBook?.author ?: downloadedBook?.author ?: "অজানা লেখক"
            val coverUrl = publicBook?.coverImageUrl ?: downloadedBook?.coverImageUrl

            val progressEntity = progressMap[bookId]
            val progressPercentage = progressEntity?.progressPercentage?.toInt() ?: 0
            val isDownloaded = downloadedBook != null

            val status = when {
                progressEntity?.status == "completed" -> "completed"
                progressEntity?.status == "reading" -> "reading"
                isDownloaded -> "downloaded"
                else -> "none"
            }

            LibraryBook(
                bookId = bookId,
                title = title,
                author = author,
                coverUrl = coverUrl,
                status = status,
                progress = progressPercentage,
                isFavorite = bookId in favoritesSet,
                isPinned = bookId in pinsSet,
                isDownloaded = isDownloaded,
                lastReadAt = progressEntity?.lastReadAt ?: downloadedBook?.downloadDate
            )
        }

        // Filter based on currently selected tab
        val tabFiltered = when (tab) {
            LibraryTab.ALL -> books
            LibraryTab.DOWNLOADS -> books.filter { it.isDownloaded }
            LibraryTab.FAVORITES -> books.filter { it.isFavorite }
            LibraryTab.READING -> books.filter { it.status == "reading" || (it.progress > 0 && it.status != "completed") }
            LibraryTab.COMPLETED -> books.filter { it.status == "completed" || it.progress >= 100 }
            LibraryTab.PINNED -> books.filter { it.isPinned }
        }

        // Filter based on search query
        val searchFiltered = if (query.isNotBlank()) {
            tabFiltered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true)
            }
        } else {
            tabFiltered
        }

        // Sort pinned items to the top, then by last read / downloaded timestamp desc
        searchFiltered.sortedWith(
            compareByDescending<LibraryBook> { it.isPinned }
                .thenByDescending { it.lastReadAt ?: 0L }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactively compute library stats card numbers by combining flows
    val stats: StateFlow<LibraryStats> = _userId.flatMapLatest { uid ->
        if (uid.isEmpty()) {
            flowOf(LibraryStats())
        } else {
            combine(
                appDatabase.progressDao().getAllProgressFlow(uid),
                appDatabase.favoriteDao().getFavoritesFlow(uid),
                appDatabase.downloadDao().getAllDownloadedBooksFlow()
            ) { progressList, favoritesList, downloadsList ->
                val reading = progressList.count { it.status == "reading" || (it.progressPercentage > 0.0 && it.progressPercentage < 100.0) }
                val completed = progressList.count { it.status == "completed" || it.progressPercentage >= 100.0 }
                LibraryStats(
                    totalBooks = (progressList.map { it.bookId } + favoritesList.map { it.bookId } + downloadsList.map { it.bookId }).distinct().size,
                    completedBooks = completed,
                    readingBooks = reading,
                    favoriteBooks = favoritesList.size
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryStats())

    // Database manipulation utilities
    fun toggleFavorite(bookId: String) {
        val uid = _userId.value
        if (uid.isEmpty()) return
        viewModelScope.launch {
            try {
                val isFav = appDatabase.favoriteDao().isFavoriteOneShot(uid, bookId)
                if (isFav) {
                    appDatabase.favoriteDao().deleteFavoriteLocally(uid, bookId)
                } else {
                    appDatabase.favoriteDao().insertFavorite(
                        LocalFavoriteBook(
                            id = "${uid}_${bookId}",
                            userId = uid,
                            bookId = bookId,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun togglePin(bookId: String) {
        val uid = _userId.value
        if (uid.isEmpty()) return
        viewModelScope.launch {
            try {
                val isPin = appDatabase.pinDao().isPinnedOneShot(uid, bookId)
                if (isPin) {
                    appDatabase.pinDao().unpinLocally(uid, bookId)
                } else {
                    appDatabase.pinDao().insertPin(
                        LocalPinnedBook(
                            id = "${uid}_${bookId}",
                            userId = uid,
                            bookId = bookId,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun deleteDownload(bookId: String) {
        viewModelScope.launch {
            try {
                appDatabase.downloadDao().deleteDownloadedBook(bookId)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    // Required factory implementation matching our new signature
    class Factory(
        private val appDatabase: AppDatabase,
        private val supabaseService: SupabaseService
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LibraryViewModel(appDatabase, supabaseService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
