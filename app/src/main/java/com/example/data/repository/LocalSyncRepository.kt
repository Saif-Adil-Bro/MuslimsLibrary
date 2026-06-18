package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entities.LocalBookProgress
import com.example.data.local.entities.LocalFavoriteBook
import com.example.data.local.entities.LocalPinnedBook
import com.example.data.local.entities.LocalUserNote
import com.example.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

interface LocalSyncRepository {
    // --- Book Progress ---
    fun getBookProgressFlow(userId: String, bookId: String): Flow<LocalBookProgress?>
    fun getAllProgressFlow(userId: String): Flow<List<LocalBookProgress>>
    suspend fun updateBookProgress(
        userId: String,
        bookId: String,
        currentPage: Int,
        totalPages: Int,
        status: String = "reading",
        additionalReadingTimeSeconds: Long = 0L
    )

    // --- Favorites ---
    fun getFavoritesFlow(userId: String): Flow<List<LocalFavoriteBook>>
    fun isFavoriteFlow(userId: String, bookId: String): Flow<Boolean>
    suspend fun toggleFavorite(userId: String, bookId: String)

    // --- Pins ---
    fun getPinnedBooksFlow(userId: String): Flow<List<LocalPinnedBook>>
    fun isPinnedFlow(userId: String, bookId: String): Flow<Boolean>
    suspend fun togglePin(userId: String, bookId: String)

    // --- User Notes ---
    fun getNotesFlow(userId: String, bookId: String): Flow<List<LocalUserNote>>
    fun getNotesForUserFlow(userId: String): Flow<List<LocalUserNote>>
    suspend fun addNote(userId: String, bookId: String, noteContent: String, pageNumber: Int? = null): String
    suspend fun deleteNote(id: String, userId: String)

    // --- Client Sync Hooks ---
    fun isSyncing(): Flow<Boolean>
    suspend fun syncNow(userId: String)
}

class LocalSyncRepositoryImpl(
    private val database: AppDatabase,
    private val syncManager: SyncManager
) : LocalSyncRepository {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun getBookProgressFlow(userId: String, bookId: String): Flow<LocalBookProgress?> {
        return database.progressDao().getProgressFlow(userId, bookId).map { progress ->
            if (progress != null && progress.status == "reading" && System.currentTimeMillis() - progress.lastReadAt > 7 * 24 * 3600 * 1000L) {
                val updated = progress.copy(status = "paused")
                coroutineScope.launch {
                    database.progressDao().insertProgress(updated)
                }
                updated
            } else {
                progress
            }
        }
    }

    override fun getAllProgressFlow(userId: String): Flow<List<LocalBookProgress>> {
        return database.progressDao().getAllProgressFlow(userId).map { list ->
            var changed = false
            val updated = list.map { progress ->
                if (progress.status == "reading" && System.currentTimeMillis() - progress.lastReadAt > 7 * 24 * 3600 * 1000L) {
                    changed = true
                    progress.copy(status = "paused")
                } else {
                    progress
                }
            }
            if (changed) {
                coroutineScope.launch {
                    updated.forEach { progress ->
                        if (progress.status == "paused") {
                            database.progressDao().insertProgress(progress)
                        }
                    }
                }
            }
            updated
        }
    }

    override suspend fun updateBookProgress(
        userId: String,
        bookId: String,
        currentPage: Int,
        totalPages: Int,
        status: String,
        additionalReadingTimeSeconds: Long
    ) {
        Log.d("Repository", "Updating progress for book: $bookId, page: $currentPage")
        val existing = database.progressDao().getProgressOneShot(userId, bookId)
        val oldTime = existing?.readingTimeSpent ?: 0L
        val accumulatedTime = oldTime + additionalReadingTimeSeconds
        
        val isNewSession = existing != null && (System.currentTimeMillis() - existing.lastReadAt > 30 * 60 * 1000L)
        val currentSessionCount = if (existing == null) 1 else if (isNewSession) existing.sessionCount + 1 else existing.sessionCount

        val targetStatus = if (currentPage >= totalPages || currentPage >= (totalPages * 0.95).toInt()) "completed" else status
        val targetPage = if (targetStatus == "completed") totalPages else currentPage
        val percentage = if (totalPages > 0) (targetPage.toDouble() / totalPages.toDouble()) * 100.0 else 0.0

        val localProgress = LocalBookProgress(
            id = "${userId}_$bookId",
            userId = userId,
            bookId = bookId,
            currentPage = targetPage,
            totalPages = totalPages,
            progressPercentage = percentage,
            status = targetStatus,
            lastReadAt = System.currentTimeMillis(),
            isSynced = false,
            readingTimeSpent = accumulatedTime,
            sessionCount = currentSessionCount
        )
        // 1. Instantly save locally in Room Database
        database.progressDao().insertProgress(localProgress)
        Log.d("Repository", "Database write result: SUCCESS (Progress updated in Room: Time=$accumulatedTime s, Sessions=$currentSessionCount)")
        Log.d("LocalSyncRepository", "Progress updated in Room (Time: $accumulatedTime s, Sessions: $currentSessionCount). Scheduling sync...")
        
        // 2. Trigger non-blocking background queue synchronization
        triggerBackgroundSync(userId)
    }

    override fun getFavoritesFlow(userId: String): Flow<List<LocalFavoriteBook>> {
        return database.favoriteDao().getFavoritesFlow(userId)
    }

    override fun isFavoriteFlow(userId: String, bookId: String): Flow<Boolean> {
        return database.favoriteDao().isFavoriteFlow(userId, bookId)
    }

    override suspend fun toggleFavorite(userId: String, bookId: String) {
        val id = "${userId}_$bookId"
        val existing = database.favoriteDao().isFavoriteOneShot(userId, bookId)
        if (existing) {
            // Delete locally
            database.favoriteDao().deleteFavoriteLocally(userId, bookId)
            Log.d("LocalSyncRepository", "Favorite marked as soft-deleted in Local Room. Scheduling push...")
        } else {
            // Insert favorite
            val newFavorite = LocalFavoriteBook(
                id = id,
                userId = userId,
                bookId = bookId,
                timestamp = System.currentTimeMillis(),
                isSynced = false,
                isDeleted = false
            )
            database.favoriteDao().insertFavorite(newFavorite)
            Log.d("LocalSyncRepository", "Favorite added in Local Room DB. Scheduling push...")
        }

        triggerBackgroundSync(userId)
    }

    override fun getPinnedBooksFlow(userId: String): Flow<List<LocalPinnedBook>> {
        return database.pinDao().getPinnedBooksFlow(userId)
    }

    override fun isPinnedFlow(userId: String, bookId: String): Flow<Boolean> {
        return database.pinDao().isPinnedFlow(userId, bookId)
    }

    override suspend fun togglePin(userId: String, bookId: String) {
        val id = "${userId}_$bookId"
        val existing = database.pinDao().isPinnedOneShot(userId, bookId)
        if (existing) {
            // Unpin locally (soft deletion tracker)
            database.pinDao().unpinLocally(userId, bookId)
            Log.d("LocalSyncRepository", "Pin marked for soft-deletion in Room. Scheduling push...")
        } else {
            val newPin = LocalPinnedBook(
                id = id,
                userId = userId,
                bookId = bookId,
                timestamp = System.currentTimeMillis(),
                isSynced = false,
                isDeleted = false
            )
            database.pinDao().insertPin(newPin)
            Log.d("LocalSyncRepository", "Pin added to Local Room. Scheduling push...")
        }

        triggerBackgroundSync(userId)
    }

    override fun getNotesFlow(userId: String, bookId: String): Flow<List<LocalUserNote>> {
        return database.noteDao().getNotesFlow(userId, bookId)
    }

    override fun getNotesForUserFlow(userId: String): Flow<List<LocalUserNote>> {
        return database.noteDao().getNotesForUserFlow(userId)
    }

    override suspend fun addNote(
        userId: String,
        bookId: String,
        noteContent: String,
        pageNumber: Int?
    ): String {
        val noteId = UUID.randomUUID().toString()
        val note = LocalUserNote(
            id = noteId,
            userId = userId,
            bookId = bookId,
            noteContent = noteContent,
            pageNumber = pageNumber,
            timestamp = System.currentTimeMillis(),
            isSynced = false,
            isDeleted = false
        )
        database.noteDao().insertNote(note)
        Log.d("LocalSyncRepository", "Note $noteId saved in Room. Scheduling push sync...")
        triggerBackgroundSync(userId)
        return noteId
    }

    override suspend fun deleteNote(id: String, userId: String) {
        database.noteDao().deleteNoteLocally(id)
        Log.d("LocalSyncRepository", "Note $id marked for deletion locally. Scheduling push sync...")
        triggerBackgroundSync(userId)
    }

    override fun isSyncing(): Flow<Boolean> {
        return syncManager.isSyncing
    }

    override suspend fun syncNow(userId: String) {
        syncManager.syncNow(userId)
    }

    private fun triggerBackgroundSync(userId: String) {
        coroutineScope.launch {
            try {
                syncManager.syncNow(userId)
            } catch (e: Exception) {
                Log.e("LocalSyncRepository", "Asynchronous automatic sync triggered failure: ${e.message}")
            }
        }
    }
}
