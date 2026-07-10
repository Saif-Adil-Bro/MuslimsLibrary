package com.example.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.sidebar.MenuItem

@Composable
fun MenuItemComponent(
    item: MenuItem,
    isSelected: Boolean,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val activeGradient = if (isDark) {
        listOf(Color(0xFF3A2E52), Color(0xFF2A1F40))
    } else {
        listOf(Color(0xFFF0E6FF), Color(0xFFE8DFF5))
    }
    val activeTextColor = if (isDark) Color(0xFFC0B2DE) else Color(0xFF6B5B95)
    
    val inactiveTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF333333)
    val inactiveIconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF666666)
    
    val destructiveTextColor = if (isDark) Color(0xFFF87171) else Color(0xFFE53E3E)
    
    val currentTextColor = when {
        isSelected -> activeTextColor
        item.isDestructive -> destructiveTextColor
        else -> inactiveTextColor
    }
    
    val currentIconColor = when {
        isSelected -> activeTextColor
        item.isDestructive -> destructiveTextColor
        else -> inactiveIconColor
    }

    val backgroundModifier = if (isSelected) {
        Modifier.background(Brush.linearGradient(colors = activeGradient))
    } else {
        Modifier.background(Color.Transparent)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(backgroundModifier)
            .clickable { onClick(item.id) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = currentIconColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = item.title,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = currentTextColor
        )
    }
}
