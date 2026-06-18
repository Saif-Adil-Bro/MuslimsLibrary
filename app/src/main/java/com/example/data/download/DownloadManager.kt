package com.example.data.download

import android.content.Context
import android.util.Log
import com.example.data.SupabaseService
import com.example.data.local.AppDatabase
import com.example.data.local.entities.DownloadedBook
import com.example.data.model.Book
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DownloadManager(
    private val context: Context,
    private val database: AppDatabase,
    private val supabaseService: SupabaseService
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = mutableMapOf<String, Job>()
    
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    data class DownloadState(
        val progress: Int,
        val status: String, // "downloading", "paused", "failed", "completed"
        val speedText: String = "",
        val timeRemainingText: String = ""
    )

    fun getBooksDir(): File {
        return File(context.getExternalFilesDir("books") ?: context.filesDir, "")
    }

    suspend fun isBookDownloaded(bookId: String): Boolean {
        val dbBook = database.downloadDao().getDownloadedBook(bookId)
        if (dbBook != null && dbBook.downloadStatus == "completed") {
            val file = File(dbBook.localFilePath)
            if (file.exists() && file.length() > 0) {
                return true
            }
        }
        return false
    }

    suspend fun getDownloadedBook(bookId: String): DownloadedBook? {
        return database.downloadDao().getDownloadedBook(bookId)
    }

    fun startDownload(book: Book) {
        if (activeJobs.containsKey(book.id)) return
        
        val job = scope.launch {
            try {
                _downloadStates.value = _downloadStates.value + (book.id to DownloadState(0, "downloading"))
                
                val booksDir = getBooksDir()
                booksDir.mkdirs()
                val localFile = File(booksDir, "${book.id}.pdf")
                
                val downloadedBook = DownloadedBook(
                    id = book.id,
                    bookId = book.id,
                    title = book.title,
                    author = book.author,
                    coverImageUrl = book.coverUrl,
                    localFilePath = localFile.absolutePath,
                    fileSize = 0L,
                    downloadDate = System.currentTimeMillis(),
                    lastOpenedDate = null,
                    downloadStatus = "downloading",
                    progress = 0
                )
                database.downloadDao().insertDownloadedBook(downloadedBook)

                val startTime = System.currentTimeMillis()
                
                val success = supabaseService.downloadBookFile(
                    bookId = book.id,
                    fileUrl = book.pdfUrl,
                    destinationFile = localFile,
                    onProgress = { progress ->
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                        val speedText = if (elapsed > 0) {
                            val speed = (localFile.length() / (1024.0 * 1024.0)) / elapsed
                            String.format("%.1f MB/s", speed)
                        } else ""
                        
                        _downloadStates.value = _downloadStates.value + (book.id to DownloadState(
                            progress = progress,
                            status = "downloading",
                            speedText = speedText
                        ))
                        
                        scope.launch {
                            database.downloadDao().insertDownloadedBook(
                                downloadedBook.copy(
                                    downloadStatus = "downloading",
                                    progress = progress,
                                    fileSize = localFile.length()
                                )
                            )
                        }
                    }
                )

                if (success) {
                    _downloadStates.value = _downloadStates.value + (book.id to DownloadState(100, "completed"))
                    database.downloadDao().insertDownloadedBook(
                        downloadedBook.copy(
                            downloadStatus = "completed",
                            progress = 100,
                            fileSize = localFile.length(),
                            downloadDate = System.currentTimeMillis()
                        )
                    )
                } else {
                    _downloadStates.value = _downloadStates.value + (book.id to DownloadState(0, "failed"))
                    database.downloadDao().insertDownloadedBook(
                        downloadedBook.copy(
                            downloadStatus = "failed",
                            progress = 0
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("DownloadManager", "Error in download job: ${e.message}", e)
                _downloadStates.value = _downloadStates.value + (book.id to DownloadState(0, "failed"))
                val localFile = File(getBooksDir(), "${book.id}.pdf")
                database.downloadDao().insertDownloadedBook(
                    DownloadedBook(
                        id = book.id,
                        bookId = book.id,
                        title = book.title,
                        author = book.author,
                        coverImageUrl = book.coverUrl,
                        localFilePath = localFile.absolutePath,
                        fileSize = 0L,
                        downloadDate = System.currentTimeMillis(),
                        lastOpenedDate = null,
                        downloadStatus = "failed",
                        progress = 0
                    )
                )
            } finally {
                activeJobs.remove(book.id)
            }
        }
        activeJobs[book.id] = job
    }

    fun pauseDownload(bookId: String) {
        val job = activeJobs[bookId]
        if (job != null) {
            job.cancel()
            activeJobs.remove(bookId)
            val currentState = _downloadStates.value[bookId] ?: DownloadState(0, "paused")
            _downloadStates.value = _downloadStates.value + (bookId to currentState.copy(status = "paused"))
            
            scope.launch {
                val dbBook = database.downloadDao().getDownloadedBook(bookId)
                if (dbBook != null) {
                    database.downloadDao().insertDownloadedBook(
                        dbBook.copy(downloadStatus = "paused")
                    )
                }
            }
        }
    }

    fun resumeDownload(book: Book) {
        startDownload(book)
    }

    suspend fun cancelDownload(bookId: String) {
        val job = activeJobs[bookId]
        job?.cancel()
        activeJobs.remove(bookId)
        _downloadStates.value = _downloadStates.value - bookId
        
        database.downloadDao().deleteDownloadedBook(bookId)
        val localFile = File(getBooksDir(), "$bookId.pdf")
        if (localFile.exists()) {
            localFile.delete()
        }
    }

    suspend fun deleteDownload(bookId: String) {
        cancelDownload(bookId)
    }
}
