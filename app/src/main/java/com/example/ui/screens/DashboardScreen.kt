package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.data.SupabaseBook
import com.example.data.SupabaseService
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ForumViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    libraryViewModel: LibraryViewModel,
    homeViewModel: HomeViewModel,
    adminViewModel: AdminViewModel,
    forumViewModel: ForumViewModel,
    userEmail: String,
    userUid: String = "",
    userRole: String,
    onLogoutClick: () -> Unit,
    onBookClick: (SupabaseBook) -> Unit,
    onNavigateToAddBook: () -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
    debugInfo: String = "",
    isDebugMode: Boolean = false,
    onToggleDebug: () -> Unit = {},
    backupManager: com.example.data.backup.BackupManager? = null
) {
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    var showRestorePrompt by remember { mutableStateOf(false) }
    var checkingBackup by remember { mutableStateOf(false) }
    var restoreInProgress by remember { mutableStateOf(false) }
    var restoreSuccess by remember { mutableStateOf(false) }
    var restoreError by remember { mutableStateOf(null as String?) }
    var hasCheckedThisSession by remember { mutableStateOf(false) }

    LaunchedEffect(userUid) {
        if (backupManager != null && userUid.isNotBlank() && !hasCheckedThisSession) {
            checkingBackup = true
            try {
                val exists = backupManager.backupExistsOnCloud(userUid)
                if (exists) {
                    showRestorePrompt = true
                }
            } catch (e: java.lang.Exception) {
                android.util.Log.e("DashboardScreen", "Error checking backup: ${e.message}", e)
            } finally {
                checkingBackup = false
                hasCheckedThisSession = true
            }
        }
    }

    if (showRestorePrompt) {
        AlertDialog(
            onDismissRequest = { showRestorePrompt = false },
            title = { Text("ডাটা রিস্টোর করার সুযোগ", fontWeight = FontWeight.Bold, color = Color(0xFF043B2B)) },
            text = { Text("আপনার এই একাউন্টটির জন্য ক্লাউডে পূর্বের ব্যাকআপ পাওয়া গেছে। আপনি কি পূর্বের ডিভাইস থেকে পড়া বইয়ের অগ্রগতি, প্রিয় তালিকা এবং অনন্য নোটসমূহ রিস্টোর করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRestorePrompt = false
                        restoreInProgress = true
                        scope.launch {
                            try {
                                val success = backupManager?.downloadAndRestoreFromCloud(userUid) == true
                                if (success) {
                                    restoreSuccess = true
                                } else {
                                    restoreError = "No backup found for this account"
                                }
                            } catch (e: java.lang.Exception) {
                                val errMsg = e.localizedMessage ?: e.message ?: ""
                                restoreError = when {
                                    errMsg.contains("Permission denied", ignoreCase = true) || errMsg.contains("403") || errMsg.contains("policy", ignoreCase = true) || errMsg.contains("unauthorized", ignoreCase = true) -> {
                                        "Permission denied. Please contact support."
                                    }
                                    errMsg.contains("FileNotFound", ignoreCase = true) || errMsg.contains("404") || errMsg.contains("not found", ignoreCase = true) -> {
                                        "No backup found for this account"
                                    }
                                    else -> {
                                        errMsg
                                    }
                                }
                            } finally {
                                restoreInProgress = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B))
                ) {
                    Text("হ্যাঁ, রিস্টোর করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestorePrompt = false }) {
                    Text("না, পরে করব", color = Color.Gray)
                }
            }
        )
    }

    if (restoreInProgress) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color(0xFF043B2B))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("অপেক্ষা করুন...", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("আপনার ব্যাকআপ ফাইলটি ডাউনলোড এবং রিস্টোর করা হচ্ছে। অনুগ্রহ করে অ্যাপস বন্ধ করবেন না।") }
        )
    }

    if (restoreSuccess) {
        AlertDialog(
            onDismissRequest = { restoreSuccess = false },
            confirmButton = {
                Button(
                    onClick = { restoreSuccess = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B))
                ) {
                    Text("ঠিক আছে", color = Color.White)
                }
            },
            title = { Text("ডাটা রিস্টোর সফল", fontWeight = FontWeight.Bold, color = Color(0xFF0A4E38)) },
            text = { Text("অভিনন্দন! আপনার পূর্বের সব ডাটা সফলভাবে পুনরুদ্ধার করা হয়েছে।") }
        )
    }

    if (restoreError != null) {
        AlertDialog(
            onDismissRequest = { restoreError = null },
            confirmButton = {
                Button(
                    onClick = { restoreError = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B))
                ) {
                    Text("ঠিক আছে", color = Color.White)
                }
            },
            title = { Text("ত্রুটি ঘটেছে", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text(restoreError ?: "") }
        )
    }

    // Conditional tabs based on userRole
    val isAdmin = userRole.lowercase() == "admin"

    LaunchedEffect(userRole) {
        if (isAdmin) {
            selectedTab = 2
        } else {
            selectedTab = 0
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = Color(0xFF032B1D)
            ) {
                // Home Tab
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF0A4E38),
                        indicatorColor = Color(0xFF0A4E38)
                    )
                )

                // Community Tab
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Community") },
                    label = { Text("Community", style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF0A4E38),
                        indicatorColor = Color(0xFF0A4E38)
                    )
                )

                // Admin Dashboard Tab
                if (isAdmin) {
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Lock, contentDescription = "Admin Hub") },
                        label = { Text("Admin Hub", style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color(0xFF0A4E38),
                            indicatorColor = Color(0xFF0A4E38)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    HomeScreen(
                        homeViewModel = homeViewModel,
                        userEmail = userEmail,
                        role = userRole,
                        onLogoutClick = onLogoutClick,
                        onBookClick = onBookClick,
                        onNavigateToProfile = onNavigateToProfile,
                        onSwitchToAdminClick = { if (isAdmin) selectedTab = 2 },
                        onHeaderClick = onToggleDebug
                    )
                }
                1 -> {
                    ForumScreen(
                        forumViewModel = forumViewModel,
                        userEmail = userEmail,
                        userRole = userRole,
                        onNavigateToCreatePost = onNavigateToCreatePost,
                        onNavigateToPostDetail = onNavigateToPostDetail
                    )
                }
                2 -> {
                    if (isAdmin) {
                        AdminDashboardScreen(
                            adminViewModel = adminViewModel,
                            onNavigateToAddBook = onNavigateToAddBook,
                            userEmail = userEmail,
                            onLogoutClick = onLogoutClick
                        )
                    } else {
                        selectedTab = 0
                    }
                }
                else -> {
                    selectedTab = 0
                }
            }

            // Floating Debug Console Overlay at the bottom center of the screen
            if (isDebugMode && debugInfo.isNotEmpty()) {
                var isLogsVisible by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(0.95f)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.92f)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🛠️ Auth Debug Panel",
                                    color = Color(0xFFA3E2C9),
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                TextButton(
                                    onClick = { isLogsVisible = !isLogsVisible },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isLogsVisible) "Hide Details" else "Show Details",
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (isLogsVisible) {
                                Box(
                                    modifier = Modifier
                                        .heightIn(max = 180.dp)
                                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                ) {
                                    Text(
                                        text = debugInfo,
                                        color = Color(0xFFA3E2C9),
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            } else {
                                // Just show simple summary (first 5 lines contains key state info)
                                val summary = debugInfo.split("\n").take(5).joinToString("\n")
                                Text(
                                    text = summary,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
