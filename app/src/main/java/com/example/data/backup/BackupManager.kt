package com.example.data.backup

import android.content.Context
import com.example.data.SupabaseService
import com.example.data.local.AppDatabase
import com.example.data.local.entities.LocalBookProgress
import com.example.data.local.entities.LocalFavoriteBook
import com.example.data.local.entities.LocalPinnedBook
import com.example.data.local.entities.LocalUserNote
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class BackupBookProgress(
    val id: String,
    val userId: String,
    val bookId: String,
    val currentPage: Int,
    val totalPages: Int,
    val progressPercentage: Double,
    val status: String,
    val lastReadAt: Long,
    val readingTimeSpent: Long = 0L,
    val sessionCount: Int = 1
)

@Serializable
data class BackupFavoriteBook(
    val id: String,
    val userId: String,
    val bookId: String,
    val timestamp: Long
)

@Serializable
data class BackupPinnedBook(
    val id: String,
    val userId: String,
    val bookId: String,
    val timestamp: Long
)

@Serializable
data class BackupUserNote(
    val id: String,
    val userId: String,
    val bookId: String,
    val noteContent: String,
    val pageNumber: Int? = null,
    val timestamp: Long
)

@Serializable
data class FullBackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val progress: List<BackupBookProgress>,
    val favorites: List<BackupFavoriteBook>,
    val pins: List<BackupPinnedBook>,
    val notes: List<BackupUserNote>
)

class BackupManager(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val supabaseService: SupabaseService,
    private val firebaseAuth: FirebaseAuth
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun uploadBackup() {
        val userId = firebaseAuth.currentUser?.uid 
            ?: throw SecurityException("User not authenticated")
        if (userId.length < 10) {
            throw SecurityException("Invalid user ID")
        }
        uploadBackupToCloud(userId)
    }

    suspend fun downloadBackup() {
        val userId = firebaseAuth.currentUser?.uid 
            ?: throw SecurityException("User not authenticated")
        if (userId.length < 10) {
            throw SecurityException("Invalid user ID")
        }
        val exists = backupExistsOnCloud(userId)
        if (!exists) {
            throw Exception("No backup found for this account")
        }
        val success = downloadAndRestoreFromCloud(userId)
        if (!success) {
            throw Exception("No backup found for this account")
        }
    }

    // 1. Export Room data to JSON file
    suspend fun exportDataToJson(userId: String): File = withContext(Dispatchers.IO) {
        val progress = appDatabase.progressDao().getAllProgressForUser(userId).map {
            BackupBookProgress(
                id = it.id,
                userId = it.userId,
                bookId = it.bookId,
                currentPage = it.currentPage,
                totalPages = it.totalPages,
                progressPercentage = it.progressPercentage,
                status = it.status,
                lastReadAt = it.lastReadAt,
                readingTimeSpent = it.readingTimeSpent,
                sessionCount = it.sessionCount
            )
        }

        val favorites = appDatabase.favoriteDao().getAllFavoritesForUser(userId).map {
            BackupFavoriteBook(
                id = it.id,
                userId = it.userId,
                bookId = it.bookId,
                timestamp = it.timestamp
            )
        }

        val pins = appDatabase.pinDao().getAllPinsForUser(userId).map {
            BackupPinnedBook(
                id = it.id,
                userId = it.userId,
                bookId = it.bookId,
                timestamp = it.timestamp
            )
        }

        val notes = appDatabase.noteDao().getAllNotesForUser(userId).map {
            BackupUserNote(
                id = it.id,
                userId = it.userId,
                bookId = it.bookId,
                noteContent = it.noteContent,
                pageNumber = it.pageNumber,
                timestamp = it.timestamp
            )
        }

        val backupData = FullBackupData(
            version = 1,
            timestamp = System.currentTimeMillis(),
            progress = progress,
            favorites = favorites,
            pins = pins,
            notes = notes
        )

        val jsonString = json.encodeToString(FullBackupData.serializer(), backupData)
        val file = File(context.cacheDir, "backup.json")
        file.writeText(jsonString)
        file
    }

    // 2. Upload JSON to Supabase Storage
    suspend fun uploadBackupToCloud(userId: String): Unit = withContext(Dispatchers.IO) {
        val localFile = exportDataToJson(userId)
        val storagePath = "$userId/backup.json"
        
        // Use user_backups bucket
        supabaseService.uploadBackupFile(storagePath, localFile)
        
        // Clean up cached file
        try {
            if (localFile.exists()) {
                localFile.delete()
            }
        } catch (e: Exception) {
            // ignore cleanup failure
        }
    }

    // 3. Download and Restore
    suspend fun downloadAndRestoreFromCloud(userId: String): Boolean = withContext(Dispatchers.IO) {
        val storagePath = "$userId/backup.json"
        val localFile = File(context.cacheDir, "temp_restore.json")
        
        try {
            // Download content
            supabaseService.downloadBackupFile(storagePath, localFile)
            
            if (!localFile.exists()) return@withContext false
            
            val jsonString = localFile.readText()
            val backupData = json.decodeFromString(FullBackupData.serializer(), jsonString)
            
            // Map backup objects back to Room entity models
            val localProgress = backupData.progress.map {
                LocalBookProgress(
                    id = it.id,
                    userId = it.userId,
                    bookId = it.bookId,
                    currentPage = it.currentPage,
                    totalPages = it.totalPages,
                    progressPercentage = it.progressPercentage,
                    status = it.status,
                    lastReadAt = it.lastReadAt,
                    isSynced = false,
                    readingTimeSpent = it.readingTimeSpent,
                    sessionCount = it.sessionCount
                )
            }

            val localFavorites = backupData.favorites.map {
                LocalFavoriteBook(
                    id = it.id,
                    userId = it.userId,
                    bookId = it.bookId,
                    timestamp = it.timestamp,
                    isSynced = false,
                    isDeleted = false
                )
            }

            val localPins = backupData.pins.map {
                LocalPinnedBook(
                    id = it.id,
                    userId = it.userId,
                    bookId = it.bookId,
                    timestamp = it.timestamp,
                    isSynced = false,
                    isDeleted = false
                )
            }

            val localNotes = backupData.notes.map {
                LocalUserNote(
                    id = it.id,
                    userId = it.userId,
                    bookId = it.bookId,
                    noteContent = it.noteContent,
                    pageNumber = it.pageNumber,
                    timestamp = it.timestamp,
                    isSynced = false,
                    isDeleted = false
                )
            }

            // Perform bulk database inserts
            if (localProgress.isNotEmpty()) {
                appDatabase.progressDao().insertAllProgress(localProgress)
            }
            if (localFavorites.isNotEmpty()) {
                appDatabase.favoriteDao().insertAllFavorites(localFavorites)
            }
            if (localPins.isNotEmpty()) {
                appDatabase.pinDao().insertAllPins(localPins)
            }
            if (localNotes.isNotEmpty()) {
                appDatabase.noteDao().insertAllNotes(localNotes)
            }
            
            true
        } catch (e: Exception) {
            android.util.Log.e("BackupManager", "Error restoring backup: ${e.message}", e)
            false
        } finally {
            try {
                if (localFile.exists()) {
                    localFile.delete()
                }
            } catch (ex: Exception) {
                // ignore
            }
        }
    }

    // 4. Check if cloud backup exists
    suspend fun backupExistsOnCloud(userId: String): Boolean = withContext(Dispatchers.IO) {
        supabaseService.backupExists("$userId/backup.json")
    }
}
