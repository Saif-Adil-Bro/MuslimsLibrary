package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseBook
import com.example.ui.components.*
import com.example.ui.theme.AppBackground
import com.example.ui.theme.AppGradientEnd
import com.example.ui.theme.AppGradientStart
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.Author
import com.example.ui.viewmodel.AuthorUiState
import com.example.ui.viewmodel.AuthorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorScreen(
    viewModel: AuthorViewModel,
    onBookClick: (SupabaseBook) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var isGridView by remember { mutableStateOf(true) }
    var selectedAuthorForSheet by remember { mutableStateOf<Author?>(null) }

    val totalAuthorsCount = when (val state = uiState) {
        is AuthorUiState.Success -> state.authors.size
        else -> 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // 1. STICKY TOP HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppGradientStart, AppGradientEnd)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 15.dp, vertical = 15.dp)
                .testTag("author_header_box")
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .testTag("author_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "ফিরে যান",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "লেখকবৃন্দ",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 2. STICKY SEARCH BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 15.dp)
        ) {
            val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
            var isFocused by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        text = "লেখক খুঁজুন...",
                        color = Color.Gray,
                        fontSize = 15.sp
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "সার্চ",
                        tint = if (isFocused) AppGradientStart else Color(0xFF999999),
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .testTag("author_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFF8F9FA),
                    focusedBorderColor = AppGradientStart,
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        // 3. FILTER TABS (Horizontal Scroll)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    label = "সব লেখক",
                    isSelected = selectedFilter == "all",
                    onClick = { viewModel.onFilterSelected("all") },
                    testTag = "filter_author_all"
                )
                FilterChip(
                    label = "জনপ্রিয়",
                    isSelected = selectedFilter == "popular",
                    onClick = { viewModel.onFilterSelected("popular") },
                    testTag = "filter_author_popular"
                )
                FilterChip(
                    label = "সাম্প্রতিক",
                    isSelected = selectedFilter == "recent",
                    onClick = { viewModel.onFilterSelected("recent") },
                    testTag = "filter_author_recent"
                )
                FilterChip(
                    label = "সর্বাধিক বই",
                    isSelected = selectedFilter == "most-books",
                    onClick = { viewModel.onFilterSelected("most-books") },
                    testTag = "filter_author_most_books"
                )
            }
        }

        // 4. STATS BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "মোট লেখক: ",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = totalAuthorsCount.toString(),
                    color = AppGradientStart,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("author_stats_total_count")
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isGridView = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isGridView) AppGradientStart else Color(0xFFF0F2F5),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .testTag("author_view_grid_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Grid View",
                        tint = if (isGridView) Color.White else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { isGridView = false },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (!isGridView) AppGradientStart else Color(0xFFF0F2F5),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .testTag("author_view_list_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "List View",
                        tint = if (!isGridView) Color.White else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Authors selection lists
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (uiState) {
                is AuthorUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppGradientStart)
                    }
                }
                is AuthorUiState.Error -> {
                    val errorMsg = (uiState as AuthorUiState.Error).message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = errorMsg, color = Color.Red, fontSize = 14.sp)
                    }
                }
                is AuthorUiState.Empty -> {
                    AuthorEmptyState(modifier = Modifier.fillMaxSize())
                }
                is AuthorUiState.Success -> {
                    val authors = (uiState as AuthorUiState.Success).authors
                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(15.dp),
                            horizontalArrangement = Arrangement.spacedBy(15.dp),
                            verticalArrangement = Arrangement.spacedBy(15.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(authors) { author ->
                                AuthorCardGrid(
                                    author = author,
                                    onClick = { selectedAuthorForSheet = author }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(15.dp),
                            verticalArrangement = Arrangement.spacedBy(15.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(authors) { author ->
                                AuthorCardList(
                                    author = author,
                                    onClick = { selectedAuthorForSheet = author }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet display
    if (selectedAuthorForSheet != null) {
        AuthorDetailBottomSheet(
            author = selectedAuthorForSheet,
            onDismissRequest = { selectedAuthorForSheet = null },
            onBookClick = onBookClick
        )
    }
}
