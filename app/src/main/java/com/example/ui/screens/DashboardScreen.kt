package com.example.ui.screens

import androidx.compose.foundation.layout.*
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
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    // Conditional tabs based on userRole
    val isAdmin = userRole.lowercase() == "admin"

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
                        onBookClick = onBookClick
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
                            onNavigateToAddBook = onNavigateToAddBook
                        )
                    } else {
                        selectedTab = 0
                    }
                }
                else -> {
                    selectedTab = 0
                }
            }
        }
    }
}
