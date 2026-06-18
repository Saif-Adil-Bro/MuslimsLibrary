package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.SupabaseBook
import com.example.data.repository.LocalSyncRepository
import com.example.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: String,
    userId: String,
    homeViewModel: HomeViewModel,
    localSyncRepository: LocalSyncRepository,
    onReadNowClick: (SupabaseBook) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Retrieve book detail dynamically
    val book = remember(bookId, homeViewModel.allPublicBooks) {
        homeViewModel.allPublicBooks.find { it.id == bookId }
    }

    // Observe Favorites and Pins reactively from Local Room flows
    val isFavorite by localSyncRepository.isFavoriteFlow(userId, bookId).collectAsState(initial = false)
    val isPinned by localSyncRepository.isPinnedFlow(userId, bookId).collectAsState(initial = false)

    // Observe Book progress status and page counts
    val progressFlow = remember(userId, bookId) {
        localSyncRepository.getBookProgressFlow(userId, bookId)
    }
    val bookProgress by progressFlow.collectAsState(initial = null)

    // Observe Notes mapped with Room Database Flow
    val notesFlow = remember(userId, bookId) {
        localSyncRepository.getNotesFlow(userId, bookId)
    }
    val notesList by notesFlow.collectAsState(initial = emptyList())

    // Note input state
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteInputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = book?.title ?: "বইয়ের বিবরণ",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Pinned Quick action
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                localSyncRepository.togglePin(userId, bookId)
                                val msg = if (isPinned) "পিন থেকে বাদ দেওয়া হয়েছে" else "হোমে পিন করা হয়েছে"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("detail_pin_button")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Book",
                            tint = if (isPinned) Color(0xFF10B981) else Color.White
                        )
                    }

                    // Favorite Quick action
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                localSyncRepository.toggleFavorite(userId, bookId)
                                val msg = if (isFavorite) "পছন্দের তালিকা থেকে বাদ দেওয়া হয়েছে" else "পছন্দের তালিকায় যুক্ত হয়েছে"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("detail_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite Book",
                            tint = if (isFavorite) Color.Red else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF043B2B)
                )
            )
        }
    ) { innerPadding ->
        if (book == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFFCFCFA)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF043B2B))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "বইয়ের বিবরণ লোড হচ্ছে...",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF9F9F4))
                    .verticalScroll(scrollState)
            ) {
                // Header Visual Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF043B2B), Color(0xFFF9F9F4))
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Book Cover with reflection shadow
                        Card(
                            modifier = Modifier
                                .weight(1.3f)
                                .aspectRatio(2f / 3f),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(book.coverImageUrl)
                                        .placeholder(com.example.R.drawable.ic_book_placeholder)
                                        .error(com.example.R.drawable.ic_book_placeholder)
                                        .fallback(com.example.R.drawable.ic_book_placeholder)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = book.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // File type overlay
                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .background(Color(0xFF043B2B), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Text(
                                        text = book.fileType.uppercase(),
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // Right-aligned Metadata detail
                        Column(
                            modifier = Modifier.weight(2f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = book.category,
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = book.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                lineHeight = 24.sp
                            )

                            Text(
                                text = "লেখক: ${book.author}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Local Progress Tracking Info Banner Card
                            bookProgress?.let { progress ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (progress.status == "completed") "পড়া সম্পন্ন হয়েছে! 🎉" else "বর্তমান পৃষ্ঠা: ${progress.currentPage} / ${progress.totalPages}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val pct = progress.progressPercentage.toFloat() / 100f
                                        LinearProgressIndicator(
                                            progress = { pct.coerceIn(0f, 1f) },
                                            color = Color(0xFF10B981),
                                            trackColor = Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                                        )
                                    }
                                }
                            } ?: run {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "এখনই পড়া শুরু করুন! 📖",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Call-To-Action (Read Now Button)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Button(
                        onClick = { onReadNowClick(book) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("read_now_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (bookProgress != null && bookProgress!!.currentPage > 1) "পড়া অব্যাহত রাখুন (পৃষ্ঠা ${bookProgress!!.currentPage})" else "বইটি পড়ুন (Read Now)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Reactive Favorites & Pin quick settings section (M3 Switch / Card)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "লাইব্রেরি অপশন",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF043B2B)
                        )

                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        // Favorite list toggle row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.Red.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "পছন্দের তালিকায় যুক্ত করুন",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF032B1D)
                                    )
                                    Text(
                                        text = "দ্রুত পছন্দের তালিকায় কিতাবটি রাখুন",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Switch(
                                checked = isFavorite,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        localSyncRepository.toggleFavorite(userId, bookId)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF10B981)
                                ),
                                modifier = Modifier.testTag("favorite_switch")
                            )
                        }

                        // Pin list toggle row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "হোমে পিন করে রাখুন",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF032B1D)
                                    )
                                    Text(
                                        text = "হোম পেজের সবার উপরে দেখতে পাবেন",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Switch(
                                checked = isPinned,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        localSyncRepository.togglePin(userId, bookId)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF10B981)
                                ),
                                modifier = Modifier.testTag("pin_switch")
                            )
                        }
                    }
                }

                // Notes / Thoughts Panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "আমার কিতাবী নোটসমূহ (${notesList.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF043B2B)
                            )

                            // Add Note Trigger Button
                            Button(
                                onClick = {
                                    noteInputText = ""
                                    showAddNoteDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF043B2B),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("open_add_note_dialog")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("নোট যোগ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        if (notesList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "এই কিতাব সম্পর্কে কোনো গুরুত্বপূর্ণ তথ্য বা নোট যোগ করা হয়নি।",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                notesList.forEach { note ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFA)),
                                        border = BorderStroke(1.dp, Color(0xFFE9EAE3)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val addedDate = remember(note.timestamp) {
                                                    val df = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                                                    df.format(java.util.Date(note.timestamp))
                                                }
                                                Text(
                                                    text = addedDate,
                                                    color = Color.Gray,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )

                                                IconButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            localSyncRepository.deleteNote(note.id, userId)
                                                            Toast.makeText(context, "নোট ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .testTag("delete_note_${note.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "নোট মুছুন",
                                                        tint = Color.Red.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = note.noteContent,
                                                fontSize = 13.sp,
                                                color = Color(0xFF374151),
                                                lineHeight = 18.sp,
                                                fontWeight = FontWeight.Medium
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

    // Modal AlertDialog for adding notes cleanly
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = {
                Text(
                    text = "গুরুত্বপূর্ণ নোট যোগ করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF043B2B)
                )
            },
            text = {
                OutlinedTextField(
                    value = noteInputText,
                    onValueChange = { noteInputText = it },
                    placeholder = { Text("এখানে আপনার মন্তব্য বা অনুধাবন লিখুন...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("note_input_field"),
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF10B981)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteInputText.isNotBlank()) {
                            coroutineScope.launch {
                                localSyncRepository.addNote(
                                    userId = userId,
                                    bookId = bookId,
                                    noteContent = noteInputText
                                )
                                showAddNoteDialog = false
                                noteInputText = ""
                                Toast.makeText(context, "নোটটি সফলভাবে যোগ করা হয়েছে! ✍️", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "অনুগ্রহ করে কিছু লিখুন", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.testTag("confirm_add_note_button")
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("বাতিল", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}
