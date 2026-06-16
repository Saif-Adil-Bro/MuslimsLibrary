package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val coverUrl: String,
    val pdfUrl: String,
    val category: String,
    val sizeMb: Double,
    val pages: Int
)
