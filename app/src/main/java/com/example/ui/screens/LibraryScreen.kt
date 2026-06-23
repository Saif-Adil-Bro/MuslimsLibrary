package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    userId: String,
    onBookClick: (String) -> Unit,
    onGoToHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Set user ID to library view model so it collects flows for this user
    LaunchedEffect(userId) {
        libraryViewModel.setUserId(userId)
    }

    val selectedTab by libraryViewModel.selectedTab.collectAsState()
    val isGridView by libraryViewModel.isGridView.collectAsState()
    val searchQuery by libraryViewModel.searchQuery.collectAsState()
    val stats by libraryViewModel.stats.collectAsState()
    val books by libraryViewModel.libraryBooks.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Compact title and layout toggle row (removes double header and reduces margin)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "লাইব্রেরী",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = { libraryViewModel.toggleViewMode() },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("library_view_toggle")
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridOn,
                    contentDescription = "Toggle View Layout",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Stats Card at top
        LibraryStatsCard(
            stats = stats,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        // Search Bar Row
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { libraryViewModel.updateSearchQuery(it) },
            placeholder = { Text("লাইব্রেরী থেকে খুঁজুন...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("library_search_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )

        // Tab Filters with horizontal scrolling
        LibraryTabFilter(
            selectedTab = selectedTab,
            onTabSelected = { libraryViewModel.selectTab(it) },
            modifier = Modifier.padding(vertical = 2.dp)
        )

            // Main Book Display list/grid or empty state
            if (books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyMsg = when (selectedTab) {
                        LibraryViewModel.LibraryTab.DOWNLOADS -> "কোন ডাউনলোড করা বই পাওয়া যায়নি"
                        LibraryViewModel.LibraryTab.FAVORITES -> "আপনার পছন্দের তালিকায় কোন বই নেই"
                        LibraryViewModel.LibraryTab.READING -> "আপনি বর্তমানে কোন বই পড়ছেন না"
                        LibraryViewModel.LibraryTab.COMPLETED -> "আপনার সম্পূর্ণ পড়া বইয়ের তালিকা খালি"
                        LibraryViewModel.LibraryTab.PINNED -> "পিন করার সামগ্রী নেই"
                        else -> "আপনার লাইব্রেরী খালি"
                    }
                    LibraryEmptyState(
                        message = emptyMsg,
                        onButtonClick = onGoToHomeClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("library_books_grid"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = books,
                            key = { it.bookId }
                        ) { book ->
                            LibraryBookCard(
                                book = book,
                                onClick = { onBookClick(book.bookId) },
                                onMenuClick = { action ->
                                    when (action) {
                                        "read" -> onBookClick(book.bookId)
                                        "favorite" -> libraryViewModel.toggleFavorite(book.bookId)
                                        "pin" -> libraryViewModel.togglePin(book.bookId)
                                        "delete_download" -> libraryViewModel.deleteDownload(book.bookId)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("library_books_list"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = books,
                            key = { it.bookId }
                        ) { book ->
                            LibraryBookListItem(
                                book = book,
                                onClick = { onBookClick(book.bookId) },
                                onMenuClick = { action ->
                                    when (action) {
                                        "read" -> onBookClick(book.bookId)
                                        "favorite" -> libraryViewModel.toggleFavorite(book.bookId)
                                        "pin" -> libraryViewModel.togglePin(book.bookId)
                                        "delete_download" -> libraryViewModel.deleteDownload(book.bookId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
}
