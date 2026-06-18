package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_books")
data class DownloadedBook(
    @PrimaryKey val id: String, // Can be bookId
    val bookId: String,
    val title: String,
    val author: String,
    val coverImageUrl: String?,
    val localFilePath: String,
    val fileSize: Long,
    val downloadDate: Long,
    val lastOpenedDate: Long?,
    val downloadStatus: String, // "completed", "downloading", "failed", "paused"
    val progress: Int = 0 // Download progress percentage (0-100)
)
