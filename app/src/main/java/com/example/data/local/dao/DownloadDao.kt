package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.DownloadedBook
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloaded_books WHERE downloadStatus = 'completed' ORDER BY downloadDate DESC")
    suspend fun getAllDownloadedBooks(): List<DownloadedBook>

    @Query("SELECT * FROM downloaded_books WHERE downloadStatus = 'completed' ORDER BY downloadDate DESC")
    fun getAllDownloadedBooksFlow(): Flow<List<DownloadedBook>>

    @Query("SELECT * FROM downloaded_books ORDER BY downloadDate DESC")
    fun getAllBooksFlow(): Flow<List<DownloadedBook>>

    @Query("SELECT * FROM downloaded_books WHERE bookId = :bookId LIMIT 1")
    suspend fun getDownloadedBook(bookId: String): DownloadedBook?

    @Query("SELECT * FROM downloaded_books WHERE bookId = :bookId LIMIT 1")
    fun getDownloadedBookFlow(bookId: String): Flow<DownloadedBook?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedBook(book: DownloadedBook)

    @Query("DELETE FROM downloaded_books WHERE bookId = :bookId")
    suspend fun deleteDownloadedBook(bookId: String)

    @Query("SELECT COUNT(*) FROM downloaded_books WHERE downloadStatus = 'completed'")
    suspend fun getDownloadedBooksCount(): Int
}
