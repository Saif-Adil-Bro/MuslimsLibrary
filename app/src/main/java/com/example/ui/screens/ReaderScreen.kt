package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

enum class ReadTheme {
    LIGHT, SEPIA, DARK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    bookTitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var readTheme by remember { mutableStateOf(ReadTheme.LIGHT) }
    var fontSize by remember { mutableStateOf(16f) }
    var currentPage by remember { mutableStateOf(0) }
    var isBookmarked by remember { mutableStateOf(false) }
    var pdfBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoadingPdf by remember { mutableStateOf(true) }

    // Color definitions for Themes
    val backgroundColor = when (readTheme) {
        ReadTheme.LIGHT -> Color(0xFFFCFDF9)
        ReadTheme.SEPIA -> Color(0xFFF4ECD8)
        ReadTheme.DARK -> Color(0xFF121212)
    }

    val textColor = when (readTheme) {
        ReadTheme.LIGHT -> Color(0xFF1D1B16)
        ReadTheme.SEPIA -> Color(0xFF433422)
        ReadTheme.DARK -> Color(0xFFE6E1E5)
    }

    // Load sample PDF from bundle/assets using native PdfRenderer
    LaunchedEffect(bookId) {
        isLoadingPdf = true
        withContext(Dispatchers.IO) {
            try {
                // Copy dummy assets if they don't exist to simulate real PDF loading
                val fileName = "sample_${bookId}.pdf"
                val file = File(context.cacheDir, fileName)
                if (!file.exists()) {
                    val isStream: InputStream = context.assets.open("dummy.pdf")
                    val outStream = FileOutputStream(file)
                    isStream.copyTo(outStream)
                    isStream.close()
                    outStream.close()
                }

                // Initialize PdfRenderer
                val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fileDescriptor)
                val pageCount = renderer.pageCount
                val bitmaps = mutableListOf<Bitmap>()

                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    // High-quality bitmap rendering
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                    page.close()
                }
                renderer.close()
                fileDescriptor.close()

                withContext(Dispatchers.Main) {
                    pdfBitmaps = bitmaps
                    isLoadingPdf = false
                }
            } catch (e: Exception) {
                // Handle missing real PDF file gracefully, use simulated pages for robust fallback
                withContext(Dispatchers.Main) {
                    isLoadingPdf = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = bookTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Font adjustment action
                    IconButton(onClick = {
                        fontSize = if (fontSize >= 24f) 14f else fontSize + 2f
                    }) {
                        Icon(Icons.Default.FormatSize, contentDescription = "Font size")
                    }
                    // Theme Switcher action
                    IconButton(onClick = {
                        readTheme = when (readTheme) {
                            ReadTheme.LIGHT -> ReadTheme.SEPIA
                            ReadTheme.SEPIA -> ReadTheme.DARK
                            ReadTheme.DARK -> ReadTheme.LIGHT
                        }
                    }) {
                        Icon(
                            imageVector = if (readTheme == ReadTheme.DARK) Icons.Default.LightMode else Icons.Default.NightlightRound,
                            contentDescription = "Read Theme"
                        )
                    }
                    // Bookmark action
                    IconButton(onClick = { isBookmarked = !isBookmarked }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = textColor,
                    actionIconContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoadingPdf) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (pdfBitmaps.isNotEmpty()) {
                // Live Native PDF view rendered perfectly
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(pdfBitmaps) { index, bitmap ->
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // Beautiful interactive EPUB/Book content simulation (Fallback/Local testing)
                val totalChapterPages = 15
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Page Head
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Chapter ${ (currentPage / 3) + 1 }",
                                    fontSize = 12.sp,
                                    color = textColor.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Book Progress ${ (currentPage + 1) * 100 / totalChapterPages }%",
                                    fontSize = 12.sp,
                                    color = textColor.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Divider(color = textColor.copy(alpha = 0.15f))

                            // Text Body
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                fontSize = (fontSize + 6).sp,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )

                            Text(
                                text = getSimulatedBookText(currentPage),
                                fontSize = fontSize.sp,
                                color = textColor,
                                fontFamily = FontFamily.Serif,
                                lineHeight = (fontSize * 1.5).sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Bottom Navigation Indicators
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (currentPage > 0) currentPage-- },
                                enabled = currentPage > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = textColor.copy(alpha = 0.1f),
                                    contentColor = textColor
                                )
                            ) {
                                Text("Previous")
                            }

                            Text(
                                text = "Page ${currentPage + 1} of $totalChapterPages",
                                fontSize = 14.sp,
                                color = textColor,
                                fontWeight = FontWeight.Medium
                            )

                            Button(
                                onClick = { if (currentPage < totalChapterPages - 1) currentPage++ },
                                enabled = currentPage < totalChapterPages - 1,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = textColor.copy(alpha = 0.1f),
                                    contentColor = textColor
                                )
                            ) {
                                Text("Next")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Generates highly classical Islamic textual details for library simulation
private fun getSimulatedBookText(page: Int): String {
    return when (page) {
        0 -> "It was compiled with the purest intentions to revive religious discipline. Seeking absolute truth begins with sincere intention (Niyyah). Truly, actions are judged solely by intentions, and every person will receive only what they intended. Whosoever migrates for worldly gains or a matrimonial alliance, their migration is evaluated accordingly."
        1 -> "The excellence of knowledge and acquiring understanding of divine laws cannot be understated. 'Whosoever Allah intends goodness for, He grants them deep understanding of deen.' This includes active contemplation, memorisation, and constant implementation in daily life. Live with purpose and act with truth."
        2 -> "To build a character that mirrors the Prophet (peace be upon him), one must implement Riyad as-Salihin's chapters on patience, truthfulness, and persistence. Standing up for righteousness under strenuous external circumstances is the peak of faith. Real spiritual victory is maintaining inner peace through steady acts of devotion."
        3 -> "Al-Aqidah Al-Wasitiyyah focuses on the attributes of Allah, rejecting both extreme anthropomorphism and complete negation. The balanced path represents the golden mean of Islamic theology. Affirm what has been revealed in scripture without distortion (Tahrif) or speculation (Takyif)."
        4 -> "Understanding Seerah teaches us how to apply faith in statehood, community-building, and personal relations. The Meccan phase was defined by spiritual resilience, building the foundation of pure monotheism. The Medinan phase transitioned this spiritual paradigm into a lively community governed by ethical principles."
        else -> "Continuing within the great compiled manuscripts of the classical Islamic legal and spiritual traditions. The scholars of the past spent countless hours traveling from lands to lands to gather even a single narration of absolute truth. Keep reading, keep learning, and internalise this divine path."
    }
}
