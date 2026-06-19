package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActiveNavColor
import com.example.ui.theme.InactiveNavColor

@Composable
fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(15.dp)
            .background(Color.White)
            .navigationBarsPadding() // Protect against hardware overlay cutoffs
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavBarItem(
            icon = Icons.Default.Home,
            label = "হোম",
            isSelected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            testTag = "bottom_nav_home"
        )
        NavBarItem(
            icon = Icons.Default.GridView,
            label = "লাইব্রেরী",
            isSelected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            testTag = "bottom_nav_library"
        )
        NavBarItem(
            icon = Icons.Default.Forum,
            label = "ফোরাম",
            isSelected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            testTag = "bottom_nav_forum"
        )
        NavBarItem(
            icon = Icons.Default.Edit,
            label = "লেখক",
            isSelected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            testTag = "bottom_nav_author"
        )
        NavBarItem(
            icon = Icons.Default.Person,
            label = "প্রোফাইল",
            isSelected = selectedTab == 4,
            onClick = { onTabSelected(4) },
            testTag = "bottom_nav_profile"
        )
    }
}

@Composable
fun RowScope.NavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val tintColor = if (isSelected) ActiveNavColor else InactiveNavColor

    Column(
        modifier = modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = tintColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
