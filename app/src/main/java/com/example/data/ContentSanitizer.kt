package com.example.data

import java.util.regex.Pattern

object ContentSanitizer {
    fun sanitize(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }

    fun containsSuspiciousLinks(content: String): Boolean {
        val urlPattern = Pattern.compile(
            "(https?://)?([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?"
        )
        val matcher = urlPattern.matcher(content)
        
        var urlCount = 0
        while (matcher.find()) {
            urlCount++
        }
        return urlCount > 3
    }
}
