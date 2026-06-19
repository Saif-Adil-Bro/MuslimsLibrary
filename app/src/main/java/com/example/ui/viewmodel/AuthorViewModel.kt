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
import java.text.SimpleDateFormat
import java.util.Locale

data class Author(
    val id: String,           // Generated from author name
    val name: String,         // Author name from books
    val initial: String,      // First letter of name
    val booksCount: Int,      // Count of books by this author
    val books: List<SupabaseBook>,  // List of books
    val bio: String? = null   // Can be null
)

sealed interface AuthorUiState {
    object Loading : AuthorUiState
    data class Success(val authors: List<Author>) : AuthorUiState
    object Empty : AuthorUiState
    data class Error(val message: String) : AuthorUiState
}

class AuthorViewModel(
    private val supabaseService: SupabaseService
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthorUiState>(AuthorUiState.Loading)
    val uiState: StateFlow<AuthorUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("all") // "all", "popular", "recent", "most-books"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private var allAuthors = listOf<Author>()

    init {
        loadAuthors()
    }

    fun loadAuthors() {
        _uiState.value = AuthorUiState.Loading
        viewModelScope.launch {
            try {
                val books = supabaseService.fetchPublicBooks()
                allAuthors = extractAuthors(books)
                applyFiltersAndPublish()
            } catch (e: Exception) {
                _uiState.value = AuthorUiState.Error(e.localizedMessage ?: "লেখকদের ডাটা লোড করতে ব্যর্থ হয়েছে")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFiltersAndPublish()
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
        applyFiltersAndPublish()
    }

    private fun extractAuthors(books: List<SupabaseBook>): List<Author> {
        return books
            .groupBy { it.author }
            .map { (authorName, authorBooks) ->
                Author(
                    id = authorName.hashCode().toString(),
                    name = authorName,
                    initial = authorName.trim().firstOrNull()?.toString() ?: "?",
                    booksCount = authorBooks.size,
                    books = authorBooks,
                    bio = getPredefinedBio(authorName)
                )
            }
    }

    private fun applyFiltersAndPublish() {
        val query = _searchQuery.value.trim()
        val filter = _selectedFilter.value

        var filtered = if (query.isEmpty()) {
            allAuthors
        } else {
            allAuthors.filter { it.name.contains(query, ignoreCase = true) }
        }

        filtered = when (filter) {
            "popular" -> {
                filtered.sortedByDescending { it.booksCount }
            }
            "recent" -> {
                // Sort authors by the newest book date (highest timestamp first)
                filtered.sortedByDescending { author ->
                    author.books.map { book ->
                        book.createdAt?.let { parseDate(it) } ?: 0L
                    }.maxOrNull() ?: 0L
                }
            }
            "most-books" -> {
                filtered.sortedByDescending { it.booksCount }
            }
            else -> { // "all"
                filtered.sortedBy { it.name }
            }
        }

        if (filtered.isEmpty()) {
            _uiState.value = AuthorUiState.Empty
        } else {
            _uiState.value = AuthorUiState.Success(filtered)
        }
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            )
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    val date = sdf.parse(dateStr)
                    if (date != null) return date.time
                } catch (ignored: Exception) {}
            }
            0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getPredefinedBio(authorName: String): String {
        val trimmed = authorName.trim()
        return when {
            trimmed.contains("Nawawi", ignoreCase = true) || trimmed.contains("নববী", ignoreCase = true) || trimmed.contains("নব্বী", ignoreCase = true) -> {
                "ইমাম নববী (রহ.) ছিলেন একজন মহান হাদিস বিশারদ, ফকীহ ও লেখক। তাঁর বিখ্যাত সংকলন 'রিয়াদুস সালেহীন' এবং 'আরবাঈন নববী' বিশ্বজুড়ে অত্যন্ত সমাদৃত ও পঠিত।"
            }
            trimmed.contains("Taymiyyah", ignoreCase = true) || trimmed.contains("তৈমিয়া", ignoreCase = true) -> {
                "ইমাম ইবনে তাইমিয়্যাহ (রহ.) ছিলেন ইসলামের ইতিহাসের অন্যতম প্রভাবশালী চিন্তাবিদ, ফকীহ এবং সংস্কারক। তিনি ইসলামী আকীদা ও ফিকহের বহু জটিল সমস্যার সমাধান তাঁর লেখনীর মাধ্যমে উপস্থাপন করেছেন।"
            }
            trimmed.contains("Mubarakpuri", ignoreCase = true) || trimmed.contains("মুবারকপুরী", ignoreCase = true) -> {
                "আল্লামা সাফিউর রহমান মুবারকপুরী (রহ.) ছিলেন একজন খ্যাতনামা ইসলামী গবেষক ও লেখক। তাঁর রচিত 'আর্-রাহীকুল মাখতੂম' (The Sealed Nectar) রাসুলুল্লাহ (সা.)-এর সীরাতের ওপর রচিত বিশ্বসেরা ও পুরস্কৃত গ্রন্থ।"
            }
            trimmed.contains("Ashraf Ali", ignoreCase = true) || trimmed.contains("থানভী", ignoreCase = true) -> {
                "হাকীমুল উম্মত মাওলানা আশরাফ আলী থানভী (রহ.) ছিলেন ভারতীয় উপমহাদেশের একজন অনন্য সাধারণ ইসলামী পণ্ডিত, ফকীহ, মুফাসসির ও আধ্যাত্মিক সাধক।"
            }
            trimmed.contains("Ibn Rajab", ignoreCase = true) || trimmed.contains("রজব", ignoreCase = true) -> {
                "ইমাম ইবনে রজব আল-হাম্বলী (রহ.) ছিলেন হিজরী অষ্টম শতকের একজন শীর্ষস্থানীয় হাদিস বিশারদ, ফকীহ ও আল্লাহভীরু পণ্ডিত। হাদিসের ব্যাখ্যায় তাঁর চমৎকার বিশ্লেষণ ও লেখনী অতুলনীয়।"
            }
            else -> "কোনো পরিচিতি নেই"
        }
    }

    class Factory(
        private val service: SupabaseService
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthorViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthorViewModel(service) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
