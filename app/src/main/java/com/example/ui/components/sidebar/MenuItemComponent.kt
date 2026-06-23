package com.example.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val activeBackgroundColor = Color(0xFF667EEA).copy(alpha = 0.15f)
    val activeTextColor = Color(0xFF667EEA)
    val inactiveTextColor = Color(0xFF4A5568)
    val destructiveTextColor = Color(0xFFE53E3E)
    
    val currentTextColor = when {
        isSelected -> activeTextColor
        item.isDestructive -> destructiveTextColor
        else -> inactiveTextColor
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(if (isSelected) activeBackgroundColor else Color.Transparent)
            .clickable { onClick(item.id) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = currentTextColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = item.title,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = currentTextColor
        )
    }
}
