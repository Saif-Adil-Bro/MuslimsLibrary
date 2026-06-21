package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
    onAdminDashboardClick: () -> Unit = {},
    isGuestMode: Boolean = false
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB)) // Matches var(--bg-light): #f9fafb
    ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF667EEA)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA))
                        ) {
                            Text("আবার চেষ্টা করুন", color = Color.White)
                        }
                    }
                }
                is ProfileUiState.Success -> {
                    val user = state.user
                    val displayName = user.displayName ?: user.email.split("@").first().replaceFirstChar { it.uppercase() }
                    val initialChar = if (displayName.isNotBlank()) displayName.first().toString() else "U"
                    val roleName = if (user.role.equals("admin", ignoreCase = true)) "অ্যাডমিন" else "ব্যবহারকারী"

                    val gradientBrush = Brush.linearGradient(
                        colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                    )

                    val stats by viewModel.stats.collectAsState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Header Section (Gradient Background - optimized vertical padding)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(gradientBrush)
                                .padding(top = 6.dp, bottom = 48.dp, start = 20.dp, end = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Large Avatar with Edit overlay
                                Box(
                                    modifier = Modifier.size(120.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(4.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                            .clickable { if (!isGuestMode) onEditProfileClick() },
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
                                            Text(
                                                text = initialChar.uppercase(),
                                                color = Color(0xFF6366F1),
                                                fontSize = 48.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (!isGuestMode) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                                                .clickable { onEditProfileClick() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "প্রোফাইল ছবি সম্পাদন",
                                                tint = Color(0xFF6366F1),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Display Name
                                Text(
                                    text = displayName,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                // User bio text
                                Text(
                                    text = if (!user.bio.isNullOrBlank()) user.bio else "আসসালামু আলাইকুম! আমি MuslimsLibrary-এর একটি সদস্য।",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.widthIn(max = 280.dp),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )

                                // Role Badge (User Badge)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .padding(horizontal = 20.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = roleName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Content List placed underneath shifting up -30.dp
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-30).dp)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Stats Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "আমার পড়াশোনার অগ্রগতি",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1F2937)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(gradientBrush),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BarChart,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // 4-column Grid layout using weights
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        NewStatItem(
                                            value = stats.booksRead,
                                            label = "পড়া শেষ",
                                            icon = Icons.Default.Check,
                                            gradientColors = listOf(Color(0xFF10B981), Color(0xFF34D399)),
                                            modifier = Modifier.weight(1f)
                                        )
                                        NewStatItem(
                                            value = stats.booksInProgress,
                                            label = "চলমান",
                                            icon = Icons.Default.Book,
                                            gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF60A5FA)),
                                            modifier = Modifier.weight(1f)
                                        )
                                        NewStatItem(
                                            value = stats.totalFavorites,
                                            label = "প্রিয়",
                                            icon = Icons.Default.Favorite,
                                            gradientColors = listOf(Color(0xFFEF4444), Color(0xFFF87171)),
                                            modifier = Modifier.weight(1f)
                                        )
                                        NewStatItem(
                                            value = stats.totalNotes,
                                            label = "নোট",
                                            icon = Icons.Default.EditNote,
                                            gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // Backup & Restore Card / Guest banner
                            if (isGuestMode) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFFFDE7) // Warm premium cream
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFFEB3B).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "গেস্ট মোড সক্রিয় আছে ⚠️",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF5D4037)
                                        )
                                        Text(
                                            text = "আপনার পড়ার রেকর্ড, প্রিয় তালিকা বা পিন বুকমার্ক শুধুমাত্র আপনার ডিভাইসেই সংরক্ষিত থাকবে। একটি স্থায়ী অ্যাকাউন্ট ছাড়া এটি ক্লাউড ব্যাকআপ করা সম্ভব নয়।",
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp,
                                            color = Color(0xFF795548)
                                        )
                                        Button(
                                            onClick = onLogoutClick,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF0A4E38)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Text(
                                                text = "নিবন্ধন / লগইন করুন",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))
                                                )
                                            )
                                            .padding(20.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "ডাটা ব্যাকআপ ও রিস্টোর",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F2937),
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                ElegantGradientButton(
                                                    onClick = { viewModel.performBackup(userId) },
                                                    text = "ব্যাকআপ",
                                                    icon = Icons.Default.CloudUpload,
                                                    gradientColors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                ElegantSecondaryButton(
                                                    onClick = { viewModel.performRestore(userId) },
                                                    text = "রিস্টোর",
                                                    icon = Icons.Default.CloudDownload,
                                                    borderColor = Color(0xFF6366F1),
                                                    textColor = Color(0xFF6366F1),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Admin Panel Navigation Click
                            if (user.role.equals("admin", ignoreCase = true)) {
                                ElegantGradientButton(
                                    onClick = onAdminDashboardClick,
                                    text = "অ্যাডমিন প্যানেল প্রবেশ করুন",
                                    icon = Icons.Default.Security,
                                    gradientColors = listOf(Color(0xFFE53935), Color(0xFFC62828)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_dashboard_navigation_button")
                                )
                            }

                            if (!isGuestMode) {
                                // Action buttons
                                ElegantGradientButton(
                                    onClick = onEditProfileClick,
                                    text = "প্রোফাইল পরিবর্তন করুন",
                                    icon = Icons.Default.Edit,
                                    gradientColors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                ElegantSecondaryButton(
                                    onClick = onLogoutClick,
                                    text = "লগআউট করুন",
                                    icon = Icons.Default.ExitToApp,
                                    borderColor = Color(0xFFEF4444),
                                    textColor = Color(0xFFEF4444),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                else -> { /* Unhandled or Idle states */ }
            }
    }

    // Keep fully functional backup overlays / status dialogues intact
    when (backupStatus) {
        is BackupUiState.Loading -> {
            AlertDialog(
                onDismissRequest = { /* Prevent dismiss during active operation */ },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.resetBackupStatus() }) {
                        Text("বাতিল করুন", color = Color(0xFF667EEA))
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFF667EEA))
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA))
                    ) {
                        Text("ঠিক আছে", color = Color.White)
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("সফল হয়েছে!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF10B981))
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA)),
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
fun NewStatItem(
    value: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedStatNumber(targetValue = value)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AnimatedStatNumber(targetValue: Int) {
    var count by remember { mutableStateOf(0) }
    LaunchedEffect(targetValue) {
        if (targetValue <= 0) {
            count = 0
            return@LaunchedEffect
        }
        val duration = 1000L // 1 second
        val stepTime = (duration / targetValue).coerceIn(15L, 60L)
        for (i in 1..targetValue) {
            kotlinx.coroutines.delay(stepTime)
            count = i
        }
    }
    Text(
        text = count.toString(),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1F2937)
    )
}

@Composable
fun ElegantGradientButton(
    onClick: () -> Unit,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1.0f)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ElegantSecondaryButton(
    onClick: () -> Unit,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1.0f)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
