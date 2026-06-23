package com.example.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.sidebar.MenuSection
import com.example.ui.navigation.sidebar.SidebarViewModel
import com.example.ui.navigation.sidebar.UserProfile

@Composable
fun SidebarDrawer(
    profile: UserProfile,
    sections: List<MenuSection>,
    viewModel: SidebarViewModel,
    footerText: String,
    versionText: String,
    onMenuItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeItemId by viewModel.activeMenuItemId.collectAsState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(Color.White)
    ) {
        ProfileHeader(profile = profile)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 10.dp)
        ) {
            sections.forEachIndexed { index, section ->
                MenuSectionComponent(
                    section = section,
                    activeItemId = activeItemId,
                    onItemClick = { id ->
                        viewModel.setActiveMenuItem(id)
                        onMenuItemClick(id)
                    }
                )

                if (index < sections.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 20.dp),
                        color = Color(0xFFE0E0E0)
                    )
                }
            }
        }

        SidebarFooter(
            footerText = footerText,
            versionText = versionText
        )
    }
}
