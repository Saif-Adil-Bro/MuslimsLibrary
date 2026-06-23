package com.example.ui.navigation.sidebar

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SidebarViewModel : ViewModel() {
    private val _activeMenuItemId = MutableStateFlow<String?>(null)
    val activeMenuItemId: StateFlow<String?> = _activeMenuItemId.asStateFlow()

    fun setActiveMenuItem(id: String) {
        _activeMenuItemId.update { id }
    }
}
