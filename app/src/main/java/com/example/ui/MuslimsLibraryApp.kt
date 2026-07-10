package com.example.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.AppContainer
import com.example.ui.components.AuthRestoringDialog
import com.example.ui.navigation.AppNavigation
import com.example.ui.viewmodel.AdminViewModel
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
fun MuslimsLibraryApp(
    appContainer: AppContainer,
    settingsViewModel: SettingsViewModel,
    initialNavigateTo: String? = null,
    initialBookId: String? = null,
    initialPostId: String? = null
) {
    val context = LocalContext.current
    
    var navigateTo by remember { mutableStateOf(initialNavigateTo) }
    var bookId by remember { mutableStateOf(initialBookId) }
    var postId by remember { mutableStateOf(initialPostId) }

    // Initialize ViewModels inside the Composition root
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModel.Factory(
            appContainer.supabaseService,
            appContainer.guestModeManager,
            context,
            appContainer.appDatabase,
            appContainer.authRepository
        )
    )
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(
            appContainer.authRepository,
            appContainer.backupManager
        )
    )
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(
            appContainer.appDatabase, 
            appContainer.supabaseService
        )
    )
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            appContainer.supabaseService, 
            appContainer.localSyncRepository
        )
    )
    val adminViewModel: AdminViewModel = viewModel(
        factory = AdminViewModel.Factory(
            appContainer.supabaseClient, 
            appContainer.supabaseService,
            appContainer.context
        )
    )
    val forumViewModel: ForumViewModel = viewModel(
        factory = ForumViewModel.Factory(
            appContainer.supabaseService, 
            appContainer.guestModeManager
        )
    )
    val authorViewModel: AuthorViewModel = viewModel(
        factory = AuthorViewModel.Factory(
            appContainer.supabaseService
        )
    )
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(
            appContainer.authRepository,
            appContainer.supabaseService,
            appContainer.backupManager,
            appContainer.appDatabase
        )
    )
    val downloadedBooksViewModel: DownloadedBooksViewModel = viewModel(
        factory = DownloadedBooksViewModel.Factory(
            appContainer.appDatabase,
            appContainer.downloadManager,
            appContainer.supabaseService,
            context
        )
    )

    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val toastMessage by authViewModel.toastMessage.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    AuthRestoringDialog(authState = authState)

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
        AppNavigation(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding),
            appContainer = appContainer,
            authViewModel = authViewModel,
            notificationViewModel = notificationViewModel,
            libraryViewModel = libraryViewModel,
            homeViewModel = homeViewModel,
            adminViewModel = adminViewModel,
            forumViewModel = forumViewModel,
            authorViewModel = authorViewModel,
            profileViewModel = profileViewModel,
            downloadedBooksViewModel = downloadedBooksViewModel,
            settingsViewModel = settingsViewModel,
            navigateTo = navigateTo,
            bookId = bookId,
            postId = postId,
            onNotificationHandled = {
                navigateTo = null
                bookId = null
                postId = null
            }
        )
    }
}
