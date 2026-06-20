package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseBook
import com.example.ui.components.BookCardGrid
import com.example.ui.components.BookCardHorizontal
import com.example.ui.components.CategoryPill
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    userEmail: String,
    role: String,
    onBookClick: (SupabaseBook) -> Unit,
    onNavigateToAllBooks: (sortBy: String, categoryFilter: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val selectedCategory by homeViewModel.selectedCategory.collectAsState()
    val categories by homeViewModel.categories.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.refreshBooks()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // 1. Sticky Category Pills scrollable row (ALWAYS visible at the top below primary header!)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 12.dp)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { cat ->
                    CategoryPill(
                        categoryName = cat,
                        isSelected = selectedCategory == cat,
                        onClick = { homeViewModel.onCategorySelected(cat) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Content area with Pull To Refresh
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            PullToRefreshBox(
                isRefreshing = uiState is HomeUiState.Loading,
                onRefresh = { homeViewModel.refreshBooks() },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF667EEA))
                        }
                    }
                    is HomeUiState.Empty -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyPlaceholder(
                                title = "কোনো বই পাওয়া যায়নি",
                                message = "দুঃখিত, এই ক্যাটাগরিতে এই মুহূর্তে কোনো বই পাওয়া যায়নি।"
                            )
                        }
                    }
                    is HomeUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚠️", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("সংযোগ ব্যাহত হয়েছে", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.message, color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { homeViewModel.refreshBooks() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA))
                                ) {
                                    Text("পুনরায় চেষ্টা করুন", color = Color.White)
                                }
                            }
                        }
                    }
                    is HomeUiState.Success -> {
                        val books = state.books
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            // 1. Recent Books - Horizontal Section
                            if (books.isNotEmpty()) {
                                item {
                                    RecentBooksHeader(onViewAllClick = {
                                        onNavigateToAllBooks("recent", "All")
                                    })
                                    
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Transparent),
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                                    ) {
                                        val recentBooks = books.take(8)
                                        items(recentBooks) { book ->
                                            BookCardHorizontal(
                                                book = book,
                                                onClick = { onBookClick(book) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }

                            // 2. Dynamic Category Sections
                            val categoriesWithBooks = books.flatMap { it.category.split(",") }.map { it.trim() }.distinct().filter { it.isNotBlank() }
                            categoriesWithBooks.forEach { category ->
                                val categoryBooks = books.filter { book ->
                                    book.category.split(",").map { it.trim() }.any { it.equals(category, ignoreCase = true) }
                                }
                                if (categoryBooks.isNotEmpty()) {
                                    item {
                                        CategorySectionHeader(
                                            categoryName = category,
                                            onViewAllClick = {
                                                onNavigateToAllBooks("recent", category)
                                            }
                                        )
                                        
                                        LazyRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Transparent),
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.spacedBy(15.dp)
                                        ) {
                                            items(categoryBooks) { book ->
                                                BookCardHorizontal(
                                                    book = book,
                                                    onClick = { onBookClick(book) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
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
fun RecentBooksHeader(
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = Color(0xFF667EEA),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "সাম্প্রতিক বইসমূহ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748)
            )
        }

        Text(
            text = "সব দেখুন",
            color = Color(0xFF667EEA),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onViewAllClick)
        )
    }
}

@Composable
fun CategorySectionHeader(
    categoryName: String,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = null,
                tint = Color(0xFF667EEA),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = categoryName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748)
            )
        }

        Text(
            text = "সব দেখুন",
            color = Color(0xFF667EEA),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onViewAllClick)
        )
    }
}
