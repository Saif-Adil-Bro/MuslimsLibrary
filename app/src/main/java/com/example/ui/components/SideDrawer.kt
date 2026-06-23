package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppGradientEnd
import com.example.ui.theme.AppGradientStart
import com.example.ui.theme.TextPrimary

@Composable
fun SideDrawer(
    userEmail: String,
    onCloseClick: () -> Unit,
    onMenuItemClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    userDisplayName: String = "ব্যবহারকারী",
    userRole: String = "",
    isGuestMode: Boolean = false
) {
    val initialLetter = if (userDisplayName.isNotBlank()) {
        userDisplayName.first().uppercase()
    } else {
        "U"
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color.White)
    ) {
        // Drawer Header with Gradient and Avatar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppGradientStart, AppGradientEnd)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Dismiss button (✕)
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .testTag("drawer_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Drawer",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                // Circular user avatar
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialLetter,
                        color = AppGradientStart,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Profile name & metadata
                Column {
                    Text(
                        text = userDisplayName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = userEmail.ifBlank { "user@example.com" },
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // Drawer Menu Items Scroll area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            DrawerMenuItem(
                icon = Icons.Default.Home,
                label = "হোম",
                onClick = { onMenuItemClick("home") },
                testTag = "drawer_item_home"
            )
            DrawerMenuItem(
                icon = Icons.Default.Book,
                label = "আমার বই",
                onClick = { onMenuItemClick("my_books") },
                testTag = "drawer_item_my_books"
            )
            DrawerMenuItem(
                icon = Icons.Default.Category,
                label = "ক্যাটাগরি",
                onClick = { onMenuItemClick("category") },
                testTag = "drawer_item_category"
            )
            DrawerMenuItem(
                icon = Icons.Default.Download,
                label = "ডাউনলোড",
                onClick = { onMenuItemClick("downloads") },
                testTag = "drawer_item_downloads"
            )
            DrawerMenuItem(
                icon = Icons.Default.Favorite,
                label = "প্রিয় বই",
                onClick = { onMenuItemClick("favorite_books") },
                testTag = "drawer_item_favorite_books"
            )
            DrawerMenuItem(
                icon = Icons.Default.Settings,
                label = "সেটিংস",
                onClick = { onMenuItemClick("settings") },
                testTag = "drawer_item_settings"
            )
            DrawerMenuItem(
                icon = Icons.Default.Info,
                label = "সাহায্য",
                onClick = { onMenuItemClick("help") },
                testTag = "drawer_item_help"
            )
            if (userRole.equals("admin", ignoreCase = true)) {
                DrawerMenuItem(
                    icon = Icons.Default.Security,
                    label = "অ্যাডমিন প্যানেল (Admin)",
                    onClick = { onMenuItemClick("admin_panel") },
                    testTag = "drawer_item_admin"
                )
            }
            if (!isGuestMode) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = Color(0xFFF1F1F1)
                )
                DrawerMenuItem(
                    icon = Icons.Default.ExitToApp,
                    label = "লগআউট",
                    onClick = onLogoutClick,
                    testTag = "drawer_item_logout",
                    textColor = Color(0xFFE53E3E)
                )
            }
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (textColor == TextPrimary) Color(0xFF4A5568) else textColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
