package com.example.data

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseUser(
    val id: String,
    val email: String,
    val role: String = "user",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class SupabaseBook(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("file_type") val fileType: String = "pdf", // 'pdf' or 'epub'
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ForumPost(
    val id: String = "",
    @SerialName("user_id") val userId: String? = null,
    val title: String = "",
    val content: String = "",
    val category: String = "",
    @SerialName("likes_count") val likesCount: Int? = 0,
    @SerialName("replies_count") val repliesCount: Int? = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_role") val userRole: String? = null,
    @SerialName("author_email") val authorEmail: String? = null
)

@Serializable
data class ForumComment(
    val id: String = "",
    @SerialName("post_id") val postId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_role") val userRole: String? = null,
    @SerialName("author_email") val authorEmail: String? = null
)

data class ForumPostWithComments(
    val post: ForumPost,
    val comments: List<ForumComment>
)

@Serializable
data class ForumLike(
    @SerialName("post_id") val postId: String = "",
    @SerialName("user_id") val userId: String = ""
)

@Serializable
data class BookProgress(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("current_page") val currentPage: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("progress_percentage") val progressPercentage: Double? = 0.0,
    val status: String = "reading", // 'reading' or 'completed'
    @SerialName("last_read_at") val lastReadAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ForumStats(
    @SerialName("total_posts") val totalPosts: Int,
    @SerialName("total_likes") val totalLikes: Int,
    @SerialName("total_comments") val totalComments: Int,
    @SerialName("active_users") val activeUsers: Int
)

class SupabaseService(
    val supabaseClient: SupabaseClient,
    private val context: Context? = null
) {
    /**
     * Fetches all books marked as public from the Supabase database.
     * Uses Row-Level Security checks configured on the server.
     */
    suspend fun fetchPublicBooks(): List<SupabaseBook> = withContext(Dispatchers.IO) {
        supabaseClient.postgrest["books"]
            .select {
                filter {
                    eq("is_public", true)
                }
            }
            .decodeList<SupabaseBook>()
    }

    /**
     * Searches for books matching query by title or author that are marked public.
     */
    suspend fun searchBooks(query: String): List<SupabaseBook> = withContext(Dispatchers.IO) {
        supabaseClient.postgrest["books"]
            .select {
                filter {
                    eq("is_public", true)
                    or {
                        ilike("title", "%$query%")
                        ilike("author", "%$query%")
                    }
                }
            }
            .decodeList<SupabaseBook>()
    }

    /**
     * Fetches current user profile data details from the custom users table in Supabase.
     */
    suspend fun getCurrentUserProfile(userId: String): SupabaseUser? = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest["users"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<SupabaseUser>()
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserProfile(userId: String): SupabaseUser? = getCurrentUserProfile(userId)

    suspend fun updateUserProfile(userId: String, displayName: String, bio: String): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest["users"].update(
                update = {
                    set("display_name", displayName)
                    set("bio", bio)
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error updating profile: ${e.message}", e)
            false
        }
    }

    suspend fun uploadAvatar(userId: String, imageUri: Uri): String = withContext(Dispatchers.IO) {
        val ext = "jpg"
        val path = "${userId}_${System.currentTimeMillis()}.$ext"
        uploadToStorage("avatars", path, imageUri)
    }

    suspend fun updateAvatarUrl(userId: String, avatarUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest["users"].update(
                update = {
                    set("avatar_url", avatarUrl)
                }
            ) {
                filter {
                    eq("id", userId)
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error updating avatar URL: ${e.message}", e)
            false
        }
    }

    /**
     * Publishes a new book to the Supabase database.
     */
    suspend fun publishBook(book: SupabaseBook): Unit = withContext(Dispatchers.IO) {
        supabaseClient.postgrest["books"].insert(book)
    }

    /**
     * Creates a user profile record in Supabase using database RPC.
     */
    suspend fun createUserProfile(userId: String, email: String): Unit = withContext(Dispatchers.IO) {
        supabaseClient.postgrest.rpc(
            function = "create_user_profile",
            parameters = buildJsonObject {
                put("user_id", userId)
                put("user_email", email)
            }
        )
    }

    /**
     * Uploads file to Supabase storage and returns public URL.
     */
    suspend fun uploadToStorage(bucket: String, path: String, uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context?.contentResolver ?: throw Exception("Context not provided to SupabaseService")
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw Exception("Could not open input stream for Uri: $uri")
        
        supabaseClient.storage.from(bucket).upload(path, bytes) {
            upsert = true
        }
        
        "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$bucket/$path"
    }

    /**
     * Uploads a local file's content directly to a Supabase bucket.
     */
    suspend fun uploadFileToStorage(bucket: String, path: String, file: java.io.File): Unit = withContext(Dispatchers.IO) {
        val bytes = file.readBytes()
        supabaseClient.storage.from(bucket).upload(path, bytes) {
            upsert = true
        }
    }

    /**
     * Downloads a file from Supabase storage to a destination local file path.
     */
    suspend fun downloadFileFromStorage(bucket: String, path: String, destinationFile: java.io.File): Unit = withContext(Dispatchers.IO) {
        val bytes = supabaseClient.storage.from(bucket).downloadAuthenticated(path)
        destinationFile.writeBytes(bytes)
    }

    /**
     * Checks if a file exists in the specified Supabase storage bucket under list directory.
     */
    suspend fun fileExistsInStorage(bucket: String, path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val parts = path.split("/")
            val parentDir = if (parts.size > 1) parts.dropLast(1).joinToString("/") else ""
            val fileName = parts.last()
            
            val files = supabaseClient.storage.from(bucket).list(parentDir)
            files.any { it.name == fileName }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error in fileExistsInStorage: ${e.message}", e)
            false
        }
    }

    suspend fun uploadBackupFile(path: String, file: java.io.File): Unit = withContext(Dispatchers.IO) {
        val bytes = file.readBytes()
        supabaseClient.storage.from("user_backups").upload(path, bytes) {
            upsert = true
        }
    }

    suspend fun downloadBackupFile(path: String, destinationFile: java.io.File): Unit = withContext(Dispatchers.IO) {
        val bytes = supabaseClient.storage.from("user_backups").downloadAuthenticated(path)
        destinationFile.writeBytes(bytes)
    }

    suspend fun backupExists(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val parts = path.split("/")
            val parentDir = if (parts.size > 1) parts.dropLast(1).joinToString("/") else ""
            val fileName = parts.last()
            val files = supabaseClient.storage.from("user_backups").list(parentDir)
            files.any { it.name == fileName }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Inserts any arbitrary map representing a book row into public.books table.
     */
    suspend fun insertBook(bookData: Map<String, Any?>): Unit = withContext(Dispatchers.IO) {
        val jsonObject = buildJsonObject {
            bookData.forEach { (key, value) ->
                when (value) {
                    null -> put(key, JsonNull)
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    is Boolean -> put(key, value)
                    is JsonElement -> put(key, value)
                    else -> put(key, value.toString())
                }
            }
        }
        supabaseClient.postgrest["books"].insert(jsonObject)
    }

    /**
     * Returns total books count from public.books table.
     */
    suspend fun getBooksCount(): Int = withContext(Dispatchers.IO) {
        try {
            val books = supabaseClient.postgrest["books"].select().decodeList<SupabaseBook>()
            books.size
        } catch (e: Exception) {
            0
        }
    }

    fun isGoogleDriveLink(url: String): Boolean {
        return url.contains("drive.google.com")
    }

    fun convertGoogleDriveLink(url: String): String {
        if (!isGoogleDriveLink(url)) return url
        
        // Try to find file ID from /file/d/{FILE_ID}/view...
        if (url.contains("/file/d/")) {
            val startIndex = url.indexOf("/file/d/") + 8
            val sub = url.substring(startIndex)
            val endIndex = sub.indexOfAny(charArrayOf('/', '?', '&'))
            val fileId = if (endIndex != -1) sub.substring(0, endIndex) else sub
            return "https://drive.google.com/uc?export=download&id=$fileId"
        }
        
        // Try to find file ID from open?id={FILE_ID} or uc?id={FILE_ID}
        if (url.contains("id=")) {
            val startIndex = url.indexOf("id=") + 3
            val sub = url.substring(startIndex)
            val endIndex = sub.indexOf('&')
            val fileId = if (endIndex != -1) sub.substring(0, endIndex) else sub
            return "https://drive.google.com/uc?export=download&id=$fileId"
        }
        
        return url
    }

    fun isValidImageUrl(url: String): Boolean {
        val lowercase = url.lowercase().substringBefore("?")
        return (lowercase.startsWith("http://") || lowercase.startsWith("https://"))
    }

    fun isValidPdfUrl(url: String): Boolean {
        val lowercase = url.lowercase().substringBefore("?")
        return (lowercase.startsWith("http://") || lowercase.startsWith("https://")) && lowercase.endsWith(".pdf")
    }

    fun isValidEpubUrl(url: String): Boolean {
        val lowercase = url.lowercase().substringBefore("?")
        return (lowercase.startsWith("http://") || lowercase.startsWith("https://")) && lowercase.endsWith(".epub")
    }

    suspend fun fetchForumPosts(category: String? = null): List<ForumPost> = withContext(Dispatchers.IO) {
        try {
            val query = supabaseClient.postgrest["forum_posts"].select()
            val posts = query.decodeList<ForumPost>()
            val filtered = if (category == null || category == "All" || category == "") {
                posts
            } else {
                posts.filter { it.category.lowercase() == category.lowercase() }
            }
            filtered.sortedByDescending { it.createdAt ?: "" }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error fetching forum posts: ${e.message}", e)
            throw e
        }
    }

    suspend fun fetchPostDetails(postId: String): ForumPostWithComments = withContext(Dispatchers.IO) {
        val post = supabaseClient.postgrest["forum_posts"].select {
            filter {
                eq("id", postId)
            }
        }.decodeList<ForumPost>().firstOrNull() ?: throw Exception("Post not found")

        val comments = supabaseClient.postgrest["forum_comments"].select {
            filter {
                eq("post_id", postId)
            }
        }.decodeList<ForumComment>().sortedBy { it.createdAt ?: "" }

        ForumPostWithComments(post, comments)
    }

    suspend fun createForumPost(
        userId: String,
        title: String,
        content: String,
        category: String,
        authorEmail: String? = null,
        userRole: String = "user"
    ): Unit = withContext(Dispatchers.IO) {
        supabaseClient.postgrest.rpc(
            function = "create_forum_post",
            parameters = buildJsonObject {
                put("post_user_id", userId)
                put("post_title", title)
                put("post_content", content)
                put("post_category", category)
                put("post_author_email", authorEmail ?: "")
                put("post_user_role", userRole)
            }
        )
    }

    suspend fun createForumComment(
        postId: String,
        userId: String,
        content: String,
        authorEmail: String? = null,
        userRole: String = "user"
    ): Unit = withContext(Dispatchers.IO) {
        supabaseClient.postgrest.rpc(
            function = "create_forum_comment",
            parameters = buildJsonObject {
                put("comment_post_id", postId)
                put("comment_user_id", userId)
                put("comment_content", content)
                put("comment_author_email", authorEmail ?: "")
                put("comment_user_role", userRole)
            }
        )
    }

    suspend fun deletePost(postId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest.rpc(
                function = "delete_forum_post",
                parameters = buildJsonObject {
                    put("delete_post_id", postId)
                    put("delete_user_id", userId)
                }
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error deleting post: ${e.message}", e)
            throw e
        }
    }

    suspend fun fetchUserPosts(userId: String): List<ForumPost> = withContext(Dispatchers.IO) {
        try {
            val posts = supabaseClient.postgrest["forum_posts"].select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<ForumPost>()
            posts.sortedByDescending { it.createdAt ?: "" }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error fetching user posts: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun reportPost(postId: String, reason: String): Unit = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest["post_reports"].insert(
                buildJsonObject {
                    put("post_id", postId)
                    put("reason", reason)
                    put("reported_at", System.currentTimeMillis())
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error reporting post: ${e.message}", e)
            throw e
        }
    }

    suspend fun toggleLike(postId: String, userId: String): String = withContext(Dispatchers.IO) {
        val response = supabaseClient.postgrest.rpc(
            function = "toggle_like_post",
            parameters = buildJsonObject {
                put("like_post_id", postId)
                put("like_user_id", userId)
            }
        )
        response.decodeAs<String>()
    }

    suspend fun hasUserLikedPost(postId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val list = supabaseClient.postgrest["forum_likes"].select {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }.decodeList<ForumLike>()
            list.isNotEmpty()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error checking if liked: ${e.message}", e)
            false
        }
    }

    suspend fun fetchUserLikedPostIds(userId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val list = supabaseClient.postgrest["forum_likes"].select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<ForumLike>()
            list.map { it.postId }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error fetching user liked post ids: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun editPost(postId: String, userId: String, newTitle: String, newContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest.rpc(
                function = "edit_forum_post",
                parameters = buildJsonObject {
                    put("edit_post_id", postId)
                    put("edit_user_id", userId)
                    put("new_title", newTitle)
                    put("new_content", newContent)
                }
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error editing post: ${e.message}", e)
            false
        }
    }

    suspend fun editComment(commentId: String, userId: String, newContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest.rpc(
                function = "edit_forum_comment",
                parameters = buildJsonObject {
                    put("edit_comment_id", commentId)
                    put("edit_user_id", userId)
                    put("new_content", newContent)
                }
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error editing comment: ${e.message}", e)
            false
        }
    }

    suspend fun deleteComment(commentId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest.rpc(
                function = "delete_forum_comment",
                parameters = buildJsonObject {
                    put("delete_comment_id", commentId)
                    put("delete_user_id", userId)
                }
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error deleting comment: ${e.message}", e)
            false
        }
    }

    suspend fun updateBookProgress(
        userId: String,
        bookId: String,
        currentPage: Int,
        totalPages: Int,
        status: String = "reading"
    ) = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest.rpc(
                function = "update_book_progress",
                parameters = buildJsonObject {
                    put("progress_user_id", userId)
                    put("progress_book_id", bookId)
                    put("progress_current_page", currentPage)
                    put("progress_total_pages", totalPages)
                    put("progress_status", status)
                }
            )
        } catch (e: Exception) {
            android.util.Log.w("SupabaseService", "RPC update_book_progress failed, trying direct upsert: ${e.message}")
            try {
                val existing = supabaseClient.postgrest["book_progress"].select {
                    filter {
                        eq("user_id", userId)
                        eq("book_id", bookId)
                    }
                }.decodeList<BookProgress>().firstOrNull()

                val percentage = if (totalPages > 0) (currentPage.toDouble() / totalPages.toDouble()) * 100.0 else 0.0
                val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date())

                if (existing != null) {
                    supabaseClient.postgrest["book_progress"].update(
                        value = buildJsonObject {
                            put("current_page", currentPage)
                            put("total_pages", totalPages)
                            put("progress_percentage", percentage)
                            put("status", status)
                            put("last_read_at", nowStr)
                        }
                    ) {
                        filter {
                            eq("id", existing.id)
                        }
                    }
                } else {
                    supabaseClient.postgrest["book_progress"].insert(
                        value = buildJsonObject {
                            put("user_id", userId)
                            put("book_id", bookId)
                            put("current_page", currentPage)
                            put("total_pages", totalPages)
                            put("progress_percentage", percentage)
                            put("status", status)
                            put("last_read_at", nowStr)
                        }
                    )
                }
            } catch (ex: Exception) {
                android.util.Log.e("SupabaseService", "Direct book progress upsert failed: ${ex.message}", ex)
            }
        }
    }

    suspend fun getUserBookProgress(userId: String): List<BookProgress> = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest["book_progress"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<BookProgress>()
                .sortedByDescending { it.lastReadAt ?: "" }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error getUserBookProgress: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getBookProgress(userId: String, bookId: String): BookProgress? = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest["book_progress"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("book_id", bookId)
                    }
                }
                .decodeList<BookProgress>()
                .firstOrNull()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error getBookProgress: ${e.message}", e)
            null
        }
    }

    suspend fun getForumStats(): ForumStats = withContext(Dispatchers.IO) {
        try {
            val posts = supabaseClient.postgrest["forum_posts"].select().decodeList<ForumPost>()
            val totalPosts = posts.size
            val totalLikes = posts.sumOf { it.likesCount ?: 0 }
            val totalComments = posts.sumOf { it.repliesCount ?: 0 }
            val activeUsers = posts.map { it.userId }.filterNotNull().distinct().size
            ForumStats(
                totalPosts = totalPosts,
                totalLikes = totalLikes,
                totalComments = totalComments,
                activeUsers = if (activeUsers > 0) activeUsers else 1
            )
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error getForumStats: ${e.message}", e)
            ForumStats(0, 0, 0, 1)
        }
    }

    suspend fun downloadBookFile(
        bookId: String,
        fileUrl: String,
        destinationFile: java.io.File,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            destinationFile.parentFile?.mkdirs()
            val url = java.net.URL(fileUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 20000
            connection.readTimeout = 20000
            connection.connect()
            
            if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                android.util.Log.e("SupabaseService", "HTTP error ${connection.responseCode} downloading from: $fileUrl")
                return@withContext false
            }
            
            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = java.io.FileOutputStream(destinationFile)
            
            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (inputStream.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val progress = ((total * 100) / fileLength).toInt()
                    onProgress(progress)
                }
                outputStream.write(data, 0, count)
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            true
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error in downloadBookFile: ${e.message}", e)
            false
        }
    }

    suspend fun getDownloadedFilePath(bookId: String): String? = withContext(Dispatchers.IO) {
        val storageDir = java.io.File(context?.getExternalFilesDir("books") ?: context?.filesDir, "")
        val localFile = java.io.File(storageDir, "$bookId.pdf")
        if (localFile.exists()) {
            localFile.absolutePath
        } else {
            null
        }
    }
}
