package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppGradientEnd
import com.example.ui.theme.AppGradientStart

@Composable
fun CategoryPill(
    categoryName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = if (isSelected) {
        Brush.linearGradient(
            colors = listOf(AppGradientStart, AppGradientEnd)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFF0F2F5), Color(0xFFF0F2F5))
        )
    }

    val textColor = if (isSelected) Color.White else Color(0xFF4A5568)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("category_pill_${categoryName}")
    ) {
        Text(
            text = categoryName,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
