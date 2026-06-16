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
                                    navController.navigate("reader/${book.id}/${book.title}")
                                },
                                onNavigateToAddBook = {
                                    navController.navigate("add_book")
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
                            ReaderScreen(
                                bookId = bookId,
                                bookTitle = bookTitle,
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
