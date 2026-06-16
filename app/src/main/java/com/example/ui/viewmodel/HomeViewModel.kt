package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SupabaseBook
import com.example.data.SupabaseService
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
    private val supabaseService: SupabaseService
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val allBooks = mutableListOf<SupabaseBook>()

    init {
        loadBooks()
    }

    fun loadBooks() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                val books = supabaseService.fetchPublicBooks()
                allBooks.clear()
                allBooks.addAll(books)
                filterAndPublish()
            } catch (e: Exception) {
                // If it fails (e.g. database schema has no books yet or network offline), return Error state
                // We also handle empty state cleanly if fetch succeeded but returned no entries.
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Failed to fetch books from Supabase")
            }
        }
    }

    fun refreshBooks() {
        loadBooks()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterAndPublish()
    }

    private fun filterAndPublish() {
        val query = _searchQuery.value
        val filtered = if (query.isBlank()) {
            allBooks.toList()
        } else {
            allBooks.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            _uiState.value = HomeUiState.Empty
        } else {
            _uiState.value = HomeUiState.Success(filtered)
        }
    }

    class Factory(private val service: SupabaseService) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(service) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
