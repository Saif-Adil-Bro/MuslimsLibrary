package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Book
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    object Loading : LibraryUiState
    data class Success(val books: List<Book>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

class LibraryViewModel(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val allAvailableBooks = mutableListOf<Book>()

    init {
        loadBooks()
    }

    fun loadBooks() {
        _uiState.value = LibraryUiState.Loading
        viewModelScope.launch {
            try {
                bookRepository.getBooks().collectLatest { books ->
                    allAvailableBooks.clear()
                    allAvailableBooks.addAll(books)
                    filterAndPublish()
                }
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(e.localizedMessage ?: "Failed to load library books")
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

    private fun filterAndPublish() {
        var filteredList = allAvailableBooks.toList()
        
        val category = _selectedCategory.value
        if (category != "All") {
            filteredList = filteredList.filter { it.category.equals(category, ignoreCase = true) }
        }

        val query = _searchQuery.value
        if (query.isNotBlank()) {
            filteredList = filteredList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true)
            }
        }

        _uiState.value = LibraryUiState.Success(filteredList)
    }

    class Factory(private val repository: BookRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LibraryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
