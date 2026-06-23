package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Reply
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ForumComment
import com.example.data.ForumPost
import com.example.ui.viewmodel.ForumViewModel
import com.example.ui.viewmodel.PostDetailUiState
// This comment ensures the file is synchronized correctly to Github
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// Locally scoped variables to prevent conflicting declarations in the library package namespace
private val DetailPrimaryPurple = Color(0xFF6366F1)
private val DetailDarkPurple = Color(0xFF4F46E5)
private val DetailBackgroundPurplePastel = Color(0xFFF5F3FF)
private val DetailCardBackgroundWhite = Color(0xFFFFFFFF)
private val DetailBorderLightVariant = Color(0xFFE5E7EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    forumViewModel: ForumViewModel,
    userId: String,
    userEmail: String,
    userRole: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detailState by forumViewModel.detailState.collectAsState()
    val likedPostIds by forumViewModel.likedPostIds.collectAsState()
    var commentText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showReportDialog by remember { mutableStateOf(false) }

    val isGuest = userRole.equals("guest", ignoreCase = true) || userEmail.isBlank() || userEmail.contains("guest", ignoreCase = true)

    // Edit Post Dialog States
    var showEditPostDialog by remember { mutableStateOf(false) }
    var editPostTitle by remember { mutableStateOf("") }
    var editPostContent by remember { mutableStateOf("") }

    // Edit Comment Dialog States
    var showEditCommentDialog by remember { mutableStateOf(false) }
    var editCommentId by remember { mutableStateOf("") }
    var editCommentContent by remember { mutableStateOf("") }

    // Delete Comment Dialog States
    var showDeleteCommentDialog by remember { mutableStateOf(false) }
    var deleteCommentId by remember { mutableStateOf("") }

    // Delete Post Dialog States
    var showDeletePostDialog by remember { mutableStateOf(false) }
    var deletePostId by remember { mutableStateOf("") }

    // Privacy Switch local toggle state for replies
    var isCommentPrivate by remember { mutableStateOf(false) }

    // Local comment likes state manager (Map comment ID -> like count increment, i.e., 0 or 1)
    val commentLikesMap = remember { mutableStateMapOf<String, Int>() }
    // Local comment liked toggle states
    val commentLikedStates = remember { mutableStateMapOf<String, Boolean>() }

    val categoryMappings = mapOf(
        "General" to "General",
        "Quran" to "Quran",
        "Hadith" to "Hadith",
        "Fiqh" to "Fiqh",
        "Sira" to "Q&A",
        "Others" to "Others"
    )

    LaunchedEffect(postId) {
        forumViewModel.loadPostDetails(postId)
    }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            forumViewModel.checkUserLikes(userId)
        }
    }

    LaunchedEffect(Unit) {
        forumViewModel.errorMessage.collectLatest { error ->
            snackbarHostState.showSnackbar(error)
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
            title = { Text("রিপোর্ট করুন / Flag Post", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DetailDarkPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ফিতনা ও অশালীনতা মুক্ত রাখতে সাহায্য করুন। পোস্টারকে সতর্ক করা বা এডমিনের মাধ্যমে পোস্টটি পর্যালোচনা করা হবে।", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    reasons.forEach { reason ->
                        Button(
                            onClick = {
                                forumViewModel.reportPost(postId, reason) {
                                    showReportDialog = false
                                    Toast.makeText(context, "রিপোর্ট সফলভাবে দাখিল করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DetailBackgroundPurplePastel, contentColor = DetailDarkPurple),
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
                    Text("বাতিল করুন", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showEditPostDialog) {
        AlertDialog(
            onDismissRequest = { showEditPostDialog = false },
            title = { Text("পোস্ট সম্পাদনা করুন", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DetailDarkPurple) },
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
                            focusedBorderColor = DetailPrimaryPurple,
                            focusedLabelColor = DetailPrimaryPurple,
                            cursorColor = DetailPrimaryPurple
                        )
                    )
                    OutlinedTextField(
                        value = editPostContent,
                        onValueChange = { editPostContent = it },
                        label = { Text("মূল বক্তব্য") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DetailPrimaryPurple,
                            focusedLabelColor = DetailPrimaryPurple,
                            cursorColor = DetailPrimaryPurple
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editPostTitle.isNotBlank() && editPostContent.isNotBlank()) {
                            forumViewModel.editPost(postId, userId, editPostTitle, editPostContent) {
                                showEditPostDialog = false
                            }
                        } else {
                            Toast.makeText(context, "শিরোনাম এবং মূল বক্তব্য খালি রাখা যাবে না।", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DetailPrimaryPurple)
                ) {
                    Text("সংরক্ষণ করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPostDialog = false }) {
                    Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showEditCommentDialog) {
        AlertDialog(
            onDismissRequest = { showEditCommentDialog = false },
            title = { Text("মন্তব্য সম্পাদনা করুন", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DetailDarkPurple) },
            text = {
                OutlinedTextField(
                    value = editCommentContent,
                    onValueChange = { editCommentContent = it },
                    label = { Text("মন্তব্যের বিষয়বস্তু") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DetailPrimaryPurple,
                        focusedLabelColor = DetailPrimaryPurple,
                        cursorColor = DetailPrimaryPurple
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editCommentContent.isNotBlank()) {
                            forumViewModel.editComment(editCommentId, userId, editCommentContent, postId) {
                                showEditCommentDialog = false
                            }
                        } else {
                            Toast.makeText(context, "মন্তব্য খালি রাখা যাবে না।", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DetailPrimaryPurple)
                ) {
                    Text("সংরক্ষণ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCommentDialog = false }) {
                    Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showDeleteCommentDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCommentDialog = false },
            title = { Text("মন্তব্য মুছুন", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Red) },
            text = { Text("আপনি কি নিশ্চিতভাবে এই মন্তব্যটি ডিলিট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        forumViewModel.deleteComment(deleteCommentId, userId, postId) {
                            showDeleteCommentDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("মুছে ফেলুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCommentDialog = false }) {
                    Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            onBackClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("মুছে ফেলুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePostDialog = false }) {
                    Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Discussion Thread",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Discussion thread link copied!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DetailDarkPurple
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DetailBackgroundPurplePastel)
        ) {
            when (val state = detailState) {
                is PostDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DetailPrimaryPurple)
                    }
                }
                is PostDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error occurred!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { forumViewModel.loadPostDetails(postId) }, colors = ButtonDefaults.buttonColors(containerColor = DetailPrimaryPurple)) {
                            Text("Retry")
                        }
                    }
                }
                is PostDetailUiState.Success -> {
                    val post = state.postWithComments.post
                    val comments = state.postWithComments.comments

                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            // Section a: Original Post View Card
                            item {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    OriginalPostViewCard(
                                        post = post,
                                        currentUserId = userId,
                                        currentUserRole = userRole,
                                        categoryMapping = categoryMappings[post.category] ?: post.category,
                                        isLiked = likedPostIds.contains(post.id),
                                        onDeleteClick = {
                                            deletePostId = post.id
                                            showDeletePostDialog = true
                                        },
                                        onReportClick = {
                                            showReportDialog = true
                                        },
                                        onLikeClick = {
                                            if (isGuest) {
                                                coroutineScope.launch { snackbarHostState.showSnackbar("Please login to participate in the community") }
                                            } else {
                                                forumViewModel.toggleLike(post.id, userId)
                                            }
                                        },
                                        onEditClick = {
                                            editPostTitle = post.title
                                            editPostContent = post.content
                                            showEditPostDialog = true
                                        }
                                    )
                                }
                            }

                            // Comments divider & Header label
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Comments (${comments.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp),
                                        color = DetailBorderLightVariant
                                    )
                                }
                            }

                            // Empty State for comments
                            if (comments.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Comment,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = DetailPrimaryPurple.copy(alpha = 0.25f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "No comments yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                        Text(text = "Be the first to share your thoughts!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // Section b: List of Comment Items
                            items(comments) { comment ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    val isCommentLiked = commentLikedStates[comment.id] ?: false
                                    val localLikesCount = commentLikesMap[comment.id] ?: 0

                                    CommentViewItem(
                                        comment = comment,
                                        currentUserId = userId,
                                        currentUserRole = userRole,
                                        isLiked = isCommentLiked,
                                        likesCount = localLikesCount,
                                        onLikeClick = {
                                            if (isGuest) {
                                                coroutineScope.launch { snackbarHostState.showSnackbar("Please login to like comments") }
                                            } else {
                                                // Dynamic reactive like logic
                                                if (isCommentLiked) {
                                                    commentLikedStates[comment.id] = false
                                                    commentLikesMap[comment.id] = 0
                                                    Toast.makeText(context, "Comment like removed", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    commentLikedStates[comment.id] = true
                                                    commentLikesMap[comment.id] = 1
                                                    Toast.makeText(context, "Comment liked!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onReplyClick = {
                                            // Auto-mention and pre-fill the bottom input bar
                                            val name = if (!comment.authorEmail.isNullOrBlank()) {
                                                comment.authorEmail.split("@").first().replaceFirstChar { it.uppercase() }
                                            } else {
                                                "User"
                                            }
                                            commentText = "@$name "
                                            Toast.makeText(context, "Replying to @$name", Toast.LENGTH_SHORT).show()
                                        },
                                        onDeleteClick = {
                                            deleteCommentId = comment.id
                                            showDeleteCommentDialog = true
                                        },
                                        onEditClick = {
                                            editCommentId = comment.id
                                            editCommentContent = comment.content
                                            showEditCommentDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Section c: Sticky Floating Bottom Reply Input Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .shadow(24.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(DetailCardBackgroundWhite)
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Top controls: Privacy toggle switch + Character Limit indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sliding rounded private toggle switch
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { isCommentPrivate = !isCommentPrivate }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp, 24.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = if (isCommentPrivate) {
                                                        listOf(Color(0xFFD1D5DB), Color(0xFFD1D5DB))
                                                    } else {
                                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                                    }
                                                )
                                            )
                                            .padding(2.dp)
                                    ) {
                                        val targetOffset by animateDpAsState(targetValue = if (isCommentPrivate) 0.dp else 20.dp)
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .offset(x = targetOffset)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isCommentPrivate) "Private Comment" else "Public Comment",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "${commentText.length}/500",
                                    fontSize = 11.sp,
                                    color = if (commentText.length > 500) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // The rounded field + Send icon overlay
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { if (it.length <= 500) commentText = it },
                                    placeholder = { Text("Write your reply or feedback here...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF9FAFB),
                                        unfocusedContainerColor = Color(0xFFF9FAFB),
                                        focusedBorderColor = DetailPrimaryPurple,
                                        unfocusedBorderColor = DetailBorderLightVariant
                                    ),
                                    maxLines = 4,
                                    enabled = !isGuest
                                )

                                // Gradient round send button
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (commentText.isBlank() || isGuest) {
                                                Brush.linearGradient(colors = listOf(Color(0xFFE5E7EB), Color(0xFFE5E7EB)))
                                            } else {
                                                Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                                            }
                                        )
                                        .clickable(enabled = commentText.isNotBlank() && !isGuest) {
                                            val sanitized = com.example.data.ContentSanitizer.sanitize(commentText.trim())
                                            if (sanitized.length < 2) {
                                                Toast.makeText(context, "Reply must be at least 2 characters.", Toast.LENGTH_SHORT).show()
                                                return@clickable
                                            }
                                            forumViewModel.addComment(postId, userId, userEmail, sanitized, userRole)
                                            commentText = ""
                                            Toast.makeText(context, "Comment posted successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (commentText.isBlank() || isGuest) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                        modifier = Modifier.size(18.dp)
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
fun OriginalPostViewCard(
    post: ForumPost,
    currentUserId: String,
    currentUserRole: String,
    categoryMapping: String,
    isLiked: Boolean,
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

    val generatedViews = remember(post.id) {
        (post.title.substring(0, minOf(post.title.length, 5)).hashCode().absoluteValue % 130) + 52
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DetailCardBackgroundWhite),
        border = BorderStroke(1.dp, DetailBorderLightVariant)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // Header: Avatar, Name, Timestamp, Category badge, Flags and Mod controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Avatar styled beautifully with purple gradient
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authorName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = authorName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        if (post.userRole?.lowercase() == "admin") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DetailPrimaryPurple)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Category badge matching the specified mappings
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = categoryMapping,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Flag trigger
                IconButton(onClick = onReportClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Flag post",
                        tint = Color.Red.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (canDelete) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit post",
                            tint = DetailPrimaryPurple.copy(alpha = 0.8f),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Text content body fully formatted
            Text(
                text = post.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = post.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = DetailBorderLightVariant)
            Spacer(modifier = Modifier.height(14.dp))

            // Footer statistics: Reaction count, comment indicator, generated view counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // React Heart Toggle button
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
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount ?: 0}",
                        fontSize = 14.sp,
                        color = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(22.dp))

                // Comment Count
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Comment count",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.repliesCount ?: 0}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(22.dp))

                // Views row
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Views icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$generatedViews",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CommentViewItem(
    comment: ForumComment,
    currentUserId: String,
    currentUserRole: String,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    isLiked: Boolean,
    likesCount: Int,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit
) {
    val authorName = if (!comment.authorEmail.isNullOrBlank()) {
        comment.authorEmail.split("@").first().replaceFirstChar { it.uppercase() }
    } else {
        "User"
    }

    val isAuthor = currentUserId.lowercase() == comment.userId?.lowercase() || currentUserId.lowercase() == comment.authorEmail?.lowercase()
    val isAdmin = currentUserRole.lowercase() == "admin"
    val canEditOrDelete = isAuthor || isAdmin

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DetailCardBackgroundWhite),
        border = BorderStroke(1.dp, DetailBorderLightVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header metadata: Bubble Avatar, author name, Admin flag, time stamp, edit/delete icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bubble Avatar with soft background colors
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEF2F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authorName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = DetailPrimaryPurple,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = authorName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        if (comment.userRole?.lowercase() == "admin") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DetailPrimaryPurple)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
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
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = formatRelativeTime(comment.createdAt),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (canEditOrDelete) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit comment",
                            tint = DetailPrimaryPurple.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete comment",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body text
            Text(
                text = comment.content,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Triggers: Like comment (+count) and Reply button trigger (mention)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Comment Like
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onLikeClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like comment",
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${likesCount}",
                        fontSize = 11.sp,
                        color = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Reply / Auto-Mention (@Name)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onReplyClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Reply,
                        contentDescription = "Reply to comment",
                        tint = DetailPrimaryPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reply",
                        fontSize = 11.sp,
                        color = DetailPrimaryPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
