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
    @SerialName("created_at") val createdAt: String? = null
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

class SupabaseService(
    private val supabaseClient: SupabaseClient,
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
}
