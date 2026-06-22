package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
    downloadedBooksViewModel: DownloadedBooksViewModel
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
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

            LaunchedEffect(shouldAutoRestore, userEmail) {
                if (shouldAutoRestore && userEmail != "User@muslimslibrary.org" && userEmail != "Guest User") {
                    profileViewModel.performRestore(userEmail)
                    authViewModel.onAutoRestoreHandled()
                }
            }

            // UI dialogs for Backup/Restore Process
            BackupRestoreDialogs(
                profileViewModel = profileViewModel,
                userEmail = userEmail
            )

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
                            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
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
                            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
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
    }
}
