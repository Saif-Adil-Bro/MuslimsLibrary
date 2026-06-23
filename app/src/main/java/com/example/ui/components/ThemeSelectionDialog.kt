package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable

@Composable
fun ThemeSelectionDialog(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf(
        Triple("light", "লাইট মোড", Icons.Default.LightMode),
        Triple("dark", "ডার্ক মোড", Icons.Default.DarkMode),
        Triple("system", "সিস্টেম ডিফল্ট", Icons.Default.Settings)
    )
    
    GenericSettingsDialog(
        title = "থিম পরিবর্তন করুন",
        options = themes,
        selectedOption = themes.find { it.first == selectedTheme } ?: themes[1],
        onOptionSelected = { theme ->
            onThemeSelected(theme.first)
        },
        onDismiss = onDismiss,
        optionLabel = { it.second },
        optionIcon = { it.third }
    )
}
