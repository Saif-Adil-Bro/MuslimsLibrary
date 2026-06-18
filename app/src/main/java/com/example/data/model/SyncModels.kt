package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseFavorite(
    val id: String, // "userId_bookId"
    @SerialName("user_id") val userId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabasePin(
    val id: String, // "userId_bookId"
    @SerialName("user_id") val userId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabaseNote(
    val id: String, // UUID
    @SerialName("user_id") val userId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("note_content") val noteContent: String,
    @SerialName("page_number") val pageNumber: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
