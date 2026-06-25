package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.epub.EpubReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder

class EpubReaderViewModel : ViewModel() {

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    private val _scrollOffset = MutableStateFlow(0f)
    val scrollOffset: StateFlow<Float> = _scrollOffset.asStateFlow()

    fun loadBook(context: Context, bookId: String, fileUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val decodedFileUrl = URLDecoder.decode(fileUrl, "UTF-8")
                val inputStream = if (decodedFileUrl.startsWith("http")) {
                    val file = File(context.cacheDir, "temp_epub_$bookId.epub")
                    if (!file.exists()) {
                        val client = okhttp3.OkHttpClient()
                        val request = okhttp3.Request.Builder().url(decodedFileUrl).build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            response.body?.byteStream()?.use { input ->
                                file.outputStream().use { output -> input.copyTo(output) }
                            }
                        } else {
                            throw Exception("Network error: ${response.code}")
                        }
                    }
                    FileInputStream(file)
                } else {
                    FileInputStream(File(decodedFileUrl))
                }

                val epubReader = EpubReader()
                val loadedBook = epubReader.readEpub(inputStream)
                
                // Load progress
                val prefs = context.getSharedPreferences("EpubPrefs", Context.MODE_PRIVATE)
                val savedChapter = prefs.getInt("chapter_$bookId", 0)
                val savedScroll = prefs.getFloat("scroll_$bookId", 0f)

                withContext(Dispatchers.Main) {
                    _book.value = loadedBook
                    _currentChapterIndex.value = savedChapter
                    _scrollOffset.value = savedScroll
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "বই লোড করতে সমস্যা হয়েছে: ${e.message}"
                    _isLoading.value = false
                }
            }
        }
    }

    fun updateProgress(context: Context, bookId: String, chapterIndex: Int, scrollOffset: Float) {
        _currentChapterIndex.value = chapterIndex
        _scrollOffset.value = scrollOffset
        
        val prefs = context.getSharedPreferences("EpubPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("chapter_$bookId", chapterIndex)
            .putFloat("scroll_$bookId", scrollOffset)
            .apply()
    }
}
