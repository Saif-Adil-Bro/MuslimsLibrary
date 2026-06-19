package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
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
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AuthorSuggestionItem(
    suggestion: AdminViewModel.AuthorSuggestion,
    query: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Author icon with initial letter
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = suggestion.author.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            // Author name with highlighted match
            HighlightedText(
                fullText = suggestion.author.name,
                query = query,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                normalColor = Color(0xFF2D3748),
                highlightColor = Color(0xFF667EEA)
            )
            
            // Book count
            Text(
                text = "${suggestion.bookCount}টি বই রয়েছে",
                fontSize = 12.sp,
                color = Color(0xFF718096)
            )
        }
        
        // Bio indicator
        if (!suggestion.author.bio.isNullOrBlank()) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Has bio",
                tint = Color(0xFF667EEA),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
