package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.MuslimsLibraryApp
import com.example.ui.theme.MyApplicationTheme

import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val navigateToFlow = MutableStateFlow<String?>(null)
    private val bookIdFlow = MutableStateFlow<String?>(null)
    private val postIdFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission if Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        handleIntent(intent)

        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(this)
            )
            val theme by settingsViewModel.theme.collectAsState()

            val navigateTo by navigateToFlow.collectAsState()
            val bookId by bookIdFlow.collectAsState()
            val postId by postIdFlow.collectAsState()

            MyApplicationTheme(theme = theme) {
                val appContainer = (application as MuslimsLibraryApplication).container
                MuslimsLibraryApp(
                    appContainer = appContainer,
                    settingsViewModel = settingsViewModel,
                    initialNavigateTo = navigateTo,
                    initialBookId = bookId,
                    initialPostId = postId,
                    onNotificationHandled = {
                        navigateToFlow.value = null
                        bookIdFlow.value = null
                        postIdFlow.value = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        intent?.let {
            navigateToFlow.value = it.getStringExtra("navigate_to")
            bookIdFlow.value = it.getStringExtra("book_id")
            postIdFlow.value = it.getStringExtra("post_id")
        }
    }
}
