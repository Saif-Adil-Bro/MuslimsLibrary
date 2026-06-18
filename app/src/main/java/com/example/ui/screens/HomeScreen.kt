package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseBook
import com.example.data.local.entities.LocalBookProgress
import com.example.ui.components.BookCard
import com.example.ui.components.CategoryFilter
import com.example.ui.components.SearchBar
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.ArrowForward

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    userEmail: String,
    role: String,
    onLogoutClick: () -> Unit,
    onBookClick: (SupabaseBook) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onSwitchToAdminClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onHeaderClick: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val selectedCategory by homeViewModel.selectedCategory.collectAsState()
    val readingProgress by homeViewModel.readingProgress.collectAsState()

    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) {
            homeViewModel.loadReadingProgress(userEmail)
        }
    }

    var headerTapCount by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFCFDF9)) // Warm organic bone-white base
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // App Bar & Profile Header with Emerald/Teal gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF043B2B), Color(0xFF0A4E38))
                    )
                )
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    headerTapCount++
                    if (headerTapCount >= 5) {
                        onHeaderClick()
                        headerTapCount = 0
                    }
                }
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Assalamu Alaikum",
                            fontSize = 14.sp,
                            color = Color(0xFFA3E2C9),
                            fontWeight = FontWeight.Medium
                        )
                        if (role.lowercase() == "admin") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFD4AF37), // Golden accent
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "ADMIN",
                                    color = Color(0xFF043B2B),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = userEmail.split("@").first().replaceFirstChar { it.uppercase() },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Serif
                    )
                    if (role.lowercase() == "admin" && onSwitchToAdminClick != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = onSwitchToAdminClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFA3E2C9)),
                            modifier = Modifier.testTag("switch_to_admin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Switch to Admin View",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch to Admin View", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Manual Refresh trigger
                    IconButton(
                        onClick = { homeViewModel.refreshBooks() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh books listing",
                            tint = Color.White
                        )
                    }

                    // Profile Button
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .testTag("profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "My Profile",
                            tint = Color.White
                        )
                    }

                    // Logout Button
                    IconButton(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout account",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Animated Search & Title Row
        SearchBar(
            query = searchQuery,
            onQueryChanged = { homeViewModel.onSearchQueryChanged(it) },
            placeholderHint = "বই বা লেখক খুঁজুন...",
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Continue Reading List Section
        ContinueReadingSection(
            progressList = readingProgress,
            allBooks = homeViewModel.allPublicBooks,
            onResumeClick = onBookClick
        )

        // Downloaded Books Shortcut Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
                .clickable { onNavigateToDownloads() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF043B2B).copy(alpha = 0.06f)),
            border = BorderStroke(1.dp, Color(0xFF043B2B).copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF043B2B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Downloads",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "আমার ডাউনলোডসমূহ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF043B2B)
                        )
                        Text(
                            text = "অফলাইনে পড়ার জন্য ডাউনলোডকৃত বইগুলো দেখুন",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF043B2B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Category Filter scrollable row
        CategoryFilter(
            categories = homeViewModel.categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { homeViewModel.onCategorySelected(it) },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Pull to Refresh container encompassing the states
        PullToRefreshBox(
            isRefreshing = uiState is HomeUiState.Loading,
            onRefresh = { homeViewModel.refreshBooks() },
            state = rememberPullToRefreshState(),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    // Shimmer loading card grid list
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(6) {
                            ShimmerBookItem()
                        }
                    }
                }
                is HomeUiState.Empty -> {
                    // Beautiful empty state illustration/placeholder using custom Bengali string
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📚",
                                fontSize = 64.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "কোনো বই পাওয়া যায়নি",
                                color = Color(0xFF032B1D),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "দুঃখিত, আপনার ফিল্টার বা অনুসন্ধানের জন্য কোনো ইসলামিক বই খুঁজে পাওয়া যায়নি।",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
                is HomeUiState.Error -> {
                    // Connection error / retry screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "সংযোগ ব্যাহত হয়েছে",
                                color = Color(0xFF032B1D),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { homeViewModel.refreshBooks() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0A4E38)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Retry (পুনরায় চেষ্টা করুন)", color = Color.White)
                            }
                        }
                    }
                }
                is HomeUiState.Success -> {
                    // Books listing grid
                    val coroutineScope = rememberCoroutineScope()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.books) { book ->
                            val isFav by homeViewModel.localSyncRepository.isFavoriteFlow(userEmail, book.id).collectAsState(initial = false)
                            val isPin by homeViewModel.localSyncRepository.isPinnedFlow(userEmail, book.id).collectAsState(initial = false)
                            BookCard(
                                book = book,
                                onClick = { onBookClick(book) },
                                isFavorite = isFav,
                                isPinned = isPin,
                                onFavoriteClick = {
                                    coroutineScope.launch {
                                        homeViewModel.localSyncRepository.toggleFavorite(userEmail, book.id)
                                    }
                                },
                                onPinClick = {
                                    coroutineScope.launch {
                                        homeViewModel.localSyncRepository.togglePin(userEmail, book.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerBookItem() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            ShimmerCustomPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerCustomPlaceholder(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerCustomPlaceholder(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ShimmerCustomPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        Color(0xFFEBEBEB),
        Color(0xFFF5F5F5),
        Color(0xFFEBEBEB),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(0f, 0f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .background(brush)
    )
}

@Composable
fun ContinueReadingSection(
    progressList: List<LocalBookProgress>,
    allBooks: List<SupabaseBook>,
    onResumeClick: (SupabaseBook) -> Unit,
    modifier: Modifier = Modifier
) {
    val readingBooks = progressList.filter { it.status == "reading" }

    if (readingBooks.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("📖", fontSize = 28.sp)
                Column {
                    Text(
                        text = "পঠন অভ্যাস শুরু করুন!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF043B2B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "সহিহ দ্বীনি জ্ঞানার্জনে আপনার প্রথম বইটি পড়া শুরু করুন।",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = null,
                tint = Color(0xFF059669),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "আপনার বইসমূহ (Continue Reading)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF043B2B),
                fontFamily = FontFamily.Serif
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(readingBooks) { progress ->
                val book = allBooks.find { it.id == progress.bookId }
                if (book != null) {
                    Card(
                        modifier = Modifier
                            .width(280.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFFECEFF0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Book Emblem Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(width = 54.dp, height = 76.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF059669), Color(0xFF14B8A6))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = book.title.take(1),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = book.author,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                val percent = progress.progressPercentage?.toInt() ?: 0
                                LinearProgressIndicator(
                                    progress = { (progress.currentPage.toFloat() / progress.totalPages.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = Color(0xFF059669),
                                    trackColor = Color(0xFFE5E7EB)
                                )
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$percent% (${progress.currentPage}/${progress.totalPages} পৃষ্ঠা)",
                                        fontSize = 9.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onResumeClick(book) },
                                        color = Color(0xFF059669),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "Resume",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
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
