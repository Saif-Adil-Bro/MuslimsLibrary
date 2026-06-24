package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    // Unescape basic HTML entities first
    val unescapedText = text
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#x27;", "'")

    val parts = unescapedText.split("```")
    
    Column(modifier = modifier) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = parseInlineMarkdown(part.trim('\n')),
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (fontSize != TextUnit.Unspecified) fontSize * 0.9f else 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = lineHeight,
                            maxLines = maxLines,
                            overflow = overflow
                        )
                    }
                }
            } else {
                // Normal text (parse bold and italic)
                if (part.isNotEmpty()) {
                    SelectionContainer {
                        Text(
                            text = parseInlineMarkdown(part),
                            color = color,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            maxLines = maxLines,
                            overflow = overflow
                        )
                    }
                }
            }
        }
    }
}

fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        // Match **bold**, *italic*, <b>, <strong>, <i>, <em>, <a>
        val pattern = Regex(
            "\\*\\*(.*?)\\*\\*|\\*(.*?)\\*|<b>(.*?)</b>|<strong>(.*?)</strong>|<i>(.*?)</i>|<em>(.*?)</em>|<a[^>]*>(.*?)</a>",
            RegexOption.IGNORE_CASE
        )
        val matches = pattern.findAll(text)
        
        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1
            
            // Append text before match
            if (start > currentIndex) {
                append(text.substring(currentIndex, start))
            }
            
            when {
                match.groups[1] != null -> { // **bold**
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(match.groups[1]!!.value)
                    pop()
                }
                match.groups[2] != null -> { // *italic*
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(match.groups[2]!!.value)
                    pop()
                }
                match.groups[3] != null -> { // <b>
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(match.groups[3]!!.value)
                    pop()
                }
                match.groups[4] != null -> { // <strong>
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(match.groups[4]!!.value)
                    pop()
                }
                match.groups[5] != null -> { // <i>
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(match.groups[5]!!.value)
                    pop()
                }
                match.groups[6] != null -> { // <em>
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(match.groups[6]!!.value)
                    pop()
                }
                match.groups[7] != null -> { // <a>
                    pushStyle(SpanStyle(color = Color(0xFF1E88E5), textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                    append(match.groups[7]!!.value)
                    pop()
                }
            }
            
            currentIndex = end
        }
        
        // Append remaining text
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}
