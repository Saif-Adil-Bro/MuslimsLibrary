package com.example.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.sidebar.UserProfile

@Composable
fun ProfileHeader(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val headerGradient = listOf(Color(0xFFF0E6FF), Color(0xFFE8DFF5))
    val avatarGradient = listOf(Color(0xFF6B5B95), Color(0xFF5A4A85))
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(colors = headerGradient))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        ) {
            // Circular Avatar
            Box(
                modifier = Modifier
                    .size(65.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    .background(Brush.linearGradient(colors = avatarGradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.initials,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Profile info
            Column {
                Text(
                    text = profile.name,
                    color = Color(0xFF2D2D2D),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.email,
                    color = Color(0xFF666666),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
