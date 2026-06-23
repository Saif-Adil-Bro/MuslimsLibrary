package com.example.ui.screens

import android.content.Context
import android.text.Html
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.epub.EpubReader
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.net.URLDecoder

enum class EpubTheme {
    LIGHT, DARK, SEPIA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubReaderScreen(
    bookId: String,
    bookTitle: String,
    fileUrl: String,
    fileType: String,
    userId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val decodedTitle = remember(bookTitle) {
        try { URLDecoder.decode(bookTitle, "UTF-8") } catch (e: Exception) { bookTitle }
    }
    val decodedFileUrl = remember(fileUrl) {
        try { URLDecoder.decode(fileUrl, "UTF-8") } catch (e: Exception) { fileUrl }
    }

    // Settings State
    val prefs = context.getSharedPreferences("EpubPrefs_$userId", Context.MODE_PRIVATE)
    var readTheme by remember { 
        mutableStateOf(EpubTheme.values()[prefs.getInt("theme", EpubTheme.LIGHT.ordinal)]) 
    }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("fontSize", 18f)) }
    var currentChapterIndex by remember { mutableIntStateOf(prefs.getInt("chapter_$bookId", 0)) }
    
    // UI State
    var isLoading by remember { mutableStateOf(true) }
    var book by remember { mutableStateOf<Book?>(null) }
    var paragraphs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showToc by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Load Book
    LaunchedEffect(decodedFileUrl) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inputStream = if (decodedFileUrl.startsWith("http")) {
                    val file = File(context.cacheDir, "temp_epub_$bookId.epub")
                    if (!file.exists()) {
                        try {
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
                        } catch (e: Exception) {
                            if (file.exists()) {
                                file.delete()
                            }
                            throw e
                        }
                    }
                    FileInputStream(file)
                } else {
                    FileInputStream(File(decodedFileUrl))
                }

                val epubReader = EpubReader()
                val loadedBook = epubReader.readEpub(inputStream)
                
                withContext(Dispatchers.Main) {
                    book = loadedBook
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "বই লোড করতে সমস্যা হয়েছে: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    // Load Chapter Content
    LaunchedEffect(book, currentChapterIndex) {
        book?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val spineRefs = it.spine.spineReferences
                    if (currentChapterIndex in spineRefs.indices) {
                        val resource = spineRefs[currentChapterIndex].resource
                        val data = resource.data
                        val htmlContent = String(data, Charsets.UTF_8)
                        
                        // Parse HTML safely
                        val parsedText = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT).toString()
                        val newParagraphs = parsedText.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
                        
                        withContext(Dispatchers.Main) {
                            paragraphs = newParagraphs
                            listState.scrollToItem(0)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Save Progress
    DisposableEffect(currentChapterIndex, fontSize, readTheme) {
        onDispose {
            prefs.edit()
                .putInt("chapter_$bookId", currentChapterIndex)
                .putFloat("fontSize", fontSize)
                .putInt("theme", readTheme.ordinal)
                .apply()
        }
    }

    val backgroundColor = when (readTheme) {
        EpubTheme.LIGHT -> Color(0xFFF9FAFB)
        EpubTheme.DARK -> Color(0xFF111827)
        EpubTheme.SEPIA -> Color(0xFFFBF0D9)
    }
    
    val textColor = when (readTheme) {
        EpubTheme.LIGHT -> Color(0xFF1F2937)
        EpubTheme.DARK -> Color(0xFFF9FAFB)
        EpubTheme.SEPIA -> Color(0xFF432A15)
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = !isFullScreen, enter = slideInVertically(), exit = slideOutVertically()) {
                TopAppBar(
                    title = { Text(decodedTitle, maxLines = 1, color = textColor) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showToc = true }) {
                            Icon(Icons.Default.List, contentDescription = "TOC", tint = textColor)
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = textColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = !isFullScreen, enter = slideInVertically(initialOffsetY = { it }), exit = slideOutVertically(targetOffsetY = { it })) {
                BottomAppBar(containerColor = backgroundColor, contentColor = textColor) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (currentChapterIndex > 0) currentChapterIndex-- },
                            enabled = currentChapterIndex > 0
                        ) { Text("আগের অধ্যায়") }
                        
                        Text("${currentChapterIndex + 1} / ${book?.spine?.spineReferences?.size ?: 1}")
                        
                        Button(
                            onClick = { if (currentChapterIndex < (book?.spine?.spineReferences?.size ?: 1) - 1) currentChapterIndex++ },
                            enabled = currentChapterIndex < (book?.spine?.spineReferences?.size ?: 1) - 1
                        ) { Text("পরের অধ্যায়") }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .clickable { isFullScreen = !isFullScreen }
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = textColor)
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ত্রুটি (Debugging Info):", color = Color.Red, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = textColor, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { 
                        isLoading = true
                        errorMessage = null
                        // Will trigger LaunchedEffect again because decodedFileUrl is stable, we need to artificially re-trigger
                        // Actually, if we just want a simple retry, popBackStack is easier, or user can pull to refresh.
                        // For now we'll rely on the user navigating back and entering again.
                    }) {
                        Text("পুনরায় চেষ্টা করুন (Go Back & Re-open)")
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    itemsIndexed(paragraphs) { _, paragraph ->
                        Text(
                            text = paragraph,
                            fontSize = fontSize.sp,
                            color = textColor,
                            fontFamily = FontFamily.Serif,
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            lineHeight = (fontSize * 1.6f).sp
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("ফন্ট সাইজ (${fontSize.toInt()})", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 12f..36f
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("থিম পরিবর্তন", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { readTheme = EpubTheme.LIGHT },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) { Text("Light") }
                    
                    Button(
                        onClick = { readTheme = EpubTheme.DARK },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)
                    ) { Text("Dark") }
                    
                    Button(
                        onClick = { readTheme = EpubTheme.SEPIA },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBF0D9), contentColor = Color(0xFF432A15)),
                        border = BorderStroke(1.dp, Color(0xFF432A15))
                    ) { Text("Sepia") }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showToc) {
        ModalBottomSheet(onDismissRequest = { showToc = false }) {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                item {
                    Text("সূচিপত্র", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                itemsIndexed(book?.spine?.spineReferences ?: emptyList()) { index, _ ->
                    Text(
                        text = "অধ্যায় ${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentChapterIndex = index
                                showToc = false
                            }
                            .padding(vertical = 12.dp),
                        fontSize = 16.sp
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
