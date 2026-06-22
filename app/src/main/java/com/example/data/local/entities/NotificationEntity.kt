package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String, // "book", "forum", "system", "push"
    val data: String, // JSON data represent Map<String, String>
    val isRead: Boolean,
    val createdAt: Long
)
