package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.components.EmptyPlaceholder

@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    EmptyPlaceholder(
        title = "🚧 শীঘ্রই আসছে...",
        message = "এখানে আপনি আপনার সমস্ত বইয়ের ক্যাটাগরি এবং লাইব্রেরির বিশাল কালেকশন দেখতে পাবেন।",
        modifier = modifier.fillMaxSize()
    )
}
