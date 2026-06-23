package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseBook
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onBookClick: (SupabaseBook) -> Unit,
    onCategoryClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // State to hold selected category when user clicks a category block internally
    var currentCategoryName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentCategoryName ?: "ক্যাটাগরি",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentCategoryName != null) {
                            currentCategoryName = null // Go back to category list
                        } else {
                            onBackClick() // Exit screen
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is HomeUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(text = "ত্রুটি: ${state.message}", color = Color.Gray)
                    }
                }
                is HomeUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        EmptyPlaceholder(
                            title = "কোনো বই পাওয়া যায়নি",
                            message = "দুঃখিত, কোনো বই বা ক্যাটাগরি পাওয়া যায়নি।"
                        )
                    }
                }
                is HomeUiState.Success -> {
                    val books = state.books
                    val selectedCategory = currentCategoryName
                    
                    if (selectedCategory == null) {
                        // Display categories
                        val categoriesWithBooks = books.flatMap { it.category.split(",") }
                            .map { it.trim() }
                            .distinct()
                            .filter { it.isNotBlank() }
                            .sorted()

                        if (categoriesWithBooks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                EmptyPlaceholder(
                                    title = "ক্যাটাগরি খালি",
                                    message = "কোনো ক্যাটাগরি তৈরি করা হয়নি।"
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(categoriesWithBooks) { category ->
                                    val count = books.count { book -> 
                                        book.category.split(",").map { it.trim() }.any { it.equals(category, ignoreCase = true) } 
                                    }
                                    CategoryCard(
                                        name = category,
                                        count = count,
                                        onClick = { 
                                            currentCategoryName = category
                                            onCategoryClick(category)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Display books in a specific category
                        val categoryBooks = books.filter { book ->
                            book.category.split(",").map { it.trim() }.any { it.equals(selectedCategory, ignoreCase = true) }
                        }
                        
                        if (categoryBooks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                EmptyPlaceholder(
                                    title = "বই পাওয়া যায়নি",
                                    message = "এই ক্যাটাগরিতে কোনো বই নেই।"
                                )
                            }
                        } else {
                            val chunkedBooks = categoryBooks.chunked(2)
                           LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(chunkedBooks) { rowBooks ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        for (book in rowBooks) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                com.example.ui.components.BookCardGrid(
                                                    book = book,
                                                    onClick = { onBookClick(book) },
                                                    gradientColors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                                )
                                            }
                                        }
                                        if (rowBooks.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    name: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$count টি বই",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


