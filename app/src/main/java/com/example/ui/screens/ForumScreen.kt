package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.material.icons.filled.People
import androidx.compose.foundation.BorderStroke

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
        "All" to "সব পোস্ট",
        "General" to "সাধারণ আলোচনা",
        "Quran" to "আল-কুরআন",
        "Hadith" to "আল-হাদিস",
        "Fiqh" to "ইসলামিক ফিকহ",
        "Sira" to "সীরাতুন্নবী",
        "Others" to "অন্যান্য"
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
            title = { Text("রিপোর্ট করুন / Flag Post", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF043B2B)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ফিতনা ও অশালীনতা মুক্ত রাখতে সাহায্য করুন। পোস্টারকে সতর্ক করা বা এডমিনের মাধ্যমে পোস্টটি পর্যালোচনা করা হবে।", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    reasons.forEach { reason ->
                        Button(
                            onClick = {
                                forumViewModel.reportPost(selectedReportPostId, reason) {
                                    showReportDialog = false
                                    Toast.makeText(context, "রিপোর্ট সফলভাবে দাখিল করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F4F2), contentColor = Color(0xFF043B2B)),
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
                    Text("বাতিল করুন", color = Color.Gray)
                }
            }
        )
    }

    val isGuest = userRole.equals("guest", ignoreCase = true) || userEmail.isBlank() || userEmail.contains("guest", ignoreCase = true)

    if (showEditPostDialog) {
        AlertDialog(
            onDismissRequest = { showEditPostDialog = false },
            title = { Text("পোস্ট সম্পাদনা করুন", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF043B2B)) },
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
                            focusedBorderColor = Color(0xFF043B2B),
                            focusedLabelColor = Color(0xFF043B2B),
                            cursorColor = Color(0xFF043B2B)
                        )
                    )
                    OutlinedTextField(
                        value = editPostContent,
                        onValueChange = { editPostContent = it },
                        label = { Text("মূল বক্তব্য") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF043B2B),
                            focusedLabelColor = Color(0xFF043B2B),
                            cursorColor = Color(0xFF043B2B)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B))
                ) {
                    Text("সংরক্ষণ করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPostDialog = false }) {
                    Text("বাতিল", color = Color.Gray)
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
                    Text("বাতিল", color = Color.Gray)
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
                    containerColor = Color(0xFF0A4E38),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "নতুন পোস্ট লিখুন")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFCFDF9))
        ) {
            // Hero Title bar with rich teal/emerald gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF043B2B), Color(0xFF0A4E38))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "COMMUNITY SHELF & FORUM",
                            color = Color(0xFFA3E2C9),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ইসলামিক আলোচনা ও ফোরাম",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }
                    IconButton(
                        onClick = { forumViewModel.loadPosts() },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "রিলোড করুন")
                    }
                }
            }

            // Horizontal scrolling Category Chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                forumViewModel.categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { forumViewModel.selectCategory(category) },
                        label = {
                            Text(
                                text = categoryMappings[category] ?: category,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0A4E38),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF1F4F2),
                            labelColor = Color(0xFF043B2B)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color(0xFFDCE2DE),
                            selectedBorderColor = Color(0xFF0A4E38),
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            // Forum Quick Stats Dashboard
            val stats by forumViewModel.forumStats.collectAsState()
            ForumStatsSection(stats = stats)

            // Feed Content corresponding to current UI State
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
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "এই ক্যাটাগরিতে কোনো পোস্ট নেই!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF043B2B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "প্রথম পোস্টটি লিখে আলোচনা শুরু করুন।",
                                fontSize = 13.sp,
                                color = Color.Gray
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E38))
                            ) {
                                Text("পুনরায় চেষ্টা করুন")
                            }
                        }
                    }
                    is ForumUiState.Success -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "মোট পোস্ট: ${state.posts.size} টি পাওয়া গেছে",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().weight(1f),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.posts) { post ->
                                    ForumPostCard(
                                        post = post,
                                        currentUserId = userId, // Pass authenticating UID
                                        currentUserRole = userRole,
                                        categoryMapping = categoryMappings[post.category] ?: post.category,
                                        isLiked = likedPostIds.contains(post.id),
                                        onClick = { onNavigateToPostDetail(post.id) },
                                        onDeleteClick = {
                                            deletePostId = post.id
                                             showDeletePostDialog = true
                                             if (false) {
                                                Toast.makeText(context, "পোস্ট মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                            }
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

@Composable
fun ForumPostSkeletonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE8EFEA)))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(modifier = Modifier.size(width = 120.dp, height = 12.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8EFEA)))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.size(width = 70.dp, height = 8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8EFEA)))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8EFEA)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8EFEA)))
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
        "অজানা মেম্বার"
    }

    val isAuthor = currentUserId.lowercase() == post.userId?.lowercase() || currentUserId.lowercase() == post.authorEmail?.lowercase()
    val isAdmin = currentUserRole.lowercase() == "admin"
    val canDelete = isAuthor || isAdmin

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp
        ),
        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: User AV, Name, Role badge, Time and Delete (if privileged)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular profile initial indicator with rich gradient
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (post.userRole?.lowercase() == "admin") {
                                    listOf(Color(0xFFD4AF37), Color(0xFF8B6508))
                                } else {
                                    listOf(Color(0xFF059669), Color(0xFF14B8A6))
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authorName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = authorName,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (post.userRole?.lowercase() == "admin") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFD4AF37))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                  Text(
                                    text = "এডমিন",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = formatRelativeTime(post.createdAt),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Category tag overlay
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = categoryMapping,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Report / Flag post button
                IconButton(onClick = onReportClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "রিপোর্ট করুন",
                        tint = Color.Red.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (canDelete) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "সম্পাদনা করুন",
                            tint = Color(0xFF059669).copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "মুছে ফেলুন",
                            tint = Color.Red.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Title and snippet (max 3 lines)
            Text(
                text = post.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                fontFamily = FontFamily.Serif,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = post.content,
                fontSize = 14.sp,
                color = Color(0xFF4B5563),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFFFECEF0).copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer info bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Comments Indicator (Teal Pill outline)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = "মন্তব্য",
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.repliesCount ?: 0} মন্তব্য",
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Interactive Like Action (Heart Button with soft background or pulse tint)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isLiked) Color.Red.copy(alpha = 0.08f) else Color(0xFF10B981).copy(alpha = 0.05f)
                        )
                        .clickable { onLikeClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "লাইক",
                            tint = if (isLiked) Color.Red else Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.likesCount ?: 0} লাইক",
                            fontSize = 12.sp,
                            color = if (isLiked) Color.Red else Color(0xFF059669),
                            fontWeight = FontWeight.Bold
                        )
                    }
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val statsList = listOf(
            Triple(totalPostsAnimated, "মোট পোস্ট", Icons.Default.Forum),
            Triple(totalLikesAnimated, "মোট লাইক", Icons.Filled.Favorite),
            Triple(totalCommentsAnimated, "মোট মন্তব্য", Icons.Default.Comment),
            Triple(activeUsersAnimated, "সক্রিয় সদস্য", Icons.Default.People)
        )

        statsList.forEach { (value, label, icon) ->
            Card(
                modifier = Modifier
                    .width(128.dp)
                    .height(90.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF059669), Color(0xFF14B8A6))
                            )
                        )
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
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
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Text(
                            text = String.format("%,d", value),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

fun formatRelativeTime(isoString: String?): String {
    if (isoString == null) return "এইমাত্র"
    try {
        val trimmed = isoString.substringBefore(".").substringBefore("+")
        val parts = trimmed.split("T")
        if (parts.size < 2) return "কিছুক্ষণ আগে"
        val dateParts = parts[0].split("-")
        val timeParts = parts[1].split(":")
        if (dateParts.size < 3 || timeParts.size < 2) return "কিছুক্ষণ আগে"
        
        val year = dateParts[0].toIntOrNull() ?: 2026
        val month = dateParts[1].toIntOrNull() ?: 6
        val day = dateParts[2].toIntOrNull() ?: 17
        val hour = timeParts[0].toIntOrNull() ?: 0
        val minute = timeParts[1].toIntOrNull() ?: 0
        
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.set(year, month - 1, day, hour, minute, 0)
        
        val diffMs = System.currentTimeMillis() - cal.timeInMillis
        val diffMins = diffMs / (1000 * 60)
        if (diffMins < 1) return "এইমাত্র"
        if (diffMins < 60) return "${diffMins} মিনিট আগে"
        val diffHours = diffMins / 60
        if (diffHours < 24) return "${diffHours} ঘণ্টা আগে"
        val diffDays = diffHours / 24
        if (diffDays < 30) return "${diffDays} দিন আগে"
        return "${day}/${month}/${year}"
    } catch (e: Exception) {
        return "কিছুক্ষণ আগে"
    }
}

