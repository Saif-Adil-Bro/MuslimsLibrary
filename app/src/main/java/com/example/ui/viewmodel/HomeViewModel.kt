package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SupabaseBook
import com.example.data.SupabaseService
import com.example.data.repository.LocalSyncRepository
import com.example.data.local.entities.LocalBookProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val books: List<SupabaseBook>) : HomeUiState
    object Empty : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val supabaseService: SupabaseService,
    val localSyncRepository: LocalSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _readingProgress = MutableStateFlow<List<LocalBookProgress>>(emptyList())
    val readingProgress: StateFlow<List<LocalBookProgress>> = _readingProgress.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("সব")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val allBooks = mutableListOf<SupabaseBook>()
    val allPublicBooks: List<SupabaseBook> get() = allBooks.toList()

    val categories = listOf("সব", "কুরআন", "হাদিস", "ফিকহ", "তাফসীর", "সীরাত", "অন্যান্য")

    init {
        loadBooks()
    }

    fun loadBooks() {
        fetchBooks()
    }

    fun fetchBooks() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                val books = supabaseService.fetchPublicBooks()
                allBooks.clear()
                allBooks.addAll(books)
                filterAndPublish()
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Failed to fetch books from Supabase")
            }
        }
    }

    fun refreshBooks() {
        fetchBooks()
    }

    fun loadReadingProgress(userId: String) {
        viewModelScope.launch {
            try {
                localSyncRepository.getAllProgressFlow(userId).collect { list ->
                    _readingProgress.value = list.filter { it.status == "reading" }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading user book progress: ${e.message}")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterAndPublish()
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        filterAndPublish()
    }

    // Exposed function to allow filtering custom input lists or querying filtered result programmatically
    fun filteredBooks(query: String, category: String): List<SupabaseBook> {
        return allBooks.filter { book ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                book.title.contains(query, ignoreCase = true) ||
                book.author.contains(query, ignoreCase = true)
            }
            
            val matchesCategory = if (category == "সব") {
                true
            } else {
                book.category.split(",").map { it.trim() }.any { it.equals(category, ignoreCase = true) }
            }
            
            matchesQuery && matchesCategory
        }
    }

    private fun filterAndPublish() {
        val filtered = filteredBooks(_searchQuery.value, _selectedCategory.value)
        if (filtered.isEmpty()) {
            _uiState.value = HomeUiState.Empty
        } else {
            _uiState.value = HomeUiState.Success(filtered)
        }
    }

    class Factory(
        private val service: SupabaseService,
        private val repository: LocalSyncRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(service, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
