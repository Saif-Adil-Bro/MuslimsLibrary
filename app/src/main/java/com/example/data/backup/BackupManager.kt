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
    private val firebaseAuth: FirebaseAuth,
    private val guestModeManager: com.example.data.util.GuestModeManager
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun uploadBackup(roomUserId: String? = null) {
        if (guestModeManager.isGuestMode()) {
            throw SecurityException("গেস্ট মোডে ব্যাকআপ উপলব্ধ নয়। অনুগ্রহ করে লগইন করুন।")
        }
        val userId = firebaseAuth.currentUser?.uid 
            ?: throw SecurityException("User not authenticated")
        if (userId.length < 10) {
            throw SecurityException("Invalid user ID")
        }
        uploadBackupToCloud(userId, roomUserId)
    }

    suspend fun downloadBackup(cloudBackupUserId: String? = null, roomUserId: String? = null) {
        if (guestModeManager.isGuestMode()) {
            throw SecurityException("গেস্ট মোডে রিস্টোর উপলব্ধ নয়। অনুগ্রহ করে লগইন করুন।")
        }
        val currentUid = firebaseAuth.currentUser?.uid
        val userId = when {
            !cloudBackupUserId.isNullOrBlank() -> cloudBackupUserId
            !currentUid.isNullOrBlank() -> currentUid
            else -> throw SecurityException("User not authenticated")
        }
        
        var finalUserId = userId
        var exists = backupExistsOnCloud(finalUserId)
        if (!exists && !currentUid.isNullOrBlank() && finalUserId != currentUid) {
            finalUserId = currentUid
            exists = backupExistsOnCloud(finalUserId)
        }
        
        if (!exists) {
            throw Exception("No backup found for this account")
        }

        val success = downloadAndRestoreFromCloud(finalUserId, roomUserId ?: currentUid)
        if (!success) {
            throw Exception("Restore process failed.")
        }
    }

    // 1. Export Room data to JSON file
    suspend fun exportDataToJson(userId: String, roomUserId: String? = null): File = withContext(Dispatchers.IO) {
        val currentUser = firebaseAuth.currentUser
        val email = currentUser?.email ?: ""
        
        android.util.Log.d("BackupManager", "Exporting data. Passed userId (UID): $userId, roomUserId: $roomUserId, Current email: $email")

        // Gather all candidate IDs to query Room database
        val candidateIds = mutableSetOf<String>()
        candidateIds.add(userId) // Firebase UID
        if (roomUserId != null && roomUserId.isNotBlank()) {
            candidateIds.add(roomUserId)
        }
        if (email.isNotBlank()) {
            candidateIds.add(email)
        }
        candidateIds.add("Guest User")
        candidateIds.add("guest_user")
        candidateIds.add("User@muslimslibrary.org")
        candidateIds.add("User_Guest")

        android.util.Log.d("BackupManager", "Exporting for candidate user IDs: $candidateIds")

        // 1. Progress: Fetch and deduplicate by bookId (keeping the latest updated entry)
        val rawProgress = mutableListOf<LocalBookProgress>()
        for (candidate in candidateIds) {
            try {
                rawProgress.addAll(appDatabase.progressDao().getAllProgressForUser(candidate))
            } catch (ex: Exception) {
                android.util.Log.e("BackupManager", "Error querying progress for candidate $candidate", ex)
            }
        }
        val progressMap = mutableMapOf<String, LocalBookProgress>()
        for (p in rawProgress) {
            val existing = progressMap[p.bookId]
            if (existing == null || p.lastReadAt > existing.lastReadAt) {
                progressMap[p.bookId] = p
            }
        }
        val finalProgress = progressMap.values.toList()
        android.util.Log.d("BackupManager", "Export distinct progress records count: ${finalProgress.size}")

        val progress = finalProgress.map {
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

        // 2. Favorites: Fetch and deduplicate (keeping active favorites where isDeleted = false)
        val rawFavorites = mutableListOf<LocalFavoriteBook>()
        for (candidate in candidateIds) {
            try {
                rawFavorites.addAll(appDatabase.favoriteDao().getAllFavoritesForUser(candidate))
            } catch (ex: Exception) {
                android.util.Log.e("BackupManager", "Error querying favorites for candidate $candidate", ex)
            }
        }
        val favoritesMap = mutableMapOf<String, LocalFavoriteBook>()
        for (f in rawFavorites) {
            val existing = favoritesMap[f.bookId]
            if (existing == null || (existing.isDeleted && !f.isDeleted) || f.timestamp > existing.timestamp) {
                favoritesMap[f.bookId] = f
            }
        }
        val finalFavorites = favoritesMap.values.filter { !it.isDeleted }
        android.util.Log.d("BackupManager", "Export distinct favorites count: ${finalFavorites.size}")

        val favorites = finalFavorites.map {
            BackupFavoriteBook(
                id = it.id,
                userId = it.userId,
                bookId = it.bookId,
                timestamp = it.timestamp
            )
        }

        // 3. Pins: Fetch and deduplicate (keeping active pins where isDeleted = false)
        val rawPins = mutableListOf<LocalPinnedBook>()
        for (candidate in candidateIds) {
            try {
                rawPins.addAll(appDatabase.pinDao().getAllPinsForUser(candidate))
            } catch (ex: Exception) {
                android.util.Log.e("BackupManager", "Error querying pins for candidate $candidate", ex)
            }
        }
        val pinsMap = mutableMapOf<String, LocalPinnedBook>()
        for (p in rawPins) {
            val existing = pinsMap[p.bookId]
            if (existing == null || (existing.isDeleted && !p.isDeleted) || p.timestamp > existing.timestamp) {
                pinsMap[p.bookId] = p
            }
        }
        val finalPins = pinsMap.values.filter { !it.isDeleted }
        android.util.Log.d("BackupManager", "Export distinct pins count: ${finalPins.size}")

        val pins = finalPins.map {
            BackupPinnedBook(
                id = it.id,
                userId = it.userId,
                bookId = it.bookId,
                timestamp = it.timestamp
            )
        }

        // 4. Notes: Fetch and deduplicate by note ID (keeping active notes where isDeleted = false)
        val rawNotes = mutableListOf<LocalUserNote>()
        for (candidate in candidateIds) {
            try {
                rawNotes.addAll(appDatabase.noteDao().getAllNotesForUser(candidate))
            } catch (ex: Exception) {
                android.util.Log.e("BackupManager", "Error querying notes for candidate $candidate", ex)
            }
        }
        val notesMap = mutableMapOf<String, LocalUserNote>()
        for (n in rawNotes) {
            val existing = notesMap[n.id]
            if (existing == null || (existing.isDeleted && !n.isDeleted) || n.timestamp > existing.timestamp) {
                notesMap[n.id] = n
            }
        }
        val finalNotes = notesMap.values.filter { !it.isDeleted }
        android.util.Log.d("BackupManager", "Export distinct notes count: ${finalNotes.size}")

        val notes = finalNotes.map {
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
    suspend fun uploadBackupToCloud(userId: String, roomUserId: String? = null): Unit = withContext(Dispatchers.IO) {
        val localFile = exportDataToJson(userId, roomUserId)
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
    suspend fun downloadAndRestoreFromCloud(userId: String, roomUserId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val storagePath = "$userId/backup.json"
        val localFile = File(context.cacheDir, "temp_restore.json")
        
        try {
            // Determine active user ID (email preferred, falling back to UID)
            val currentUser = firebaseAuth.currentUser
            val currentEmail = currentUser?.email
            val currentUid = currentUser?.uid
            val activeUserId = when {
                roomUserId != null && roomUserId.isNotBlank() -> roomUserId
                !currentEmail.isNullOrBlank() -> currentEmail
                !uidToUseIsEmail(userId) && !currentUid.isNullOrBlank() -> currentUid
                else -> userId
            }
            
            android.util.Log.d("BackupManager", "Restoring. Mapping all backup records to active userId: $activeUserId")

            // Download content
            supabaseService.downloadBackupFile(storagePath, localFile)
            
            if (!localFile.exists()) return@withContext false
            
            val jsonString = localFile.readText()
            val backupData = json.decodeFromString(FullBackupData.serializer(), jsonString)
            
            // Map backup objects back to Room entity models using activeUserId
            val localProgress = backupData.progress.map {
                LocalBookProgress(
                    id = "${activeUserId}_${it.bookId}",
                    userId = activeUserId,
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
                    id = "${activeUserId}_${it.bookId}",
                    userId = activeUserId,
                    bookId = it.bookId,
                    timestamp = it.timestamp,
                    isSynced = false,
                    isDeleted = false
                )
            }

            val localPins = backupData.pins.map {
                LocalPinnedBook(
                    id = "${activeUserId}_${it.bookId}",
                    userId = activeUserId,
                    bookId = it.bookId,
                    timestamp = it.timestamp,
                    isSynced = false,
                    isDeleted = false
                )
            }

            val localNotes = backupData.notes.map {
                LocalUserNote(
                    id = it.id,
                    userId = activeUserId,
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

    private fun uidToUseIsEmail(uid: String): Boolean {
        return uid.contains("@")
    }

    // 4. Check if cloud backup exists
    suspend fun backupExistsOnCloud(userId: String): Boolean = withContext(Dispatchers.IO) {
        supabaseService.backupExists("$userId/backup.json")
    }

    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        appDatabase.clearAllTables()
    }
}
