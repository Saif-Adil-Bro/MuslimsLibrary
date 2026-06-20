package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseBook
import com.example.ui.components.BookCardGrid
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.theme.AppBackground
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllBooksScreen(
    categoryFilter: String? = null,
    sortBy: String = "recent",
    onBackClick: () -> Unit,
    onBookClick: (SupabaseBook) -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    // Collect reading / books from View Model
    val books = viewModel.allPublicBooks

    // Filter books based on category
    val filteredBooks = remember(books, categoryFilter) {
        if (categoryFilter == null || categoryFilter.equals("all", ignoreCase = true)) {
            books
        } else {
            books.filter { book ->
                book.category.split(",").map { it.trim() }.any { it.equals(categoryFilter, ignoreCase = true) }
            }
        }
    }

    // Sort books based on recent/popular
    val sortedBooks = remember(filteredBooks, sortBy) {
        when (sortBy) {
            "recent" -> filteredBooks.sortedByDescending { it.createdAt ?: "" }
            "popular" -> filteredBooks.sortedBy { it.title }
            else -> filteredBooks
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (categoryFilter == null || categoryFilter.equals("all", ignoreCase = true)) "সব বই" else categoryFilter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("all_books_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "უკან",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF667EEA)
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding)
        ) {
            if (sortedBooks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "এই ক্যাটেগরিতে কোনো বই নেই",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("all_books_grid"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sortedBooks, key = { it.id }) { book ->
                        BookCardGrid(
                            book = book,
                            onClick = { onBookClick(book) },
                            gradientColors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                        )
                    }
                }
            }
        }
    }
}
