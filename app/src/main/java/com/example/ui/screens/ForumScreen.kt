package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ForumPost
import com.example.data.ForumStats
import com.example.ui.viewmodel.ForumUiState
import com.example.ui.viewmodel.ForumViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// Colors based on the requested community styling
val PrimaryGradientStart = Color(0xFF667EEA)
val PrimaryGradientEnd = Color(0xFF764BA2)
val PrimaryPurple = Color(0xFF6366F1)
val DarkPurple = Color(0xFF4F46E5)
val BackgroundPurplePastel = Color(0xFFF5F3FF)
val CardBackgroundWhite = Color(0xFFFFFFFF)
val TextGrayMain = Color(0xFF1F2937)
val TextGrayMuted = Color(0xFF6B7280)
val BorderLightVariant = Color(0xFFE5E7EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumScreen(
    forumViewModel: ForumViewModel,
    userId: String,
    userEmail: String,
    userRole: String,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by forumViewModel.uiState.collectAsState()
    val selectedCategory by forumViewModel.selectedCategory.collectAsState()
    val likedPostIds by forumViewModel.likedPostIds.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReportPostId by remember { mutableStateOf("") }

    var showEditPostDialog by remember { mutableStateOf(false) }
    var editPostId by remember { mutableStateOf("") }
    var editPostTitle by remember { mutableStateOf("") }
    var editPostContent by remember { mutableStateOf("") }

    var showDeletePostDialog by remember { mutableStateOf(false) }
    var deletePostId by remember { mutableStateOf("") }

    // Category mappings for display inside the UI (English database tag -> Bengali display tag)
    val categoryMappings = mapOf(
        "All" to "All",
        "General" to "General",
        "Quran" to "Quran",
        "Hadith" to "Hadith",
        "Fiqh" to "Fiqh",
        "Sira" to "Q&A",
        "Others" to "Others"
    )

    LaunchedEffect(Unit) {
        forumViewModel.loadPosts()
        forumViewModel.errorMessage.collectLatest { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            forumViewModel.checkUserLikes(userId)
        }
    }

    LaunchedEffect(Unit) {
        forumViewModel.successMessage.collectLatest { success ->
            Toast.makeText(context, success, Toast.LENGTH_SHORT).show()
        }
    }

    if (showReportDialog) {
        val reasons = listOf(
            "Spam / স্প্যাম বা বিজ্ঞাপন",
            "Inappropriate / অনুপযুক্ত বিষয়বস্তু",
            "Harassment / অশালীন বা হয়রানিমূলক আচরণ",
            "Other / অন্যান্য"
        )
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("রিপোর্ট করুন / Report Post", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ফিতনা ও অশালীনতা মুক্ত রাখতে সাহায্য করুন। পোস্টারকে সতর্ক করা বা এডমিনের মাধ্যমে পোস্টটি পর্যালোচনা করা হবে।", fontSize = 13.sp, color = TextGrayMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    reasons.forEach { reason ->
                        Button(
                            onClick = {
                                forumViewModel.reportPost(selectedReportPostId, reason) {
                                    showReportDialog = false
                                    Toast.makeText(context, "রিপোর্ট সফলভাবে দাখিল করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BackgroundPurplePastel, contentColor = DarkPurple),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(reason, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("বাতিল করুন", color = TextGrayMuted)
                }
            }
        )
    }

    val isGuest = userRole.equals("guest", ignoreCase = true) || userEmail.isBlank() || userEmail.contains("guest", ignoreCase = true)

    if (showEditPostDialog) {
        AlertDialog(
            onDismissRequest = { showEditPostDialog = false },
            title = { Text("পোস্ট সম্পাদনা করুন", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkPurple) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editPostTitle,
                        onValueChange = { editPostTitle = it },
                        label = { Text("শিরোনাম") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            focusedLabelColor = PrimaryPurple,
                            cursorColor = PrimaryPurple
                        )
                    )
                    OutlinedTextField(
                        value = editPostContent,
                        onValueChange = { editPostContent = it },
                        label = { Text("মূল বক্তব্য") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            focusedLabelColor = PrimaryPurple,
                            cursorColor = PrimaryPurple
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editPostTitle.isNotBlank() && editPostContent.isNotBlank()) {
                            forumViewModel.editPost(editPostId, userId, editPostTitle, editPostContent) {
                                showEditPostDialog = false
                            }
                        } else {
                            Toast.makeText(context, "শিরোনাম এবং মূল বক্তব্য খালি রাখা যাবে না।", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("সংরক্ষণ করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPostDialog = false }) {
                    Text("বাতিল", color = TextGrayMuted)
                }
            }
        )
    }

    if (showDeletePostDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePostDialog = false },
            title = { Text("পোস্ট মুছুন", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Red) },
            text = { Text("আপনি কি নিশ্চিতভাবে এই পোস্টটি ডিলিট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        forumViewModel.deletePost(deletePostId, userId) {
                            showDeletePostDialog = false
                            Toast.makeText(context, "পোস্টটি সফলভাবে মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("মুছে ফেলুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePostDialog = false }) {
                    Text("বাতিল", color = TextGrayMuted)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isGuest) {
                FloatingActionButton(
                    onClick = onNavigateToCreatePost,
                    containerColor = PrimaryPurple,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .shadow(12.dp, shape = CircleShape, clip = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "নতুন পোস্ট লিখুন",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState is ForumUiState.Loading,
            onRefresh = { forumViewModel.loadPosts() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundPurplePastel)
            ) {
                // Hero Title bar with beautiful primary gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryGradientStart, PrimaryGradientEnd)
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text(
                            text = "COMMUNITY FORUM",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

            // Horizontal scrolling Category Pills container matching the style requested
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackgroundWhite)
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    forumViewModel.categories.forEach { category ->
                     val isSelected = selectedCategory == category
                     val displayLabel = categoryMappings[category] ?: category
                     
                     Box(
                         modifier = Modifier
                             .clip(RoundedCornerShape(20.dp))
                             .background(
                                 if (isSelected) {
                                     Brush.linearGradient(colors = listOf(PrimaryGradientStart, PrimaryGradientEnd))
                                 } else {
                                     Brush.linearGradient(colors = listOf(Color(0xFFF3F4F6), Color(0xFFF3F4F6)))
                                 }
                             )
                             .clickable { forumViewModel.selectCategory(category) }
                             .border(
                                 1.dp,
                                 if (isSelected) PrimaryPurple else BorderLightVariant,
                                 RoundedCornerShape(20.dp)
                             )
                             .padding(horizontal = 20.dp, vertical = 8.dp),
                         contentAlignment = Alignment.Center
                     ) {
                         Text(
                             text = displayLabel,
                             fontSize = 14.sp,
                             fontWeight = FontWeight.Medium,
                             color = if (isSelected) Color.White else TextGrayMuted
                         )
                     }
                 }
                }
            }

            // Forum Quick Stats Dashboard styled beautifully using Gradient Highlight Borders
            val stats by forumViewModel.forumStats.collectAsState()
            ForumStatsSection(stats = stats)

            // Feed Content with success states
            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is ForumUiState.Loading -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(3) {
                                ForumPostSkeletonCard()
                            }
                        }
                    }
                    is ForumUiState.Empty -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = PrimaryPurple.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "কোনো পোস্ট পাওয়া যায়নি!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGrayMain
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "প্রথম পোস্টটি লিখে আপনার আলোচনা শুরু করুন।",
                                fontSize = 13.sp,
                                color = TextGrayMuted
                            )
                        }
                    }
                    is ForumUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ত্রুটি ঘটেছে!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { forumViewModel.loadPosts() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                            ) {
                                Text("পুনরায় চেষ্টা করুন")
                            }
                        }
                    }
                    is ForumUiState.Success -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "মেম্বারদের মোট পোস্টসংখ্যা: ${state.posts.size} টি",
                                fontSize = 12.sp,
                                color = TextGrayMuted,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                            
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.posts) { post ->
                                    ForumPostCard(
                                        post = post,
                                        currentUserId = userId,
                                        currentUserRole = userRole,
                                        categoryMapping = categoryMappings[post.category] ?: post.category,
                                        isLiked = likedPostIds.contains(post.id),
                                        onClick = { onNavigateToPostDetail(post.id) },
                                        onDeleteClick = {
                                            deletePostId = post.id
                                            showDeletePostDialog = true
                                        },
                                        onReportClick = {
                                            selectedReportPostId = post.id
                                            showReportDialog = true
                                        },
                                        onLikeClick = {
                                            if (isGuest) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Please login to participate in the community")
                                                }
                                            } else {
                                                forumViewModel.toggleLike(post.id, userId)
                                            }
                                        },
                                        onEditClick = {
                                            editPostId = post.id
                                            editPostTitle = post.title
                                            editPostContent = post.content
                                            showEditPostDialog = true
                                        }
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

@Composable
fun ForumPostSkeletonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE0E7FF)))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(modifier = Modifier.size(width = 120.dp, height = 12.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE0E7FF)))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.size(width = 70.dp, height = 8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE0E7FF)))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE0E7FF)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE0E7FF)))
        }
    }
}

@Composable
fun ForumPostCard(
    post: ForumPost,
    currentUserId: String,
    currentUserRole: String,
    categoryMapping: String,
    isLiked: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit,
    onLikeClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val authorName = if (!post.authorEmail.isNullOrBlank()) {
        post.authorEmail.split("@").first().replaceFirstChar { it.uppercase() }
    } else {
        "User"
    }

    val isAuthor = currentUserId.lowercase() == post.userId?.lowercase() || currentUserId.lowercase() == post.authorEmail?.lowercase()
    val isAdmin = currentUserRole.lowercase() == "admin"
    val canDelete = isAuthor || isAdmin

    // Generate consistent visual view count for metrics layout
    val generatedViews = remember(post.id) {
        (post.title.substring(0, minOf(post.title.length, 5)).hashCode().absoluteValue % 140) + 48
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                clip = true
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundWhite),
        border = BorderStroke(1.dp, BorderLightVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: Avatar, Author, Time, Badges, Mod actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar styled beautifully with the gradient
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryGradientStart, PrimaryGradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authorName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = authorName,
                            fontWeight = FontWeight.SemiBold,
                            color = TextGrayMain,
                            fontSize = 15.sp
                        )
                        if (post.userRole?.lowercase() == "admin") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryPurple)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Admin",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatRelativeTime(post.createdAt),
                        fontSize = 12.sp,
                        color = TextGrayMuted
                    )
                }

                // Category badge matching colors from HTML rules
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryGradientStart, PrimaryGradientEnd)
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = categoryMapping,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Report button
                IconButton(onClick = onReportClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Report post",
                        tint = Color.Red.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (canDelete) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit post",
                            tint = PrimaryPurple.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete post",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body
            Text(
                text = post.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextGrayMain,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = post.content,
                fontSize = 14.sp,
                color = TextGrayMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderLightVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Footer / Stats Row: Like/React, comments, views
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Interactive React Box
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLikeClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Reaction",
                        tint = if (isLiked) Color.Red else TextGrayMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount ?: 0}",
                        fontSize = 13.sp,
                        color = if (isLiked) Color.Red else TextGrayMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Comment Indicator row
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Comments",
                        tint = TextGrayMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.repliesCount ?: 0}",
                        fontSize = 13.sp,
                        color = TextGrayMuted,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Generated dynamic views row
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Views count",
                        tint = TextGrayMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$generatedViews",
                        fontSize = 13.sp,
                        color = TextGrayMuted,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ForumStatsSection(stats: ForumStats?) {
    val totalPostsAnimated by animateIntAsState(targetValue = stats?.totalPosts ?: 0, label = "posts")
    val totalLikesAnimated by animateIntAsState(targetValue = stats?.totalLikes ?: 0, label = "likes")
    val totalCommentsAnimated by animateIntAsState(targetValue = stats?.totalComments ?: 0, label = "comments")
    val activeUsersAnimated by animateIntAsState(targetValue = stats?.activeUsers ?: 1, label = "users")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val statsList = listOf(
            Triple(totalPostsAnimated, "Total Posts", Icons.Default.Forum),
            Triple(totalLikesAnimated, "Total Likes", Icons.Filled.Favorite),
            Triple(totalCommentsAnimated, "Replies", Icons.Default.Comment),
            Triple(activeUsersAnimated, "Active Members", Icons.Default.People)
        )

        statsList.forEach { (value, label, icon) ->
            Card(
                modifier = Modifier
                    .width(134.dp)
                    .height(92.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, BorderLightVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = TextGrayMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = String.format("%,d", value),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextGrayMain
                    )
                }
            }
        }
    }
}

fun formatRelativeTime(isoString: String?): String {
    if (isoString == null) return "Just now"
    try {
        val trimmed = isoString.substringBefore(".").substringBefore("+")
        val parts = trimmed.split("T")
        if (parts.size < 2) return "Recently"
        val dateParts = parts[0].split("-")
        val timeParts = parts[1].split(":")
        if (dateParts.size < 3 || timeParts.size < 2) return "Recently"
        
        val year = dateParts[0].toIntOrNull() ?: 2026
        val month = dateParts[1].toIntOrNull() ?: 6
        val day = dateParts[2].toIntOrNull() ?: 17
        val hour = timeParts[0].toIntOrNull() ?: 0
        val minute = timeParts[1].toIntOrNull() ?: 0
        
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.set(year, month - 1, day, hour, minute, 0)
        
        val diffMs = System.currentTimeMillis() - cal.timeInMillis
        val diffMins = diffMs / (1000 * 60)
        if (diffMins < 1) return "Just now"
        if (diffMins < 60) return "${diffMins}m ago"
        val diffHours = diffMins / 60
        if (diffHours < 24) return "${diffHours}h ago"
        val diffDays = diffHours / 24
        if (diffDays < 30) return "${diffDays}d ago"
        return "${day}/${month}/${year}"
    } catch (e: Exception) {
        return "Recently"
    }
}
