package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.core.content.edit
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.example.AppContainer
import com.example.ui.components.BackupRestoreDialogs
import com.example.ui.screens.AddBookScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AllBooksScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BookDetailScreen
import com.example.ui.screens.BookReaderScreen
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DownloadedBooksScreen
import com.example.ui.screens.EditProfileScreen
import com.example.ui.screens.ForumScreen
import com.example.ui.screens.NotificationCenterScreen
import com.example.ui.screens.NotificationSettingsScreen
import com.example.ui.screens.PostDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.AuthorViewModel
import com.example.ui.viewmodel.DownloadedBooksViewModel
import com.example.ui.viewmodel.ForumViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.NotificationViewModel
import com.example.ui.viewmodel.ProfileViewModel

import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    appContainer: AppContainer,
    authViewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel,
    libraryViewModel: LibraryViewModel,
    homeViewModel: HomeViewModel,
    adminViewModel: AdminViewModel,
    forumViewModel: ForumViewModel,
    authorViewModel: AuthorViewModel,
    profileViewModel: ProfileViewModel,
    downloadedBooksViewModel: DownloadedBooksViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("splash") {
            val isAutoGuestLogin by authViewModel.isAutoGuestLogin.collectAsState()
            val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
            val isInitialCheckDone by authViewModel.isInitialCheckDone.collectAsState()

            if (isAutoGuestLogin || !isInitialCheckDone) {
                // Loading animation দেখান
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFF667EEA),
                            modifier = Modifier.size(64.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "অ্যাপ লোড হচ্ছে...",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            } else if (isLoggedIn) {
                // Auto-login successful, go to dashboard
                LaunchedEffect(Unit) {
                    navController.navigate("dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            } else {
                // Auto-login failed, go to auth
                LaunchedEffect(Unit) {
                    navController.navigate("auth") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }

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

        composable("dashboard") {
            val authState = authViewModel.uiState.collectAsState().value
            val userEmail = if (authState is AuthState.Success) authState.email else "User@muslimslibrary.org"
            val userUid = if (authState is AuthState.Success) authState.uid else ""
            val userRole by authViewModel.userRole.collectAsState()
            val debugInfo by authViewModel.debugInfo.collectAsState()
            val isDebugMode by authViewModel.isDebugMode.collectAsState()
            val unreadNotificationsCount by notificationViewModel.unreadCount.collectAsState()
            val shouldAutoRestore by authViewModel.shouldAutoRestore.collectAsState()
            
            LaunchedEffect(shouldAutoRestore, userUid) {
                if (shouldAutoRestore && userUid.isNotBlank() && userEmail != "User@muslimslibrary.org" && userEmail != "Guest User") {
                    profileViewModel.performRestore(userId = userUid, localRoomUserId = userEmail)
                    authViewModel.onAutoRestoreHandled()
                }
            }
            
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
                            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit {clear()}
                        } catch (e: Exception) {
                            android.util.Log.e("AppNavigation", "Error clearing preferences on logout", e)
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
                onNavigateToCategory = {
                    navController.navigate("category")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToAbout = {
                    navController.navigate("about")
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate("privacy_policy")
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

            // UI dialogs for Backup/Restore Process
            BackupRestoreDialogs(
                profileViewModel = profileViewModel,
                userId = userUid
            )

        }

        composable("category") {
            com.example.ui.screens.CategoryScreen(
                viewModel = homeViewModel,
                onBackClick = { navController.popBackStack() },
                onBookClick = { book ->
                    navController.navigate("book_detail/${book.id}")
                },
                onCategoryClick = { /* handled internally currently by CategoryScreen */ }
            )
        }

        composable("settings") {
            com.example.ui.screens.SettingsScreen(
                settingsViewModel = settingsViewModel,
                profileViewModel = profileViewModel,
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() },
                onNotificationSettingsClick = { navController.navigate("notification_settings") },
                onBackupClick = {
                    val authState = authViewModel.uiState.value
                    if (authState is AuthState.Success) {
                        profileViewModel.performBackup(userId = authState.uid, localRoomUserId = authState.email)
                    }
                },
                onRestoreClick = {
                    val authState = authViewModel.uiState.value
                    if (authState is AuthState.Success) {
                        profileViewModel.performRestore(userId = authState.uid, localRoomUserId = authState.email)
                    }
                }
            )
        }

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

        composable("create_post") {
            val authState = authViewModel.uiState.collectAsState().value
            val userEmail = if (authState is AuthState.Success) authState.email else "User@muslimslibrary.org"
            val userUid = if (authState is AuthState.Success) authState.uid else ""
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

        composable(
            route = "post_detail/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val authState = authViewModel.uiState.collectAsState().value
            val userEmail = if (authState is AuthState.Success) authState.email else "User@muslimslibrary.org"
            val userUid = if (authState is AuthState.Success) authState.uid else ""
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

        composable(
            route = "book_detail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val userEmail = authViewModel.uiState.collectAsState().value.let { state ->
                if (state is AuthState.Success) state.email else "User@muslimslibrary.org"
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
                onBookClick = { suggestedBook ->
                    navController.navigate("book_detail/${suggestedBook.id}") {
                        popUpTo("dashboard")
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

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
                if (state is AuthState.Success) state.email else "User@muslimslibrary.org"
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
                if (state is AuthState.Success) state.email else "User@muslimslibrary.org"
            }
            if (fileType.lowercase() == "epub") {
                com.example.ui.screens.EpubReaderScreen(
                    bookId = bookId,
                    bookTitle = bookTitle,
                    fileUrl = fileUrl,
                    fileType = fileType,
                    userId = userEmail,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            } else {
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
        }

        composable("admin_dashboard") {
            val authState = authViewModel.uiState.collectAsState().value
            val userEmail = if (authState is AuthState.Success) authState.email else ""
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
                            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit {clear()}
                        } catch (e: Exception) {
                            android.util.Log.e("AppNavigation", "Error clearing preferences on logout", e)
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

        composable("profile") {
            val authState = authViewModel.uiState.collectAsState().value
            val userEmail = if (authState is AuthState.Success) authState.email else ""
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
                            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit {clear()}
                        } catch (e: Exception) {
                            android.util.Log.e("AppNavigation", "Error clearing preferences on logout", e)
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

        composable("edit_profile") {
            EditProfileScreen(
                viewModel = profileViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("downloaded_books") {
            DownloadedBooksScreen(
                viewModel = downloadedBooksViewModel,
                onBookClick = { bookId ->
                    val dbBook = downloadedBooksViewModel.downloadedBooks.value.find { it.bookId == bookId }
                    val encodedTitle = java.net.URLEncoder.encode(dbBook?.title ?: "বই", "UTF-8")
                    val encodedPath = java.net.URLEncoder.encode(dbBook?.localFilePath ?: "", "UTF-8")
                    val fileType = if (dbBook?.localFilePath?.endsWith(".epub", ignoreCase = true) == true) "epub" else "pdf"
                    navController.navigate("reader/$bookId/$encodedTitle/$encodedPath/$fileType")
                },
                onBackClick = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }

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
        
        composable("about") {
            com.example.ui.screens.AboutScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable("privacy_policy") {
            com.example.ui.screens.PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
