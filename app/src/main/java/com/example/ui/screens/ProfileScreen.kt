package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.LocalSyncRepository
import com.example.ui.viewmodel.ProfileUiState
import com.example.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    localSyncRepository: LocalSyncRepository,
    userId: String,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("আমার প্রোফাইল", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (uiState is ProfileUiState.Success) {
                        IconButton(onClick = onEditProfileClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "প্রোফাইল সম্পাদন",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF043B2B)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7F9FA))
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF043B2B)
                    )
                }
                is ProfileUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp
                        )
                        Button(
                            onClick = { viewModel.loadProfile() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B))
                        ) {
                            Text("আবার চেষ্টা করুন", color = Color.White)
                        }
                    }
                }
                is ProfileUiState.Success -> {
                    val user = state.user
                    val displayName = user.displayName ?: user.email.split("@").first().replaceFirstChar { it.uppercase() }
                    val initialChar = if (displayName.isNotBlank()) displayName.first().toString() else "U"

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Circular Avatar/Profile Picture
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF043B2B).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!user.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = "প্রোফাইল ছবি",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0A4E38)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initialChar.uppercase(),
                                        color = Color.White,
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Display Name & Email
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = displayName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF043B2B),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = user.email,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Role Badge
                        val roleName = if (user.role.equals("admin", ignoreCase = true)) "অ্যাডমিন" else "ব্যবহারকারী"
                        val badgeColor = if (user.role.equals("admin", ignoreCase = true)) Color(0xFFE53935) else Color(0xFF0A4E38)
                        val badgeBg = badgeColor.copy(alpha = 0.12f)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = badgeBg),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = roleName,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }

                        // Observe Statistics from Room database flows
                        val progressList by localSyncRepository.getAllProgressFlow(userId).collectAsState(initial = emptyList())
                        val favoritesList by localSyncRepository.getFavoritesFlow(userId).collectAsState(initial = emptyList())
                        val notesList by localSyncRepository.getNotesForUserFlow(userId).collectAsState(initial = emptyList())

                        val completedCount = remember(progressList) { progressList.count { it.status == "completed" } }
                        val currentlyReadingCount = remember(progressList) { progressList.count { it.status == "reading" } }

                        // Reading Stats Dashboard Card (Material 3 Grid styled layout)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "আমার পড়াশোনার অগ্রগতি (Statistics)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF043B2B)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Stat Item 1: Reading
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = currentlyReadingCount.toString(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0A4E38)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "পড়ছি",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )
                                    }

                                    // Stat Item 2: Completed
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = completedCount.toString(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "পড়া শেষ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )
                                    }

                                    // Stat Item 3: Notes
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = notesList.size.toString(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "নোট সংখ্যা",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )
                                    }

                                    // Stat Item 4: Favorites
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = favoritesList.size.toString(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE53935)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "প্রিয় তালিকা",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        // Profile Details Box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "আমার জীবনী ও পরিচিতি",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF043B2B)
                                )
                                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                                Text(
                                    text = if (!user.bio.isNullOrBlank()) user.bio else "কোনো আত্মজীবনী বা বিবরণ যোগ করা হয়নি।",
                                    fontSize = 15.sp,
                                    color = if (!user.bio.isNullOrBlank()) Color.DarkGray else Color.Gray,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Logout and Edit Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = onEditProfileClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("প্রোফাইল পরিবর্তন করুন", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onLogoutClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("লগআউট করুন", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {
                    // Idle state or unhandled
                }
            }
        }
    }
}
