package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppGradientEnd
import com.example.ui.theme.AppGradientStart
import com.example.ui.theme.CategoryDefaultColor
import com.example.ui.theme.TextSecondary

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val shape = RoundedCornerShape(20.dp)
    
    val modifierWithShadow = if (isSelected) {
        modifier.shadow(4.dp, shape = shape)
    } else {
        modifier
    }

    Box(
        modifier = modifierWithShadow
            .clip(shape)
            .background(
                if (isSelected) {
                    Brush.linearGradient(
                        colors = listOf(AppGradientStart, AppGradientEnd)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(CategoryDefaultColor, CategoryDefaultColor)
                    )
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
