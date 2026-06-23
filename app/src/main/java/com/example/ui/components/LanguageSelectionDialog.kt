package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable

@Composable
fun LanguageSelectionDialog(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        Triple("bn", "বাংলা", Icons.Default.Language),
        Triple("en", "English", Icons.Default.Translate),
        Triple("ar", "العربية", Icons.Default.ArrowRight),
        Triple("ur", "اردو", Icons.Default.TextFields)
    )
    
    GenericSettingsDialog(
        title = "ভাষা নির্বাচন করুন",
        options = languages,
        selectedOption = languages.find { it.first == selectedLanguage } ?: languages[0],
        onOptionSelected = { lang ->
            onLanguageSelected(lang.first)
        },
        onDismiss = onDismiss,
        optionLabel = { it.second },
        optionIcon = { it.third }
    )
}
