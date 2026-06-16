package com.example.data.repository

import com.example.data.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getBooks(): Flow<List<Book>>
    suspend fun getBookById(id: String): Book?
    suspend fun searchBooks(query: String): List<Book>
    suspend fun downloadBookFile(pdfUrl: String): ByteArray
}
