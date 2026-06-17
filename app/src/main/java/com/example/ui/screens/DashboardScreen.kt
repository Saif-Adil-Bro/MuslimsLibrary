package com.example.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import com.example.data.SupabaseBook
import com.example.data.SupabaseService
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    libraryViewModel: LibraryViewModel,
    homeViewModel: HomeViewModel,
    adminViewModel: AdminViewModel,
    userEmail: String,
    userRole: String,
    onLogoutClick: () -> Unit,
    onBookClick: (SupabaseBook) -> Unit,
    onNavigateToAddBook: () -> Unit,
    modifier: Modifier = Modifier,
    debugInfo: String = "",
    isDebugMode: Boolean = false,
    onToggleDebug: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }

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
                        onSwitchToAdminClick = { if (isAdmin) selectedTab = 2 },
                        onHeaderClick = onToggleDebug
                    )
                }
                1 -> {
                    ProfileForumScreen(
                        userEmail = userEmail,
                        role = userRole
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
