package com.example.data.repository

import com.example.data.model.Book
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BookRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : BookRepository {

    // Elegant, highly realistic starter library for MuslimsLibrary
    private val starterBooks = listOf(
        Book(
            id = "1",
            title = "Riyad as-Salihin (رياض الصالحين)",
            author = "Imam Al-Nawawi",
            description = "A classic compilation of verses from the Qur'an and Hadiths, categorized across various moral and ethical themes of Islamic life.",
            coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=400",
            pdfUrl = "riyad_as_salihin.pdf",
            category = "Hadith",
            sizeMb = 4.2,
            pages = 350
        ),
        Book(
            id = "2",
            title = "Al-Aqidah Al-Wasitiyyah (العقيدة الواسطية)",
            author = "Ibn Taymiyyah",
            description = "An essential treatise outlining the creed of Ahlus-Sunnah wal-Jama'ah with precise logical and scriptural evidence.",
            coverUrl = "https://images.unsplash.com/photo-1506880018603-83d5b814b5a6?auto=format&fit=crop&q=80&w=400",
            pdfUrl = "al_aqidah.pdf",
            category = "Aqidah",
            sizeMb = 1.8,
            pages = 120
        ),
        Book(
            id = "3",
            title = "The Sealed Nectar (Ar-Raheeq Al-Makhtum)",
            author = "Safiur Rahman Mubarakpuri",
            description = "A detailed, award-winning biography of Prophet Muhammad (peace be upon him) tracking his noble life throughout Mecca and Medina.",
            coverUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&q=80&w=400",
            pdfUrl = "sealed_nectar.pdf",
            category = "Seerah",
            sizeMb = 8.5,
            pages = 580
        )
    )

    override fun getBooks(): Flow<List<Book>> = flow {
        try {
            // Attempt to query live Supabase table
            val booksFromDb = supabaseClient.postgrest["books"].select().decodeList<Book>()
            if (booksFromDb.isNotEmpty()) {
                emit(booksFromDb)
            } else {
                emit(starterBooks)
            }
        } catch (e: Exception) {
            // Fail safe, allow developer testing out-of-the-box
            emit(starterBooks)
        }
    }

    override suspend fun getBookById(id: String): Book? {
        return try {
            supabaseClient.postgrest["books"].select {
                filter {
                    eq("id", id)
                }
            }.decodeSingle<Book>()
        } catch (e: Exception) {
            starterBooks.find { it.id == id }
        }
    }

    override suspend fun searchBooks(query: String): List<Book> {
        return try {
            supabaseClient.postgrest["books"].select {
                filter {
                    ilike("title", "%$query%")
                }
            }.decodeList<Book>()
        } catch (e: Exception) {
            starterBooks.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true)
            }
        }
    }

    override suspend fun downloadBookFile(pdfUrl: String): ByteArray {
        return try {
            supabaseClient.storage.from("books").downloadPublic(pdfUrl)
        } catch (e: Exception) {
            ByteArray(0)
        }
    }
}
