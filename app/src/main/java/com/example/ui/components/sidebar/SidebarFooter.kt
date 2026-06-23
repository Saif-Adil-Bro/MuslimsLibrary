package com.example.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
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
    val footerGradient = listOf(Color(0xFFF0E6FF), Color(0xFFE8DFF5))
    
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
            color = Color(0xFF6B5B95),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = versionText,
            color = Color(0xFF888888),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
