package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

@Composable
fun HighlightedText(
    fullText: String,
    query: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    normalColor: Color,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    val lowerText = fullText.lowercase()
    val lowerQuery = query.lowercase()
    val startIndex = lowerText.indexOf(lowerQuery)
    
    if (startIndex == -1 || query.isEmpty()) {
        Text(
            text = fullText,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = normalColor,
            modifier = modifier
        )
    } else {
        val before = fullText.substring(0, startIndex)
        val match = fullText.substring(startIndex, startIndex + query.length)
        val after = fullText.substring(startIndex + query.length)
        
        Row(modifier = modifier) {
            Text(before, fontSize = fontSize, fontWeight = fontWeight, color = normalColor)
            Text(match, fontSize = fontSize, fontWeight = FontWeight.Bold, color = highlightColor)
            Text(after, fontSize = fontSize, fontWeight = fontWeight, color = normalColor)
        }
    }
}
