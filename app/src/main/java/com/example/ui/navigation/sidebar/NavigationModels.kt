package com.example.ui.navigation.sidebar

import androidx.compose.ui.graphics.vector.ImageVector

data class UserProfile(
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val initials: String = ""
)

data class MenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val isDestructive: Boolean = false
)

data class MenuSection(
    val title: String? = null,
    val items: List<MenuItem>
)
