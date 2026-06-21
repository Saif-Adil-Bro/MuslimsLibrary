package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AddBookScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.AllBooksScreen
import androidx.compose.foundation.layout.WindowInsets
import com.example.ui.screens.BookReaderScreen
import com.example.ui.screens.BookDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ForumViewModel
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.screens.ForumScreen
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.PostDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.EditProfileScreen
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import com.example.ui.viewmodel.NotificationViewModel
import com.example.ui.screens.NotificationCenterScreen
import com.example.ui.screens.NotificationSettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission if Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Obtain AppContainer dependencies
                val appContainer = (application as MuslimsLibraryApplication).container
                val context = androidx.compose.ui.platform.LocalContext.current

                // Inject via native factories
                val notificationViewModel: NotificationViewModel = viewModel(
                    factory = NotificationViewModel.Factory(
                        appContainer.supabaseService,
                        appContainer.guestModeManager,
                        context
                    )
                )
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.Factory(appContainer.authRepository)
                )
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.Factory(appContainer.appDatabase, appContainer.supabaseService)
                )
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(appContainer.supabaseService, appContainer.localSyncRepository)
                )
                val adminViewModel: AdminViewModel = viewModel(
                    factory = AdminViewModel.Factory(appContainer.supabaseClient, appContainer.supabaseService)
                )
                val forumViewModel: ForumViewModel = viewModel(
                    factory = ForumViewModel.Factory(appContainer.supabaseService, appContainer.guestModeManager)
                )
                val authorViewModel: com.example.ui.viewmodel.AuthorViewModel = viewModel(
                    factory = com.example.ui.viewmodel.AuthorViewModel.Factory(appContainer.supabaseService)
                )
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.Factory(
                        appContainer.authRepository,
                        appContainer.supabaseService,
                        appContainer.backupManager,
                        appContainer.appDatabase
                    )
                )
                val downloadedBooksViewModel: com.example.ui.viewmodel.DownloadedBooksViewModel = viewModel(
                    factory = com.example.ui.viewmodel.DownloadedBooksViewModel.Factory(
                        appContainer.appDatabase,
                        appContainer.downloadManager,
                        appContainer.supabaseService
                    )
                )
                
                val navController = rememberNavController()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                
                val toastMessage by authViewModel.toastMessage.collectAsState()
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                var isRestoring by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                var restoreMessage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("ক্লাউড থেকে তথ্য পরীক্ষা করা হচ্ছে...") }

                val authState by authViewModel.uiState.collectAsState()

                LaunchedEffect(authState) {
                    val currentAuthState = authState
                    if (currentAuthState is com.example.ui.viewmodel.AuthState.Success) {
                        val userUid = currentAuthState.uid
                        val userEmail = currentAuthState.email
                        if (userUid.isNotBlank() || userEmail.isNotBlank()) {
                            val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            val key = "auto_restore_completed_$userUid"
                            val hasAutoRestored = prefs.getBoolean(key, false)
                            if (!hasAutoRestored) {
                                // Prevent double triggering while launching async jobs
                                prefs.edit().putBoolean(key, true).apply()
                                try {
                                    isRestoring = true
                                    restoreMessage = "ক্লাউড ব্যাকআপ পরীক্ষা করা হচ্ছে..."
                                    android.util.Log.d("MainActivity", "Logged in successfully. Checking backup... UID: $userUid, Email: $userEmail")
                                    var exists = false
                                    var backupIdToUse = ""
                                    
                                    if (userEmail.isNotBlank()) {
                                        exists = appContainer.backupManager.backupExistsOnCloud(userEmail)
                                        if (exists) {
                                            backupIdToUse = userEmail
                                        }
                                    }
                                    if (!exists && userUid.isNotBlank()) {
                                        exists = appContainer.backupManager.backupExistsOnCloud(userUid)
                                        if (exists) {
                                            backupIdToUse = userUid
                                        }
                                    }

                                    if (exists && backupIdToUse.isNotBlank()) {
                                        restoreMessage = "আপনার ডাটা রিস্টোর হচ্ছে... অনুগ্রহ করে অপেক্ষা করুন"
                                        try {
                                            appContainer.backupManager.downloadBackup(
                                                cloudBackupUserId = backupIdToUse,
                                                roomUserId = userEmail
                                            )
                                            
                                            // Refresh stats on profile screen immediately
                                            try {
                                                profileViewModel.loadStatistics(userEmail)
                                            } catch (ex: Exception) {
                                                android.util.Log.e("MainActivity", "Failed to force reload profile stats: ${ex.message}")
                                            }
                                            
                                            android.widget.Toast.makeText(context, "আপনার ডাটা সফলভাবে রিস্টোর হয়েছে!", android.widget.Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            val errMsg = e.localizedMessage ?: e.message ?: ""
                                            android.util.Log.e("MainActivity", "Automated restore failed: $errMsg", e)
                                            android.widget.Toast.makeText(context, "রিস্টোর ব্যর্থ হয়েছে: $errMsg", android.widget.Toast.LENGTH_LONG).show()
                                        } finally {
                                            isRestoring = false
                                        }
                                    } else {
                                        isRestoring = false
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Error checking automated backup availability: ${e.message}", e)
                                    isRestoring = false
                                }
                            }
                        }
                    }
                }

                if (isRestoring) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {},
                        title = {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(color = androidx.compose.ui.graphics.Color(0xFF043B2B))
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                                androidx.compose.material3.Text(
                                    "ডাটা রিস্টোর হচ্ছে...", 
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF043B2B)
                                )
                            }
                        },
                        text = { androidx.compose.material3.Text(restoreMessage) }
                    )
                }

                LaunchedEffect(Unit) {
                    authViewModel.initDebugMode(context)
                }
                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
                        authViewModel.clearToastMessage()
                    }
                }

                 Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) "dashboard" else "auth",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // Registration & login route
                        composable(
                            route = "auth?fromGuest={fromGuest}",
                            arguments = listOf(
                                navArgument("fromGuest") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { backStackEntry ->
                            val fromGuestArg = backStackEntry.arguments?.getBoolean("fromGuest") ?: false
                            val isFromGuestVM = authViewModel.isFromGuestMode.collectAsState().value
                            val fromGuest = fromGuestArg || isFromGuestVM
                            AuthScreen(
                                viewModel = authViewModel,
                                showGuestOption = !fromGuest,
                                onCloseClick = if (fromGuest) {
                                    {
                                        authViewModel.setFromGuestMode(false)
                                        navController.popBackStack()
                                    }
                                } else null,
                                onAuthSuccess = {
                                    authViewModel.setFromGuestMode(false)
                                    navController.navigate("dashboard") {
                                        popUpTo("auth?fromGuest={fromGuest}") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Library catalog directory route
                        composable("dashboard") {
                            val authState = authViewModel.uiState.collectAsState().value
                            val userEmail = if (authState is com.example.ui.viewmodel.AuthState.Success) authState.email else "User@muslimslibrary.org"
                            val userUid = if (authState is com.example.ui.viewmodel.AuthState.Success) authState.uid else ""
                            val userRole by authViewModel.userRole.collectAsState()
                            val debugInfo by authViewModel.debugInfo.collectAsState()
                            val isDebugMode by authViewModel.isDebugMode.collectAsState()
                            val unreadNotificationsCount by notificationViewModel.unreadCount.collectAsState()
                            DashboardScreen(
                                libraryViewModel = libraryViewModel,
                                homeViewModel = homeViewModel,
                                adminViewModel = adminViewModel,
                                forumViewModel = forumViewModel,
                                profileViewModel = profileViewModel,
                                authorViewModel = authorViewModel,
                                localSyncRepository = appContainer.localSyncRepository,
                                userEmail = userEmail,
                                userUid = userUid,
                                userRole = userRole,
                                onLogoutClick = {
                                    val wasGuest = appContainer.guestModeManager.isGuestMode()
                                    authViewModel.setFromGuestMode(wasGuest)
                                    if (!wasGuest) {
                                        try {
                                            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                                        } catch (e: Exception) {
                                            android.util.Log.e("MainActivity", "Error clearing preferences on logout", e)
                                        }
                                        authViewModel.logout()
                                        navController.navigate("auth?fromGuest=$wasGuest") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("auth?fromGuest=$wasGuest")
                                    }
                                },
                                onBookClick = { book ->
                                    navController.navigate("book_detail/${book.id}")
                                },
                                onNavigateToAddBook = {
                                    navController.navigate("add_book")
                                },
                                onNavigateToCreatePost = {
                                    navController.navigate("create_post")
                                },
                                onNavigateToPostDetail = { postId ->
                                    navController.navigate("post_detail/$postId")
                                },
                                onNavigateToProfile = {
                                    navController.navigate("edit_profile")
                                },
                                onNavigateToDownloads = {
                                    navController.navigate("downloaded_books")
                                },
                                onNavigateToAllBooks = { sortBy, categoryFilter ->
                                    val filterParam = categoryFilter ?: "All"
                                    navController.navigate("all_books/$sortBy/$filterParam")
                                },
                                onNavigateToAdminDashboard = {
                                    navController.navigate("admin_dashboard")
                                },
                                debugInfo = debugInfo,
                                isDebugMode = isDebugMode,
                                onToggleDebug = { authViewModel.toggleDebugMode(context) },
                                backupManager = appContainer.backupManager,
                                isGuestMode = appContainer.guestModeManager.isGuestMode(),
                                onNotificationsClick = {
                                    navController.navigate("notification_center")
                                },
                                onNavigateToNotificationSettings = {
                                    navController.navigate("notification_settings")
                                },
                                unreadNotificationsCount = unreadNotificationsCount
                            )
                        }

                        // All Books Screen Route
                        composable(
                            route = "all_books/{sortBy}/{categoryFilter}",
                            arguments = listOf(
                                navArgument("sortBy") { type = NavType.StringType },
                                navArgument("categoryFilter") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val sortBy = backStackEntry.arguments?.getString("sortBy") ?: "recent"
                            val categoryFilter = backStackEntry.arguments?.getString("categoryFilter") ?: "All"
                            
                            AllBooksScreen(
                                sortBy = sortBy,
                                categoryFilter = if (categoryFilter == "All") null else categoryFilter,
                                onBackClick = { navController.popBackStack() },
                                onBookClick = { book ->
                                    navController.navigate("book_detail/${book.id}")
                                },
                                viewModel = homeViewModel
                            )
                        }

                        // Create Post Screen
                        composable("create_post") {
                            val authState = authViewModel.uiState.collectAsState().value
                            val userEmail = if (authState is com.example.ui.viewmodel.AuthState.Success) authState.email else "User@muslimslibrary.org"
                            val userUid = if (authState is com.example.ui.viewmodel.AuthState.Success) authState.uid else ""
                            val userRole by authViewModel.userRole.collectAsState()
                            CreatePostScreen(
                                forumViewModel = forumViewModel,
                                userId = userUid,
                                userEmail = userEmail,
                                userRole = userRole,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Post Detail Screen
                        composable(
                            route = "post_detail/{postId}",
                            arguments = listOf(navArgument("postId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val postId = backStackEntry.arguments?.getString("postId") ?: ""
                            val authState = authViewModel.uiState.collectAsState().value
                            val userEmail = if (authState is com.example.ui.viewmodel.AuthState.Success) authState.email else "User@muslimslibrary.org"
                            val userUid = if (authState is com.example.ui.viewmodel.AuthState.Success) authState.uid else ""
                            val userRole by authViewModel.userRole.collectAsState()
                            PostDetailScreen(
                                postId = postId,
                                forumViewModel = forumViewModel,
                                userId = userUid,
                                userEmail = userEmail,
                                userRole = userRole,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Book Detail Route
                        composable(
                            route = "book_detail/{bookId}",
                            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                            val userEmail = authViewModel.uiState.collectAsState().value.let { state ->
                                if (state is com.example.ui.viewmodel.AuthState.Success) state.email else "User@muslimslibrary.org"
                            }
                            BookDetailScreen(
                                bookId = bookId,
                                userId = userEmail,
                                homeViewModel = homeViewModel,
                                localSyncRepository = appContainer.localSyncRepository,
                                downloadedBooksViewModel = downloadedBooksViewModel,
                                onReadNowClick = { clickedBook ->
                                    val encodedTitle = java.net.URLEncoder.encode(clickedBook.title, "UTF-8")
                                    val encodedUrl = java.net.URLEncoder.encode(clickedBook.fileUrl ?: "", "UTF-8")
                                    navController.navigate("reader/${clickedBook.id}/$encodedTitle/$encodedUrl/${clickedBook.fileType}")
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Detailed PDF / Book render route
                        composable(
                            route = "reader/{bookId}/{bookTitle}",
                            arguments = listOf(
                                navArgument("bookId") { type = NavType.StringType },
                                navArgument("bookTitle") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                            val bookTitle = backStackEntry.arguments?.getString("bookTitle") ?: ""
                            val userEmail = authViewModel.uiState.collectAsState().value.let { state ->
                                if (state is com.example.ui.viewmodel.AuthState.Success) state.email else "User@muslimslibrary.org"
                            }
                            BookReaderScreen(
                                bookId = bookId,
                                bookTitle = bookTitle,
                                userId = userEmail,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Updated reader route with document URL and file type
                        composable(
                            route = "reader/{bookId}/{bookTitle}/{fileUrl}/{fileType}",
                            arguments = listOf(
                                navArgument("bookId") { type = NavType.StringType },
                                navArgument("bookTitle") { type = NavType.StringType },
                                navArgument("fileUrl") { type = NavType.StringType },
                                navArgument("fileType") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                            val bookTitle = backStackEntry.arguments?.getString("bookTitle") ?: ""
                            val fileUrl = backStackEntry.arguments?.getString("fileUrl") ?: ""
                            val fileType = backStackEntry.arguments?.getString("fileType") ?: ""
                            val userEmail = authViewModel.uiState.collectAsState().value.let { state ->
                                if (state is com.example.ui.viewmodel.AuthState.Success) state.email else "User@muslimslibrary.org"
                            }
                            BookReaderScreen(
                                bookId = bookId,
                                bookTitle = bookTitle,
                                fileUrl = fileUrl,
                                fileType = fileType,
                                userId = userEmail,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Admin Dashboard / Manager Route
                        composable("admin_dashboard") {
                            val authState = authViewModel.uiState.collectAsState().value
                            val userEmail = if (authState is com.example.ui.viewmodel.AuthState.Success) authState.email else ""
                            val userRole by authViewModel.userRole.collectAsState()
                            
                            if (userRole.lowercase() == "admin") {
                                AdminDashboardScreen(
                                    adminViewModel = adminViewModel,
                                    onNavigateToAddBook = {
                                        navController.navigate("add_book")
                                    },
                                    onNavigateToEditBook = { bookId ->
                                        navController.navigate("add_book?bookId=$bookId")
                                    },
                                    userEmail = userEmail,
                                    onLogoutClick = {
                                        try {
                                            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                                        } catch (e: Exception) {
                                            android.util.Log.e("MainActivity", "Error clearing preferences on logout", e)
                                        }
                                        authViewModel.logout()
                                        navController.navigate("auth") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            }
                        }

                        // Add Book Route with Admin authorization check guard
                        composable(
                            route = "add_book?bookId={bookId}",
                            arguments = listOf(
                                navArgument("bookId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val bookId = backStackEntry.arguments?.getString("bookId")
                            val authState = authViewModel.uiState.collectAsState().value
                            val userEmail = if (authState is com.example.ui.viewmodel.AuthState.Success) authState.email else ""
                            val userRole by authViewModel.userRole.collectAsState()
                            if (userRole.lowercase() == "admin") {
                                AddBookScreen(
                                    adminViewModel = adminViewModel,
                                    bookId = bookId,
                                    onBackClick = {
                                        navController.popBackStack()
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            }
                        }

                        // Profile Screen Route
                        composable("profile") {
                            val authState = authViewModel.uiState.collectAsState().value
                            val userEmail = if (authState is com.example.ui.viewmodel.AuthState.Success) authState.email else ""
                            val isGuest = appContainer.guestModeManager.isGuestMode()
                            ProfileScreen(
                                viewModel = profileViewModel,
                                localSyncRepository = appContainer.localSyncRepository,
                                userId = userEmail,
                                isGuestMode = isGuest,
                                onEditProfileClick = {
                                    navController.navigate("edit_profile")
                                },
                                onLogoutClick = {
                                    authViewModel.setFromGuestMode(isGuest)
                                    if (!isGuest) {
                                        try {
                                            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                                        } catch (e: Exception) {
                                            android.util.Log.e("MainActivity", "Error clearing preferences on logout", e)
                                        }
                                        authViewModel.logout()
                                        navController.navigate("auth?fromGuest=$isGuest") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("auth?fromGuest=$isGuest")
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onAdminDashboardClick = {
                                    navController.navigate("admin_dashboard")
                                },
                                onNotificationSettingsClick = {
                                    navController.navigate("notification_settings")
                                }
                            )
                        }

                        // Edit Profile Screen Route
                        composable("edit_profile") {
                            EditProfileScreen(
                                viewModel = profileViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Downloaded Books Screen Route
                        composable("downloaded_books") {
                            com.example.ui.screens.DownloadedBooksScreen(
                                viewModel = downloadedBooksViewModel,
                                onBookClick = { bookId ->
                                    val dbBook = downloadedBooksViewModel.downloadedBooks.value.find { it.bookId == bookId }
                                    val encodedTitle = java.net.URLEncoder.encode(dbBook?.title ?: "বই", "UTF-8")
                                    val encodedPath = java.net.URLEncoder.encode(dbBook?.localFilePath ?: "", "UTF-8")
                                    navController.navigate("reader/$bookId/$encodedTitle/$encodedPath/pdf")
                                },
                                onBackClick = { navController.popBackStack() },
                                onNavigateToHome = {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Notification Center Route
                        composable("notification_center") {
                            val isGuest = appContainer.guestModeManager.isGuestMode()
                            NotificationCenterScreen(
                                viewModel = notificationViewModel,
                                isGuest = isGuest,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Notification Settings Route
                        composable("notification_settings") {
                            val isGuest = appContainer.guestModeManager.isGuestMode()
                            NotificationSettingsScreen(
                                viewModel = notificationViewModel,
                                isGuest = isGuest,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
