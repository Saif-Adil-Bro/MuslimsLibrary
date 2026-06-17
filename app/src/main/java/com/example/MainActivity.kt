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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AddBookScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.LibraryViewModel
import androidx.compose.runtime.LaunchedEffect

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
                    factory = LibraryViewModel.Factory(appContainer.bookRepository)
                )
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(appContainer.supabaseService)
                )
                val adminViewModel: AdminViewModel = viewModel(
                    factory = AdminViewModel.Factory(appContainer.supabaseClient, appContainer.supabaseService)
                )
                
                val navController = rememberNavController()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                
                val toastMessage by authViewModel.toastMessage.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current
                LaunchedEffect(Unit) {
                    authViewModel.initDebugMode(context)
                }
                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
                        authViewModel.clearToastMessage()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                            val userEmail = authViewModel.uiState.collectAsState().value.let { state ->
                                if (state is com.example.ui.viewmodel.AuthState.Success) state.email else "User@muslimslibrary.org"
                            }
                            val userRole by authViewModel.userRole.collectAsState()
                            val debugInfo by authViewModel.debugInfo.collectAsState()
                            val isDebugMode by authViewModel.isDebugMode.collectAsState()
                            DashboardScreen(
                                libraryViewModel = libraryViewModel,
                                homeViewModel = homeViewModel,
                                adminViewModel = adminViewModel,
                                userEmail = userEmail,
                                userRole = userRole,
                                onLogoutClick = {
                                    authViewModel.logout()
                                    navController.navigate("auth") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
                                onBookClick = { book ->
                                    val encodedTitle = java.net.URLEncoder.encode(book.title, "UTF-8")
                                    val encodedUrl = java.net.URLEncoder.encode(book.fileUrl ?: "", "UTF-8")
                                    navController.navigate("reader/${book.id}/$encodedTitle/$encodedUrl/${book.fileType}")
                                },
                                onNavigateToAddBook = {
                                    navController.navigate("add_book")
                                },
                                debugInfo = debugInfo,
                                isDebugMode = isDebugMode,
                                onToggleDebug = { authViewModel.toggleDebugMode(context) }
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
                            ReaderScreen(
                                bookId = bookId,
                                bookTitle = bookTitle,
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
                            ReaderScreen(
                                bookId = bookId,
                                bookTitle = bookTitle,
                                fileUrl = fileUrl,
                                fileType = fileType,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
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
                    }
                }
            }
        }
    }
}
