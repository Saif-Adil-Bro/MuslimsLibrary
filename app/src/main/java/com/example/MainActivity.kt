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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Obtain AppContainer dependencies
                val appContainer = (application as MuslimsLibraryApplication).container
                
                // Inject via native factories
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
                    factory = ForumViewModel.Factory(appContainer.supabaseService)
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
                val context = androidx.compose.ui.platform.LocalContext.current
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                var showRestoreDialogForUser by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
                var isRestoring by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                val authState by authViewModel.uiState.collectAsState()

                LaunchedEffect(authState) {
                    val currentAuthState = authState
                    if (currentAuthState is com.example.ui.viewmodel.AuthState.Success) {
                        val userUid = currentAuthState.uid
                        if (userUid.isNotBlank()) {
                            val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            val key = "restore_prompt_shown_$userUid"
                            val hasShownPrompt = prefs.getBoolean(key, false)
                            if (!hasShownPrompt) {
                                // Mark as shown IMMEDIATELY to prevent repeat triggers on recomposition
                                prefs.edit().putBoolean(key, true).apply()
                                // Now check if backup exists on the cloud
                                try {
                                    val exists = appContainer.backupManager.backupExistsOnCloud(userUid)
                                    if (exists) {
                                        showRestoreDialogForUser = userUid
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Error checking backup exists on cloud", e)
                                }
                            }
                        }
                    }
                }

                if (showRestoreDialogForUser != null) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showRestoreDialogForUser = null },
                        title = { androidx.compose.material3.Text("ব্যাকআপ পাওয়া গেছে", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF043B2B)) },
                        text = { androidx.compose.material3.Text("আপনার আগের ডিভাইস থেকে ডাটা রিস্টোর করতে চান?") },
                        confirmButton = {
                            androidx.compose.material3.Button(
                                onClick = {
                                    showRestoreDialogForUser = null
                                    isRestoring = true
                                    coroutineScope.launch {
                                        try {
                                            appContainer.backupManager.downloadBackup()
                                            android.widget.Toast.makeText(context, "রিস্টোর সফল হয়েছে!", android.widget.Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            val errMsg = e.localizedMessage ?: e.message ?: ""
                                            android.widget.Toast.makeText(context, "রিস্টোর ব্যর্থ হয়েছে: $errMsg", android.widget.Toast.LENGTH_LONG).show()
                                        } finally {
                                            isRestoring = false
                                        }
                                    }
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF043B2B))
                            ) {
                                androidx.compose.material3.Text("রিস্টোর করুন", color = androidx.compose.ui.graphics.Color.White)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { showRestoreDialogForUser = null }
                            ) {
                                androidx.compose.material3.Text("এখন নয়", color = androidx.compose.ui.graphics.Color.Gray)
                            }
                        }
                    )
                }

                if (isRestoring) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {},
                        title = {
                            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                androidx.compose.material3.CircularProgressIndicator(color = androidx.compose.ui.graphics.Color(0xFF043B2B))
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))
                                androidx.compose.material3.Text("অপেক্ষা করুন...", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        },
                        text = { androidx.compose.material3.Text("আপনার ব্যাকআপ ফাইলটি ডাউনলোড এবং রিস্টোর করা হচ্ছে। অনুগ্রহ করে অ্যাপস বন্ধ করবেন না।") }
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
                        composable("auth") {
                            AuthScreen(
                                viewModel = authViewModel,
                                onAuthSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("auth") { inclusive = true }
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
                                    authViewModel.logout()
                                    navController.navigate("auth") {
                                        popUpTo("dashboard") { inclusive = true }
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
                                    navController.navigate("profile")
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
                                backupManager = appContainer.backupManager
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
                            
                            if (userRole.lowercase() == "admin" || userEmail.lowercase() == "admin@muslimslibrary.org") {
                                AdminDashboardScreen(
                                    adminViewModel = adminViewModel,
                                    onNavigateToAddBook = {
                                        navController.navigate("add_book")
                                    },
                                    userEmail = userEmail,
                                    onLogoutClick = {
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
                        composable("add_book") {
                            val userRole by authViewModel.userRole.collectAsState()
                            if (userRole.lowercase() == "admin") {
                                AddBookScreen(
                                    adminViewModel = adminViewModel,
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
                            ProfileScreen(
                                viewModel = profileViewModel,
                                localSyncRepository = appContainer.localSyncRepository,
                                userId = userEmail,
                                onEditProfileClick = {
                                    navController.navigate("edit_profile")
                                },
                                onLogoutClick = {
                                    authViewModel.logout()
                                    navController.navigate("auth") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onAdminDashboardClick = {
                                    navController.navigate("admin_dashboard")
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
                    }
                }
            }
        }
    }
}
