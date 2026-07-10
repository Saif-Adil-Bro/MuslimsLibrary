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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission if Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        val navigateTo = intent?.getStringExtra("navigate_to")
        val bookId = intent?.getStringExtra("book_id")
        val postId = intent?.getStringExtra("post_id")

        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(this)
            )
            val theme by settingsViewModel.theme.collectAsState()

            MyApplicationTheme(theme = theme) {
                val appContainer = (application as MuslimsLibraryApplication).container
                MuslimsLibraryApp(
                    appContainer = appContainer,
                    settingsViewModel = settingsViewModel,
                    initialNavigateTo = navigateTo,
                    initialBookId = bookId,
                    initialPostId = postId
                )
            }
        }
    }
}
