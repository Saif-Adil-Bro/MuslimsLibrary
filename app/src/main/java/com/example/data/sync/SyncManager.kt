package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.SupabaseService
import com.example.data.local.AppDatabase
import com.example.data.local.entities.LocalBookProgress
import com.example.data.local.entities.LocalFavoriteBook
import com.example.data.local.entities.LocalPinnedBook
import com.example.data.local.entities.LocalUserNote
import com.example.data.model.SupabaseFavorite
import com.example.data.model.SupabaseNote
import com.example.data.model.SupabasePin
import com.example.data.util.NetworkMonitor
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val supabaseService: SupabaseService,
    private val networkMonitor: NetworkMonitor,
    private val guestModeManager: com.example.data.util.GuestModeManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        // Automatically trigger sync when network comes back online
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    Log.d("SyncManager", "Network is ON: Auto-triggering sync check...")
                    // If a user is logged in, we could trigger sync.
                    // But usually, the Repository triggers this with the current logged-in user's ID.
                }
            }
        }
    }

    /**
     * Performs a bidirectional sync:
     * 1. PUSH unsynced local changes (including soft deletes) to Supabase.
     * 2. PULL the latest server state from Supabase to local Room database.
     */
    suspend fun syncNow(userId: String) = withContext(Dispatchers.IO) {
        if (guestModeManager.isGuestMode()) {
            Log.d("SyncManager", "Guest mode - skipping cloud sync")
            return@withContext
        }
        if (userId.isEmpty()) return@withContext
        
        // Prevent concurrent sync cycles
        if (_isSyncing.value) {
            Log.d("SyncManager", "Sync is already in progress, skipping...")
            return@withContext
        }

        val isOnlineNow = networkMonitor.isOnline.first()
        if (!isOnlineNow) {
            Log.d("SyncManager", "Device is offline. Skipping sync until next online event.")
            return@withContext
        }

        try {
            _isSyncing.value = true
            Log.d("SyncManager", "Sync started for userId: $userId")

            // Phase 1: PUSH local offline mutations to server
            pushLocalMutationsToServer(userId)

            // Phase 2: PULL latest server states to local Room DB
            pullServerStateToLocal(userId)

            Log.d("SyncManager", "Sync completed successfully!")
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync cycle encountered error matches: ${e.message}", e)
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun pushLocalMutationsToServer(userId: String) {
        // --- 1. Push Book Progress ---
        try {
            val unsyncedProgress = database.progressDao().getUnsyncedProgress()
            Log.d("SyncManager", "Pushing ${unsyncedProgress.size} progress entries...")
            for (p in unsyncedProgress.filter { it.userId == userId }) {
                try {
                    supabaseService.updateBookProgress(
                        userId = p.userId,
                        bookId = p.bookId,
                        currentPage = p.currentPage,
                        totalPages = p.totalPages,
                        status = p.status
                    )
                    database.progressDao().markProgressSynced(p.id)
                } catch (e: Exception) {
                    Log.e("SyncManager", "Failed to push progress ${p.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Progress push failure: ${e.message}")
        }

        // --- 2. Push Favorites ---
        try {
            val unsyncedFavs = database.favoriteDao().getUnsyncedFavorites()
            Log.d("SyncManager", "Pushing ${unsyncedFavs.size} favorite mutations...")
            for (f in unsyncedFavs.filter { it.userId == userId }) {
                try {
                    if (f.isDeleted) {
                        // Delete on Supabase
                        supabaseService.supabaseClient.postgrest["favorite_books"].delete {
                            filter {
                                eq("id", f.id)
                            }
                        }
                        database.favoriteDao().removeFavoriteFromDb(f.id)
                    } else {
                        // Insert/Upsert on Supabase
                        val favModel = SupabaseFavorite(id = f.id, userId = f.userId, bookId = f.bookId)
                        supabaseService.supabaseClient.postgrest["favorite_books"].upsert(favModel)
                        database.favoriteDao().markFavoriteSynced(f.id)
                    }
                } catch (e: Exception) {
                    Log.e("SyncManager", "Failed to push favorite ${f.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Favorites push failure: ${e.message}")
        }

        // --- 3. Push Pins ---
        try {
            val unsyncedPins = database.pinDao().getUnsyncedPins()
            Log.d("SyncManager", "Pushing ${unsyncedPins.size} pin mutations...")
            for (p in unsyncedPins.filter { it.userId == userId }) {
                try {
                    if (p.isDeleted) {
                        supabaseService.supabaseClient.postgrest["pinned_books"].delete {
                            filter {
                                eq("id", p.id)
                            }
                        }
                        database.pinDao().removePinFromDb(p.id)
                    } else {
                        val pinModel = SupabasePin(id = p.id, userId = p.userId, bookId = p.bookId)
                        supabaseService.supabaseClient.postgrest["pinned_books"].upsert(pinModel)
                        database.pinDao().markPinSynced(p.id)
                    }
                } catch (e: Exception) {
                    Log.e("SyncManager", "Failed to push pin ${p.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Pins push failure: ${e.message}")
        }

        // --- 4. Push Notes ---
        try {
            val unsyncedNotes = database.noteDao().getUnsyncedNotes()
            Log.d("SyncManager", "Pushing ${unsyncedNotes.size} note mutations...")
            for (n in unsyncedNotes.filter { it.userId == userId }) {
                try {
                    if (n.isDeleted) {
                        supabaseService.supabaseClient.postgrest["user_notes"].delete {
                            filter {
                                eq("id", n.id)
                            }
                        }
                        database.noteDao().removeNoteFromDb(n.id)
                    } else {
                        val noteModel = SupabaseNote(
                            id = n.id,
                            userId = n.userId,
                            bookId = n.bookId,
                            noteContent = n.noteContent,
                            pageNumber = n.pageNumber
                        )
                        supabaseService.supabaseClient.postgrest["user_notes"].upsert(noteModel)
                        database.noteDao().markNoteSynced(n.id)
                    }
                } catch (e: Exception) {
                    Log.e("SyncManager", "Failed to push note ${n.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Notes push failure: ${e.message}")
        }
    }

    private suspend fun pullServerStateToLocal(userId: String) {
        // --- 1. Pull Book Progress ---
        try {
            val serverProgressList = supabaseService.getUserBookProgress(userId)
            Log.d("SyncManager", "Pulled ${serverProgressList.size} progress objects from server")
            for (sp in serverProgressList) {
                val localModel = LocalBookProgress.fromSupabaseModel(sp, isSynced = true)
                val existingLocal = database.progressDao().getProgressOneShot(sp.userId, sp.bookId)
                if (existingLocal == null || !existingLocal.isSynced) {
                    // Update/Insert since it doesn't exist locally or local isn't synced and we resolve (server wins by default on fresh pull)
                    database.progressDao().insertProgress(localModel)
                } else if (localModel.lastReadAt >= existingLocal.lastReadAt) {
                    // Conflicted: server is newer or equal
                    database.progressDao().insertProgress(localModel)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Error pulling book progress: ${e.message}")
        }

        // --- 2. Pull Favorites ---
        try {
            val serverFavs = supabaseService.supabaseClient.postgrest["favorite_books"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }.decodeList<SupabaseFavorite>()
            
            Log.d("SyncManager", "Pulled ${serverFavs.size} favorites from server")
            for (sf in serverFavs) {
                val localFav = LocalFavoriteBook(
                    id = sf.id.ifEmpty { "${sf.userId}_${sf.bookId}" },
                    userId = sf.userId,
                    bookId = sf.bookId,
                    timestamp = System.currentTimeMillis(),
                    isSynced = true,
                    isDeleted = false
                )
                
                val isLocalFav = database.favoriteDao().isFavoriteOneShot(sf.userId, sf.bookId)
                if (!isLocalFav) {
                    database.favoriteDao().insertFavorite(localFav)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Error pulling favorites: ${e.message}")
        }

        // --- 3. Pull Pins ---
        try {
            val serverPins = supabaseService.supabaseClient.postgrest["pinned_books"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }.decodeList<SupabasePin>()
            
            Log.d("SyncManager", "Pulled ${serverPins.size} pinned books from server")
            for (sp in serverPins) {
                val localPin = LocalPinnedBook(
                    id = sp.id.ifEmpty { "${sp.userId}_${sp.bookId}" },
                    userId = sp.userId,
                    bookId = sp.bookId,
                    timestamp = System.currentTimeMillis(),
                    isSynced = true,
                    isDeleted = false
                )
                val isLocalPinned = database.pinDao().isPinnedOneShot(sp.userId, sp.bookId)
                if (!isLocalPinned) {
                    database.pinDao().insertPin(localPin)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Error pulling pins: ${e.message}")
        }

        // --- 4. Pull Notes ---
        try {
            val serverNotes = supabaseService.supabaseClient.postgrest["user_notes"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }.decodeList<SupabaseNote>()
            
            Log.d("SyncManager", "Pulled ${serverNotes.size} notes from server")
            for (sn in serverNotes) {
                val localNote = LocalUserNote(
                    id = sn.id,
                    userId = sn.userId,
                    bookId = sn.bookId,
                    noteContent = sn.noteContent,
                    pageNumber = sn.pageNumber,
                    timestamp = System.currentTimeMillis(),
                    isSynced = true,
                    isDeleted = false
                )
                
                val existingLocalNote = database.noteDao().getNoteById(sn.id)
                if (existingLocalNote == null) {
                    database.noteDao().insertNote(localNote)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Error pulling notes: ${e.message}")
        }
    }
}
