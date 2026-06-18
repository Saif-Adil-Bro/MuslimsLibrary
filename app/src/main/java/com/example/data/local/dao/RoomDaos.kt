package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.LocalBookProgress
import com.example.data.local.entities.LocalFavoriteBook
import com.example.data.local.entities.LocalPinnedBook
import com.example.data.local.entities.LocalUserNote
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM book_progress WHERE userId = :userId")
    suspend fun getAllProgressForUser(userId: String): List<LocalBookProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProgress(list: List<LocalBookProgress>)

    @Query("SELECT * FROM book_progress WHERE userId = :userId ORDER BY lastReadAt DESC")
    fun getAllProgressFlow(userId: String): Flow<List<LocalBookProgress>>

    @Query("SELECT * FROM book_progress WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    fun getProgressFlow(userId: String, bookId: String): Flow<LocalBookProgress?>

    @Query("SELECT * FROM book_progress WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    suspend fun getProgressOneShot(userId: String, bookId: String): LocalBookProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: LocalBookProgress)

    @Query("SELECT * FROM book_progress WHERE isSynced = 0")
    suspend fun getUnsyncedProgress(): List<LocalBookProgress>

    @Query("UPDATE book_progress SET isSynced = 1 WHERE id = :id")
    suspend fun markProgressSynced(id: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_books WHERE userId = :userId")
    suspend fun getAllFavoritesForUser(userId: String): List<LocalFavoriteBook>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFavorites(list: List<LocalFavoriteBook>)

    @Query("SELECT * FROM favorite_books WHERE userId = :userId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getFavoritesFlow(userId: String): Flow<List<LocalFavoriteBook>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_books WHERE userId = :userId AND bookId = :bookId AND isDeleted = 0)")
    fun isFavoriteFlow(userId: String, bookId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_books WHERE userId = :userId AND bookId = :bookId AND isDeleted = 0)")
    suspend fun isFavoriteOneShot(userId: String, bookId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: LocalFavoriteBook)

    @Query("UPDATE favorite_books SET isDeleted = 1, isSynced = 0 WHERE userId = :userId AND bookId = :bookId")
    suspend fun deleteFavoriteLocally(userId: String, bookId: String)

    @Query("SELECT * FROM favorite_books WHERE isSynced = 0")
    suspend fun getUnsyncedFavorites(): List<LocalFavoriteBook>

    @Query("UPDATE favorite_books SET isSynced = 1 WHERE id = :id")
    suspend fun markFavoriteSynced(id: String)

    @Delete
    suspend fun deleteFavorite(favorite: LocalFavoriteBook)

    @Query("DELETE FROM favorite_books WHERE id = :id")
    suspend fun removeFavoriteFromDb(id: String)
}

@Dao
interface PinDao {
    @Query("SELECT * FROM pinned_books WHERE userId = :userId")
    suspend fun getAllPinsForUser(userId: String): List<LocalPinnedBook>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPins(list: List<LocalPinnedBook>)

    @Query("SELECT * FROM pinned_books WHERE userId = :userId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getPinnedBooksFlow(userId: String): Flow<List<LocalPinnedBook>>

    @Query("SELECT EXISTS(SELECT 1 FROM pinned_books WHERE userId = :userId AND bookId = :bookId AND isDeleted = 0)")
    fun isPinnedFlow(userId: String, bookId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM pinned_books WHERE userId = :userId AND bookId = :bookId AND isDeleted = 0)")
    suspend fun isPinnedOneShot(userId: String, bookId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPin(pin: LocalPinnedBook)

    @Query("UPDATE pinned_books SET isDeleted = 1, isSynced = 0 WHERE userId = :userId AND bookId = :bookId")
    suspend fun unpinLocally(userId: String, bookId: String)

    @Query("SELECT * FROM pinned_books WHERE isSynced = 0")
    suspend fun getUnsyncedPins(): List<LocalPinnedBook>

    @Query("UPDATE pinned_books SET isSynced = 1 WHERE id = :id")
    suspend fun markPinSynced(id: String)

    @Delete
    suspend fun deletePin(pin: LocalPinnedBook)

    @Query("DELETE FROM pinned_books WHERE id = :id")
    suspend fun removePinFromDb(id: String)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM user_notes WHERE userId = :userId")
    suspend fun getAllNotesForUser(userId: String): List<LocalUserNote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotes(list: List<LocalUserNote>)

    @Query("SELECT * FROM user_notes WHERE userId = :userId AND bookId = :bookId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getNotesFlow(userId: String, bookId: String): Flow<List<LocalUserNote>>

    @Query("SELECT * FROM user_notes WHERE userId = :userId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getNotesForUserFlow(userId: String): Flow<List<LocalUserNote>>

    @Query("SELECT * FROM user_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): LocalUserNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: LocalUserNote)

    @Query("UPDATE user_notes SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun deleteNoteLocally(id: String)

    @Query("SELECT * FROM user_notes WHERE isSynced = 0")
    suspend fun getUnsyncedNotes(): List<LocalUserNote>

    @Query("UPDATE user_notes SET isSynced = 1 WHERE id = :id")
    suspend fun markNoteSynced(id: String)

    @Delete
    suspend fun deleteNote(note: LocalUserNote)

    @Query("DELETE FROM user_notes WHERE id = :id")
    suspend fun removeNoteFromDb(id: String)
}
