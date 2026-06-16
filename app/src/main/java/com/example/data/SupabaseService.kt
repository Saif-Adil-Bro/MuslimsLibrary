package com.example.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
    private val supabaseClient: SupabaseClient
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
}
