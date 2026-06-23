package com.example.ui.components.sidebar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.sidebar.MenuSection

@Composable
fun MenuSectionComponent(
    section: MenuSection,
    activeItemId: String?,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        section.title?.let { title ->
            Text(
                text = title.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B5B95),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 15.dp, bottom = 10.dp)
            )
        }
        
        section.items.forEach { item ->
            MenuItemComponent(
                item = item,
                isSelected = item.id == activeItemId,
                onClick = onItemClick
            )
        }
    }
}
