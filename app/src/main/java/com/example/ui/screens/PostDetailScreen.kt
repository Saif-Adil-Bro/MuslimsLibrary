package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ForumComment
import com.example.data.ForumPost
import com.example.ui.viewmodel.ForumViewModel
import com.example.ui.viewmodel.PostDetailUiState
import kotlinx.coroutines.flow.collectLatest

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

    val categoryMappings = mapOf(
        "General" to "সাধারণ আলোচনা",
        "Quran" to "আল-কুরআন",
        "Hadith" to "আল-হাদিস",
        "Fiqh" to "ইসলামিক ফিকহ",
        "Sira" to "সীরাতুন্নবী",
        "Others" to "অন্যান্য"
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
            title = { Text("রিপোর্ট করুন / Flag Post", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF043B2B)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ফিতনা ও অশালীনতা মুক্ত রাখতে সাহায্য করুন। পোস্টারকে সতর্ক করা বা এডমিনের মাধ্যমে পোস্টটি পর্যালোচনা করা হবে।", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    reasons.forEach { reason ->
                        Button(
                            onClick = {
                                forumViewModel.reportPost(postId, reason) {
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
                            forumViewModel.editPost(postId, userId, editPostTitle, editPostContent) {
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

    if (showEditCommentDialog) {
        AlertDialog(
            onDismissRequest = { showEditCommentDialog = false },
            title = { Text("মন্তব্য সম্পাদনা করুন", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF043B2B)) },
            text = {
                OutlinedTextField(
                    value = editCommentContent,
                    onValueChange = { editCommentContent = it },
                    label = { Text("মন্তব্যের বিষয়বস্তু") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF043B2B),
                        focusedLabelColor = Color(0xFF043B2B),
                        cursorColor = Color(0xFF043B2B)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B))
                ) {
                    Text("সংরক্ষণ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCommentDialog = false }) {
                    Text("বাতিল", color = Color.Gray)
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
                            onBackClick() // navigate back upon deletion
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "পোস্ট বিস্তারিত",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "প্রস্থান করুন",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF043B2B)
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFCFDF9))
        ) {
            // Main list area (post + replies)
            Box(modifier = Modifier.weight(1f)) {
                when (val state = detailState) {
                    is PostDetailUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF0A4E38))
                        }
                    }
                    is PostDetailUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "পোস্ট লোড করা যায়নি!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = state.message, color = Color.Gray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { forumViewModel.loadPostDetails(postId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E38))
                                ) {
                                    Text("পুনরায় চেষ্টা করুন")
                                }
                            }
                        }
                    }
                    is PostDetailUiState.Success -> {
                        val post = state.postWithComments.post
                        val comments = state.postWithComments.comments

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // First element: original post container view card
                            item {
                                OriginalPostViewCard(
                                    post = post,
                                    currentUserId = userId,
                                    currentUserRole = userRole,
                                    categoryDisplay = categoryMappings[post.category] ?: post.category,
                                    isLiked = likedPostIds.contains(post.id),
                                    onDeleteClick = {
                                        deletePostId = post.id
                                         showDeletePostDialog = true
                                         if (false) {
                                            Toast.makeText(context, "পোস্টটি মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                                            onBackClick()
                                        }
                                    },
                                    onReportClick = {
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
                                        editPostTitle = post.title
                                        editPostContent = post.content
                                        showEditPostDialog = true
                                    }
                                )
                            }

                            // Subheader of comments section
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = Color(0xFF0A4E38),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${comments.size} টি মন্তব্য",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF043B2B)
                                    )
                                }
                            }

                            if (comments.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "এখনো কোনো মন্তব্য করা হয়নি। প্রথম মন্তব্যটি লিখুন!",
                                            fontSize = 13.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            } else {
                                items(comments) { comment ->
                                    CommentViewItem(
                                        comment = comment,
                                        currentUserId = userId,
                                        currentUserRole = userRole,
                                        onEditClick = {
                                            editCommentId = comment.id
                                            editCommentContent = comment.content
                                            showEditCommentDialog = true
                                        },
                                        onDeleteClick = {
                                            deleteCommentId = comment.id
                                            showDeleteCommentDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input reply box docked strictly at the bottom
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = if (isGuest) "" else commentText,
                        onValueChange = { if (!isGuest) commentText = it },
                        placeholder = { Text(if (isGuest) "Login to comment" else "একটি চমৎকার মন্তব্য বা প্রশ্ন লিখুন...", fontSize = 14.sp) },
                        enabled = !isGuest,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color(0xFFF1F4F2),
                            unfocusedContainerColor = Color(0xFFF1F4F2),
                            disabledIndicatorColor = Color.Transparent,
                            disabledContainerColor = Color(0xFFF1F4F2).copy(alpha = 0.6f)
                        ),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = {
                            if (isGuest) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please login to participate in the community")
                                }
                            } else if (commentText.isNotBlank()) {
                                forumViewModel.addComment(
                                    postId = postId,
                                    userId = userId,
                                    email = userEmail,
                                    content = commentText.trim(),
                                    role = userRole
                                )
                                commentText = ""
                            }
                        },
                        enabled = !isGuest && commentText.isNotBlank(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFF0A4E38),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFECEFF0),
                            disabledContentColor = Color.Gray
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "মন্তব্য পাঠান", modifier = Modifier.size(20.dp))
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
    categoryDisplay: String,
    isLiked: Boolean,
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

    val isAuthor = currentUserId.lowercase() == post.authorEmail?.lowercase()
    val isAdmin = currentUserRole.lowercase() == "admin"
    val canDelete = isAuthor || isAdmin

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author row details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (post.userRole?.lowercase() == "admin") {
                                Color(0xFFD4AF37).copy(alpha = 0.2f)
                            } else {
                                Color(0xFF0A4E38).copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authorName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = if (post.userRole?.lowercase() == "admin") Color(0xFF8B6508) else Color(0xFF0A4E38),
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = authorName,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF043B2B),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (post.userRole?.lowercase() == "admin") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFD4AF37))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatRelativeTime(post.createdAt),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Category tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A4E38).copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = categoryDisplay,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF043B2B)
                    )
                }

                // Report button
                IconButton(onClick = onReportClick) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "রিপোর্ট করুন",
                        tint = Color.Red.copy(alpha = 0.5f)
                    )
                }

                if (canDelete) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "সম্পাদনা করুন",
                            tint = Color(0xFF043B2B).copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "মুছে ফেলুন",
                            tint = Color.Red.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = post.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF032B1D),
                fontFamily = FontFamily.Serif
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Body content
            Text(
                text = post.content,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = Color(0xFFECEFF0))

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "মন্তব্য",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.repliesCount ?: 0} টি মন্তব্য",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLikeClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "লাইক",
                        tint = if (isLiked) Color.Red else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.likesCount ?: 0} লাইক",
                        fontSize = 12.sp,
                        color = if (isLiked) Color.Red else Color.Gray,
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
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val authorName = if (!comment.authorEmail.isNullOrBlank()) {
        comment.authorEmail.split("@").first().replaceFirstChar { it.uppercase() }
    } else {
        "ফোরাম মেম্বার"
    }

    val isAuthor = currentUserId.lowercase() == comment.authorEmail?.lowercase()
    val isAdmin = currentUserRole.lowercase() == "admin"
    val canModify = isAuthor || isAdmin

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Initial indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (comment.userRole?.lowercase() == "admin") {
                                Color(0xFFD4AF37).copy(alpha = 0.15f)
                            } else {
                                Color(0xFF0A4E38).copy(alpha = 0.08f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authorName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = if (comment.userRole?.lowercase() == "admin") Color(0xFF8B6508) else Color(0xFF0A4E38),
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = authorName,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF043B2B),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (comment.userRole?.lowercase() == "admin") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFD4AF37))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "এডমিন",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Text(
                        text = formatRelativeTime(comment.createdAt),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                if (canModify) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "সম্পাদনা",
                                tint = Color(0xFF043B2B).copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "মুছে ফেলুন",
                                tint = Color.Red.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Message text
            Text(
                text = comment.content,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFF4A4A4A)
            )
        }
    }
}

