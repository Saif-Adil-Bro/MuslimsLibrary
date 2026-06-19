package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.ui.viewmodel.BackupUiState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
    onBackClick: () -> Unit,
    onAdminDashboardClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    // Load statistics using Firebase UID (not Supabase UID)
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            android.util.Log.d("ProfileScreen", "Loading stats for Firebase UID: $userId")
            viewModel.loadStatistics(userId)
        }
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

                        // Observe Live Statistics from Room Database via ProfileViewModel
                        val stats by viewModel.stats.collectAsState()

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
                                        text = "আমার পড়াশোনার অগ্রগতি (Statistics)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF043B2B)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = "Statistics",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                                // Stats Grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatItem(
                                        label = "পড়া শেষ",
                                        value = stats.booksRead.toString(),
                                        icon = Icons.Default.CheckCircle,
                                        iconTint = Color(0xFF10B981)
                                    )

                                    StatItem(
                                        label = "চলমান",
                                        value = stats.booksInProgress.toString(),
                                        icon = Icons.Default.Book,
                                        iconTint = Color(0xFF3B82F6)
                                    )

                                    StatItem(
                                        label = "প্রিয়",
                                        value = stats.totalFavorites.toString(),
                                        icon = Icons.Default.Favorite,
                                        iconTint = Color(0xFFEF4444)
                                    )

                                    StatItem(
                                        label = "নোট",
                                        value = stats.totalNotes.toString(),
                                        icon = Icons.Default.Note,
                                        iconTint = Color(0xFFFBBF24)
                                    )
                                }

                                if (stats.totalPins > 0) {
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.15f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        StatItem(
                                            label = "পিন করা",
                                            value = stats.totalPins.toString(),
                                            icon = Icons.Default.PushPin,
                                            iconTint = Color(0xFF8B5CF6)
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
                            // Data Backup & Restore Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF4F0)),
                                border = BorderStroke(1.dp, Color(0xFF043B2B).copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "ডাটা ব্যাকআপ ও রিস্টোর",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF043B2B)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.performBackup(userId) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ব্যাকআপ", fontSize = 12.sp, color = Color.White)
                                        }

                                        Button(
                                            onClick = { viewModel.performRestore(userId) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E38)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("রিস্টোর", fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                            
                            if (user.role.equals("admin", ignoreCase = true) || userId.equals("admin@muslimslibrary.org", ignoreCase = true)) {
                                Button(
                                    onClick = onAdminDashboardClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("admin_dashboard_navigation_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC53030)), // crimson red accent
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = "Security Status", tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("অ্যাডমিন প্যানেল প্রবেশ করুন", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

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

    // Backup/Restore status dialogs/overlays
    when (backupStatus) {
        is BackupUiState.Loading -> {
            AlertDialog(
                onDismissRequest = { /* Prevent dismiss during operation */ },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.resetBackupStatus() }) {
                        Text("বাতিল করুন", color = Color(0xFF043B2B))
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFF043B2B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("অপেক্ষা করুন...", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Text("আপনার ডাটা ক্লাউডে অত্যন্ত নিরাপদে ব্যাকআপ/রিস্টোর করা হচ্ছে। অনুগ্রহ করে অ্যাপ বন্ধ করবেন না।")
                }
            )
        }
        is BackupUiState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetBackupStatus() },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetBackupStatus() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B))
                    ) {
                        Text("ঠিক আছে", color = Color.White)
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF0A4E38))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("সফল হয়েছে!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0A4E38))
                    }
                },
                text = {
                    Text("অভিনন্দন! আপনার ব্যাকআপ/রিস্টোর প্রক্রিয়াটি সফলভাবে সম্পন্ন হয়েছে।")
                }
            )
        }
        is BackupUiState.Error -> {
            val errorMessage = (backupStatus as BackupUiState.Error).message
            AlertDialog(
                onDismissRequest = { viewModel.resetBackupStatus() },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.resetBackupStatus() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("বন্ধ করুন", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                if (errorMessage.contains("রিস্টোর") || errorMessage.contains("রিস্টোর ")) {
                                    viewModel.performRestore(userId)
                                } else {
                                    viewModel.performBackup(userId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("পুনরায় চেষ্টা করুন", color = Color.White)
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ত্রুটি ঘটেছে", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Red)
                    }
                },
                text = {
                    Text(errorMessage)
                }
            )
        }
        else -> { /* Idle - do nothing */ }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = iconTint.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(5, 59, 43)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}
