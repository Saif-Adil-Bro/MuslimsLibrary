package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.components.EmptyPlaceholder

@Composable
fun AuthorScreen(modifier: Modifier = Modifier) {
    EmptyPlaceholder(
        title = "🚧 শীঘ্রই আসছে...",
        message = "এখানে আপনি আমাদের সম্মানিত লেখকবৃন্দের পরিচিতি এবং তাদের রচিত সাহিত্যসমূহ খুঁজে পাবেন।",
        modifier = modifier.fillMaxSize()
    )
}
