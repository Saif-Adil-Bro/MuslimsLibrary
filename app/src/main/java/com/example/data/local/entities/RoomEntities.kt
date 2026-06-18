package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.BookProgress

@Entity(tableName = "book_progress")
data class LocalBookProgress(
    @PrimaryKey val id: String, // format: "userId_bookId" or UUID
    val userId: String,
    val bookId: String,
    val currentPage: Int,
    val totalPages: Int,
    val progressPercentage: Double,
    val status: String, // "reading" or "completed"
    val lastReadAt: Long,
    val isSynced: Boolean = false,
    val readingTimeSpent: Long = 0L, // in seconds
    val sessionCount: Int = 1
) {
    fun toSupabaseModel(): BookProgress {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val dateStr = sdf.format(java.util.Date(lastReadAt))
        return BookProgress(
            id = id,
            userId = userId,
            bookId = bookId,
            currentPage = currentPage,
            totalPages = totalPages,
            progressPercentage = progressPercentage,
            status = status,
            lastReadAt = dateStr
        )
    }

    companion object {
        fun fromSupabaseModel(model: BookProgress, isSynced: Boolean = true): LocalBookProgress {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val time = try {
                model.lastReadAt?.let { sdf.parse(it)?.time } ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
            return LocalBookProgress(
                id = model.id.ifEmpty { "${model.userId}_${model.bookId}" },
                userId = model.userId,
                bookId = model.bookId,
                currentPage = model.currentPage,
                totalPages = model.totalPages,
                progressPercentage = model.progressPercentage ?: 0.0,
                status = model.status,
                lastReadAt = time,
                isSynced = isSynced,
                readingTimeSpent = 0,
                sessionCount = 1
            )
        }
    }
}

@Entity(tableName = "favorite_books")
data class LocalFavoriteBook(
    @PrimaryKey val id: String, // format: "userId_bookId"
    val userId: String,
    val bookId: String,
    val timestamp: Long,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false // Track deleted state for offline sync deletions
)

@Entity(tableName = "pinned_books")
data class LocalPinnedBook(
    @PrimaryKey val id: String, // format: "userId_bookId"
    val userId: String,
    val bookId: String,
    val timestamp: Long,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false // Track deleted state for offline sync deletions
)

@Entity(tableName = "user_notes")
data class LocalUserNote(
    @PrimaryKey val id: String, // UUID
    val userId: String,
    val bookId: String,
    val noteContent: String,
    val pageNumber: Int? = null,
    val timestamp: Long,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false // Track soft deletion for offline syncing
)
