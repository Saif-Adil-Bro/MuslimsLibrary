package com.example.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SidebarFooter(
    footerText: String,
    versionText: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val footerGradient = if (isDark) {
        listOf(Color(0xFF2E1E3F), Color(0xFF1A1225))
    } else {
        listOf(Color(0xFFF0E6FF), Color(0xFFE8DFF5))
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(colors = footerGradient))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = footerText,
            color = if (isDark) Color(0xFFC0B2DE) else Color(0xFF6B5B95),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = versionText,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF888888),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
