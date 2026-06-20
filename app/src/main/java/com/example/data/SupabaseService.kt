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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
data class SupabaseAuthor(
    val id: String = "",
    val name: String,
    val bio: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabaseCategory(
    val id: String = "",
    val name: String = ""
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
    private fun saveCategoriesLocally(categories: List<SupabaseCategory>) {
        if (context == null) return
        try {
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val jsonString = Json.encodeToString(categories)
            sharedPrefs.edit().putString("cached_categories", jsonString).apply()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error saving categories locally: ${e.message}")
        }
    }

    private fun loadCategoriesLocally(): List<SupabaseCategory>? {
        if (context == null) return null
        try {
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val jsonString = sharedPrefs.getString("cached_categories", null) ?: return null
            return Json.decodeFromString<List<SupabaseCategory>>(jsonString)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error loading categories locally: ${e.message}")
            return null
        }
    }

    private fun trackDeletedBookLocally(id: String) {
        if (context == null) return
        try {
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val deletedStr = sharedPrefs.getString("locally_deleted_books", "[]") ?: "[]"
            val deletedList = try {
                Json.decodeFromString<List<String>>(deletedStr)
            } catch (e: Exception) {
                emptyList()
            }
            val updatedList = (deletedList + id).distinct()
            sharedPrefs.edit().putString("locally_deleted_books", Json.encodeToString(updatedList)).apply()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error tracking deleted book: ${e.message}")
        }
    }

    private fun trackUpdatedBookLocally(book: SupabaseBook) {
        if (context == null) return
        try {
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val updatedStr = sharedPrefs.getString("locally_updated_books", "[]") ?: "[]"
            val updatedList = try {
                Json.decodeFromString<List<SupabaseBook>>(updatedStr)
            } catch (e: Exception) {
                emptyList()
            }
            val newUpdatedList = (updatedList.filter { it.id != book.id } + book)
            sharedPrefs.edit().putString("locally_updated_books", Json.encodeToString(newUpdatedList)).apply()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error tracking updated book: ${e.message}")
        }
    }

    private fun trackAddedBookLocally(book: SupabaseBook) {
        if (context == null) return
        try {
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val addedStr = sharedPrefs.getString("locally_added_books", "[]") ?: "[]"
            val addedList = try {
                Json.decodeFromString<List<SupabaseBook>>(addedStr)
            } catch (e: Exception) {
                emptyList()
            }
            val newAddedList = (addedList.filter { it.id != book.id } + book)
            sharedPrefs.edit().putString("locally_added_books", Json.encodeToString(newAddedList)).apply()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error tracking added book: ${e.message}")
        }
    }

    private fun applyLocalOverrides(books: List<SupabaseBook>): List<SupabaseBook> {
        if (context == null) return books
        var resultList = books
        try {
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            
            // 1. Remove deleted books
            val deletedStr = sharedPrefs.getString("locally_deleted_books", "[]") ?: "[]"
            val deletedList = try {
                Json.decodeFromString<List<String>>(deletedStr)
            } catch (e: Exception) {
                emptyList()
            }
            if (deletedList.isNotEmpty()) {
                resultList = resultList.filter { it.id !in deletedList }
            }

            // 2. Apply updated books
            val updatedStr = sharedPrefs.getString("locally_updated_books", "[]") ?: "[]"
            val updatedList = try {
                Json.decodeFromString<List<SupabaseBook>>(updatedStr)
            } catch (e: Exception) {
                emptyList()
            }
            if (updatedList.isNotEmpty()) {
                val updatedMap = updatedList.associateBy { it.id }
                resultList = resultList.map { book ->
                    updatedMap[book.id] ?: book
                }
            }

            // 3. Append added books
            val addedStr = sharedPrefs.getString("locally_added_books", "[]") ?: "[]"
            val addedList = try {
                Json.decodeFromString<List<SupabaseBook>>(addedStr)
            } catch (e: Exception) {
                emptyList()
            }
            if (addedList.isNotEmpty()) {
                val activeAdded = addedList.filter { it.id !in deletedList }
                val finalAdded = activeAdded.map { addedBook ->
                    val updatedVersion = updatedList.find { it.id == addedBook.id }
                    updatedVersion ?: addedBook
                }
                resultList = (resultList + finalAdded).distinctBy { it.id }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error applying local overrides: ${e.message}")
        }
        return resultList
    }

    private fun saveBooksLocally(books: List<SupabaseBook>) {
        if (context == null) return
        try {
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val jsonString = Json.encodeToString(books)
            sharedPrefs.edit().putString("cached_books", jsonString).apply()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error saving books locally: ${e.message}")
        }
    }

    private fun loadBooksLocally(): List<SupabaseBook>? {
        if (context == null) return null
        try {
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val jsonString = sharedPrefs.getString("cached_books", null) ?: return null
            return Json.decodeFromString<List<SupabaseBook>>(jsonString)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error loading books locally: ${e.message}")
            return null
        }
    }

    /**
     * Fetches all books marked as public from the Supabase database.
     * Uses Row-Level Security checks configured on the server.
     */
    suspend fun fetchPublicBooks(): List<SupabaseBook> = withContext(Dispatchers.IO) {
        try {
            val remoteBooks = supabaseClient.postgrest["books"]
                .select {
                    filter {
                        eq("is_public", true)
                    }
                }
                .decodeList<SupabaseBook>()
            val finalBooks = applyLocalOverrides(remoteBooks)
            saveBooksLocally(finalBooks)
            finalBooks
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error fetching books from Supabase, loading cache: ${e.message}")
            val cached = loadBooksLocally() ?: emptyList()
            applyLocalOverrides(cached)
        }
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
        trackAddedBookLocally(book)
        val current = loadBooksLocally() ?: fetchPublicBooks()
        val updated = applyLocalOverrides(current)
        saveBooksLocally(updated)
        
        try {
            supabaseClient.postgrest["books"].insert(book)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error publishing book to database: ${e.message}")
        }
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

        try {
            val id = (jsonObject["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
            val title = (jsonObject["title"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
            val author = (jsonObject["author"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
            val category = (jsonObject["category"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
            val coverImageUrl = (jsonObject["cover_image_url"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            val fileUrl = (jsonObject["file_url"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            val fileType = (jsonObject["file_type"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "pdf"
            
            if (id.isNotEmpty() && title.isNotEmpty()) {
                val book = SupabaseBook(
                    id = id,
                    title = title,
                    author = author,
                    category = category,
                    coverImageUrl = coverImageUrl,
                    fileUrl = fileUrl,
                    fileType = fileType
                )
                trackAddedBookLocally(book)
                val current = loadBooksLocally() ?: fetchPublicBooks()
                val updated = applyLocalOverrides(current)
                saveBooksLocally(updated)
            }
        } catch (ex: Exception) {
            android.util.Log.e("SupabaseService", "Error decoding book JSON to local cache: ${ex.message}")
        }

        try {
            supabaseClient.postgrest["books"].insert(jsonObject)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error inserting book into database: ${e.message}")
        }
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
                put("post_user_role", userRole.lowercase())
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
                put("comment_user_role", userRole.lowercase())
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

    suspend fun searchAuthors(query: String, limit: Int = 10): List<SupabaseAuthor> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()
        try {
            supabaseClient.postgrest["authors"]
                .select {
                    filter {
                        ilike("name", "%$query%")
                    }
                    limit(count = limit.toLong())
                }
                .decodeList<SupabaseAuthor>()
                .sortedBy { it.name }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error searching authors from authors table (falling back to books): ${e.message}", e)
            try {
                // FALLBACK: Query 'books' table, extract unique authors
                val books = supabaseClient.postgrest["books"]
                    .select {
                        filter {
                            ilike("author", "%$query%")
                        }
                    }
                    .decodeList<SupabaseBook>()
                
                books.map { it.author }
                    .distinct()
                    .take(limit)
                    .map { authorName ->
                        SupabaseAuthor(
                            id = authorName.hashCode().toString(),
                            name = authorName,
                            bio = null
                        )
                    }
                    .sortedBy { it.name }
            } catch (ex: Exception) {
                android.util.Log.e("SupabaseService", "Error fallback searching authors: ${ex.message}", ex)
                emptyList()
            }
        }
    }

    suspend fun getAuthorBookCount(authorName: String): Int = withContext(Dispatchers.IO) {
        try {
            val books = supabaseClient.postgrest["books"]
                .select {
                    filter {
                        eq("author", authorName)
                        eq("is_public", true)
                    }
                }
                .decodeList<SupabaseBook>()
            books.size
        } catch (e: Exception) {
            0
        }
    }

    suspend fun checkAuthorExistsInAuthorsTable(authorName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = supabaseClient.postgrest["authors"]
                .select {
                    filter {
                        eq("name", authorName)
                    }
                }
                .decodeList<SupabaseAuthor>()
            existing.isNotEmpty()
        } catch (e: Exception) {
            android.util.Log.w("SupabaseService", "Error checking checkAuthorExistsInAuthorsTable: ${e.message}")
            false
        }
    }

    // Fetch all authors from 'authors' table
    suspend fun getAllAuthors(): List<SupabaseAuthor> = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest["authors"]
                .select()
                .decodeList<SupabaseAuthor>()
                .sortedBy { it.name }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error loading all authors from authors table (falling back to book authors): ${e.message}", e)
            try {
                val books = supabaseClient.postgrest["books"].select().decodeList<SupabaseBook>()
                books.map { it.author }
                    .distinct()
                    .map { authorName ->
                        SupabaseAuthor(
                            id = authorName.hashCode().toString(),
                            name = authorName,
                            bio = null
                        )
                    }
                    .sortedBy { it.name }
            } catch (ex: Exception) {
                android.util.Log.e("SupabaseService", "Error fallback authors fetching: ${ex.message}", ex)
                emptyList()
            }
        }
    }

    // Upsert Author (Update if exists, add if not)
    suspend fun upsertAuthor(name: String, bio: String? = null): Unit = withContext(Dispatchers.IO) {
        try {
            val existing = supabaseClient.postgrest["authors"]
                .select {
                    filter {
                        eq("name", name)
                    }
                }
                .decodeList<SupabaseAuthor>()

            if (existing.isNotEmpty()) {
                val existingAuthor = existing.first()
                val jsonObject = buildJsonObject {
                    put("id", existingAuthor.id)
                    put("name", name)
                    if (bio != null) put("bio", bio) else put("bio", JsonNull)
                }
                supabaseClient.postgrest["authors"].update(jsonObject) {
                    filter {
                        eq("id", existingAuthor.id)
                    }
                }
            } else {
                val id = java.util.UUID.randomUUID().toString()
                val jsonObject = buildJsonObject {
                    put("id", id)
                    put("name", name)
                    if (bio != null) put("bio", bio) else put("bio", JsonNull)
                }
                supabaseClient.postgrest["authors"].insert(jsonObject)
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error upserting author: ${e.message}", e)
        }
    }

    // Get categories from 'categories' table with a fallback using books or default list elements
    suspend fun getCategories(): List<SupabaseCategory> = withContext(Dispatchers.IO) {
        try {
            val dbCats = supabaseClient.postgrest["categories"]
                .select()
                .decodeList<SupabaseCategory>()
            
            if (dbCats.isEmpty()) {
                val defaultList = listOf("কুরআন", "হাদিস", "ফিকহ", "তাফসীর", "সীরাত", "অন্যান্য")
                val insertedList = mutableListOf<SupabaseCategory>()
                defaultList.forEach { name ->
                    val id = java.util.UUID.randomUUID().toString()
                    val jsonObject = buildJsonObject {
                        put("id", id)
                        put("name", name)
                    }
                    try {
                        supabaseClient.postgrest["categories"].insert(jsonObject)
                        insertedList.add(SupabaseCategory(id = id, name = name))
                    } catch (e: Exception) {
                        android.util.Log.e("SupabaseService", "Error pre-populating category $name: ${e.message}")
                    }
                }
                val resultList = if (insertedList.isNotEmpty()) {
                    insertedList.sortedBy { it.name }
                } else {
                    dbCats.sortedBy { it.name }
                }
                saveCategoriesLocally(resultList)
                resultList
            } else {
                val resultList = dbCats.sortedBy { it.name }
                saveCategoriesLocally(resultList)
                resultList
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error loading categories from categories table (using fallback): ${e.message}")
            val cached = loadCategoriesLocally()
            if (cached != null && cached.isNotEmpty()) {
                cached.sortedBy { it.name }
            } else {
                val defaultList = listOf("কুরআন", "হাদিস", "ফিকহ", "তাফসীর", "সীরাত", "অন্যান্য")
                try {
                    val books = supabaseClient.postgrest["books"].select().decodeList<SupabaseBook>()
                    val bookCategories = books.flatMap { it.category.split(",") }.map { it.trim() }.filter { it.isNotEmpty() }
                    val combined = (defaultList + bookCategories).distinct()
                    val resultList = combined.map {
                        SupabaseCategory(id = it.hashCode().toString(), name = it)
                    }.sortedBy { it.name }
                    saveCategoriesLocally(resultList)
                    resultList
                } catch (ex: Exception) {
                    val resultList = defaultList.map { SupabaseCategory(id = it.hashCode().toString(), name = it) }.sortedBy { it.name }
                    saveCategoriesLocally(resultList)
                    resultList
                }
            }
        }
    }

    // Add category as Supabase element
    suspend fun addCategory(name: String): Unit = withContext(Dispatchers.IO) {
        val id = java.util.UUID.randomUUID().toString()
        val newCat = SupabaseCategory(id = id, name = name)
        
        val current = loadCategoriesLocally() ?: getCategories()
        val updated = (current + newCat).distinctBy { it.name.trim().lowercase() }
        saveCategoriesLocally(updated)

        try {
            val jsonObject = buildJsonObject {
                put("id", id)
                put("name", name)
            }
            supabaseClient.postgrest["categories"].insert(jsonObject)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error adding category to table (ignoring): ${e.message}")
        }
    }

    // Update Category name
    suspend fun updateCategory(id: String, name: String): Unit = withContext(Dispatchers.IO) {
        val defaultList = listOf("কুরআন", "হাদিস", "ফিকহ", "তাফসীর", "সীরাত", "অন্যান্য")
        val currentCats = loadCategoriesLocally() ?: getCategories()
        val existing = currentCats.find { it.id == id }
        var oldName = existing?.name

        if (oldName == null) {
            oldName = defaultList.find { it.hashCode().toString() == id }
        }

        val updatedCats = currentCats.map { 
            if (it.id == id) it.copy(name = name) else it 
        }
        saveCategoriesLocally(updatedCats)

        try {
            val jsonObject = buildJsonObject {
                put("id", id)
                put("name", name)
            }
            var existsInDb = false
            try {
                val check = supabaseClient.postgrest["categories"].select {
                    filter {
                        eq("id", id)
                    }
                }.decodeList<SupabaseCategory>()
                existsInDb = check.isNotEmpty()
            } catch (e: Exception) {
                // ignore
            }

            if (existsInDb) {
                supabaseClient.postgrest["categories"].update(jsonObject) {
                    filter {
                        eq("id", id)
                    }
                }
            } else {
                supabaseClient.postgrest["categories"].insert(jsonObject)
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error updating/inserting category to categories table (ignoring): ${e.message}")
        }

        if (oldName != null && oldName.trim().lowercase() != name.trim().lowercase()) {
            try {
                val books = supabaseClient.postgrest["books"].select().decodeList<SupabaseBook>()
                books.forEach { book ->
                    val categoriesList = book.category.split(",").map { it.trim() }
                    if (categoriesList.any { it.equals(oldName, ignoreCase = true) }) {
                        val updatedCategories = categoriesList.map { 
                            if (it.equals(oldName, ignoreCase = true)) name else it 
                        }.joinToString(", ")
                        
                        val updatedBookObj = buildJsonObject {
                            put("category", updatedCategories)
                        }
                        supabaseClient.postgrest["books"].update(updatedBookObj) {
                            filter {
                                eq("id", book.id)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseService", "Error updating books: ${e.message}")
            }
        }
    }

    // Update Book details in Postgrest
    suspend fun updateBook(
        id: String,
        title: String,
        author: String,
        category: String,
        coverImageUrl: String? = null,
        fileUrl: String? = null,
        fileType: String? = null
    ): Unit = withContext(Dispatchers.IO) {
        val currentList = loadBooksLocally() ?: fetchPublicBooks()
        val existingBook = currentList.find { it.id == id }
        val updatedBook = SupabaseBook(
            id = id,
            title = title,
            author = author,
            category = category,
            coverImageUrl = coverImageUrl ?: existingBook?.coverImageUrl,
            fileUrl = fileUrl ?: existingBook?.fileUrl,
            fileType = fileType ?: existingBook?.fileType ?: "pdf"
        )
        trackUpdatedBookLocally(updatedBook)
        
        val finalBooks = applyLocalOverrides(currentList)
        saveBooksLocally(finalBooks)

        try {
            val jsonObject = buildJsonObject {
                put("title", title)
                put("author", author)
                put("category", category)
                if (coverImageUrl != null) put("cover_image_url", coverImageUrl)
                if (fileUrl != null) put("file_url", fileUrl)
                if (fileType != null) put("file_type", fileType)
            }
            supabaseClient.postgrest["books"].update(jsonObject) {
                filter {
                    eq("id", id)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error updating book: ${e.message}", e)
        }
    }

    // Delete Book detail
    suspend fun deleteBook(id: String): Unit = withContext(Dispatchers.IO) {
        trackDeletedBookLocally(id)
        val current = loadBooksLocally() ?: fetchPublicBooks()
        val updated = applyLocalOverrides(current)
        saveBooksLocally(updated)

        try {
            // Delete dependent progress rows first to respect foreign key constraint in Supabase
            try {
                supabaseClient.postgrest["book_progress"].delete {
                    filter {
                        eq("book_id", id)
                    }
                }
            } catch (ex: Exception) {
                android.util.Log.e("SupabaseService", "Error deleting dependent progress: ${ex.message}")
            }

            supabaseClient.postgrest["books"].delete {
                filter {
                    eq("id", id)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error deleting book: ${e.message}", e)
        }
    }

    // Delete Author detail
    suspend fun deleteAuthor(id: String): Unit = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest["authors"].delete {
                filter {
                    eq("id", id)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error deleting author: ${e.message}", e)
        }
    }

    // Delete Category detail
    suspend fun deleteCategory(id: String): Unit = withContext(Dispatchers.IO) {
        val defaultList = listOf("কুরআন", "হাদিস", "ফিকহ", "তাফসীর", "সীরাত", "অন্যান্য")
        val currentCats = loadCategoriesLocally() ?: getCategories()
        val existing = currentCats.find { it.id == id }
        var oldName = existing?.name

        if (oldName == null) {
            oldName = defaultList.find { it.hashCode().toString() == id }
        }

        // Update local cache
        val updatedCats = currentCats.filter { it.id != id }
        saveCategoriesLocally(updatedCats)

        try {
            supabaseClient.postgrest["categories"].delete {
                filter {
                    eq("id", id)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "Error deleting from categories table (ignoring): ${e.message}")
        }

        if (oldName != null) {
            try {
                val books = supabaseClient.postgrest["books"].select().decodeList<SupabaseBook>()
                books.forEach { book ->
                    val categoriesList = book.category.split(",").map { it.trim() }
                    if (categoriesList.any { it.equals(oldName, ignoreCase = true) }) {
                        val updatedCategoriesList = categoriesList.filter { !it.equals(oldName, ignoreCase = true) }
                        val updatedCategories = if (updatedCategoriesList.isEmpty()) "অন্যান্য" else updatedCategoriesList.joinToString(", ")
                        
                        val updatedBookObj = buildJsonObject {
                            put("category", updatedCategories)
                        }
                        supabaseClient.postgrest["books"].update(updatedBookObj) {
                            filter {
                                eq("id", book.id)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseService", "Error updating books after category deletion: ${e.message}")
            }
        }
    }

    suspend fun addAuthor(name: String, bio: String? = null): Unit = withContext(Dispatchers.IO) {
        try {
            val id = java.util.UUID.randomUUID().toString()
            val jsonObject = buildJsonObject {
                put("id", id)
                put("name", name)
                if (bio != null) put("bio", bio) else put("bio", JsonNull)
            }
            supabaseClient.postgrest["authors"].insert(jsonObject)
        } catch (e: Exception) {
            android.util.Log.w("SupabaseService", "Error adding author to authors table (ignoring as fallback if table missing): ${e.message}", e)
        }
    }
}
