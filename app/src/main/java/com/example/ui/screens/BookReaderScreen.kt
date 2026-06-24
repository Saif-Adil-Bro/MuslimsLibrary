package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.widget.Toast
import android.util.Log
import android.view.ScaleGestureDetector
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MuslimsLibraryApplication
import com.example.data.repository.LocalSyncRepository
import com.example.ui.viewmodel.BookReaderState
import com.example.ui.viewmodel.BookReaderViewModel
import java.net.URLDecoder
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    bookId: String,
    bookTitle: String,
    fileUrl: String = "",
    fileType: String = "pdf",
    userId: String = "User@muslimslibrary.org",
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as MuslimsLibraryApplication).container
    val localSyncRepository = appContainer.localSyncRepository
    val bookRepository = appContainer.bookRepository

    val viewModel: BookReaderViewModel = viewModel(
        factory = BookReaderViewModel.Factory(
            localSyncRepository = localSyncRepository,
            bookRepository = bookRepository,
            bookId = bookId,
            userId = userId
        )
    )

    val rState by viewModel.currentState.collectAsState()

    // Dispose effect: SAVE ON SCREEN CLOSE (Requirement 3)
    DisposableEffect(Unit) {
        onDispose {
            Log.d("PDF_Reader", "BookReaderScreen disposed - executing final save")
            viewModel.saveFinalProgress()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    var readTheme by remember { mutableStateOf(ReadTheme.LIGHT) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isWebLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }

    var isPdfReady by remember { mutableStateOf(false) }
    var isZooming by remember { mutableStateOf(false) }
    var zoomEndTime by remember { mutableStateOf(0L) }

    val scaleGestureDetector = remember {
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                return true
            }
        })
    }

    // Safely Decode URL & Title (Handle Bengali and special characters)
    val decodedTitle = remember(bookTitle) {
        try {
            URLDecoder.decode(bookTitle, "UTF-8")
        } catch (e: Exception) {
            bookTitle
        }
    }
    
    val decodedFileUrl = remember(fileUrl) {
        try {
            URLDecoder.decode(fileUrl, "UTF-8")
        } catch (e: Exception) {
            fileUrl
        }
    }

    val onScroll = rememberUpdatedState { scrollY: Int, range: Int, viewHeight: Int ->
        if (isZooming) {
            Log.d("PDF_Debug", "Skipping progress updates and page changes because zoom is in progress")
            return@rememberUpdatedState
        }
        val total = rState.totalPages
        val current = rState.currentPage
        val maxScroll = range - viewHeight
        if (maxScroll > 0 && total > 0) {
            val progressPercent = scrollY.toFloat() / maxScroll.toFloat()
            val calculatedPage = (1 + progressPercent * (total - 1)).toInt().coerceIn(1, total)
            if (calculatedPage != current) {
                viewModel.onPageChanged(calculatedPage, total)
            }
        }
    }

    // Fetch the real book's page count if we don't have progress in Room DB, or as secondary safe fallback
    LaunchedEffect(bookId, decodedFileUrl, retryTrigger) {
        Log.d("PDF_Debug", "=== BOOK READER LOADED ===")
        Log.d("PDF_Debug", "Book ID: $bookId")
        Log.d("PDF_Debug", "PDF Path: $decodedFileUrl")
        Log.d("PDF_Debug", "Initial state: currentPage=${rState.currentPage}, totalPages=${rState.totalPages}")
        isWebLoading = true
        hasError = false

        try {
            val book = bookRepository.getBookById(bookId)
            if (book != null && book.pages > 0) {
                Log.d("PDF_Reader", "Loaded book page count from database: ${book.pages}")
                viewModel.setTotalPages(book.pages)
            }
        } catch (e: Exception) {
            Log.e("PDF_Reader", "Failed to load book page count: ${e.message}")
        }

        // Get actual page count from PDF document in background
        if (decodedFileUrl.isNotEmpty() && !decodedFileUrl.lowercase().endsWith(".epub") && fileType.lowercase() != "epub") {
            try {
                Log.d("PDF_Debug", "Initiating background PDF download & page check...")
                Log.d("PDF_Debug", "PDF Path: $decodedFileUrl")
                val realPages = downloadAndGetPdfPageCount(context, decodedFileUrl, bookId)
                if (realPages > 0) {
                    viewModel.setTotalPages(realPages)
                    isPdfReady = true
                    isWebLoading = false
                    val currentPg = rState.currentPage
                    Log.d("PDF_Debug", "Total pages from PDF: $realPages")
                    Log.d("PDF_Debug", "Current page: $currentPg")
                    Log.d("PDF_Debug", "Progress: $currentPg / $realPages")
                } else {
                    Log.w("PDF_Debug", "Warning: Real page detection returned non-positive value: $realPages")
                    hasError = true
                    isWebLoading = false
                }
            } catch (e: Exception) {
                Log.e("PDF_Debug", "Error while attempting to download and read pages from PDF", e)
                hasError = true
                isWebLoading = false
            }
        } else {
            isWebLoading = false
        }
    }

    // Color theme setups
    val isDark = readTheme == ReadTheme.DARK
    val backgroundColor = when (readTheme) {
        ReadTheme.LIGHT -> MaterialTheme.colorScheme.background
        ReadTheme.SEPIA -> Color(0xFFFBF4E4)
        ReadTheme.DARK -> Color(0xFF161C1A)
    }
    val contentColor = when (readTheme) {
        ReadTheme.LIGHT -> MaterialTheme.colorScheme.tertiary
        ReadTheme.SEPIA -> Color(0xFF4C381E)
        ReadTheme.DARK -> Color(0xFFECECEC)
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = decodedTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                maxLines = 1,
                                color = contentColor
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to list",
                                    tint = contentColor
                                )
                            }
                        },
                        actions = {
                            // Toggle Read Theme
                            IconButton(onClick = {
                                readTheme = when (readTheme) {
                                    ReadTheme.LIGHT -> ReadTheme.SEPIA
                                    ReadTheme.SEPIA -> ReadTheme.DARK
                                    ReadTheme.DARK -> ReadTheme.LIGHT
                                }
                            }) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.NightlightRound,
                                    contentDescription = "Shift theme mode",
                                    tint = contentColor
                                )
                            }

                            // External Browser Fallback
                            if (decodedFileUrl.isNotEmpty()) {
                                IconButton(onClick = {
                                    try {
                                        val directUrl = convertGoogleDriveLink(decodedFileUrl)
                                        val intent = Intent(Intent.ACTION_VIEW, directUrl.toUri())
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "ব্রাউজারে খোলা অসম্ভব", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Open in Web Browser",
                                        tint = contentColor
                                    )
                                }

                                // Download locally
                                IconButton(onClick = {
                                    downloadPdf(context, decodedFileUrl, decodedTitle)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download Book file",
                                        tint = contentColor
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = backgroundColor,
                            titleContentColor = contentColor
                        )
                    )
                    
                    // Subtle dynamic progress & sync header row (Material 3 density layout)
                    val isSyncingState by localSyncRepository.isSyncing().collectAsState(initial = false)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF1E2623) else Color(0xFFECECEC).copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পৃষ্ঠা ${rState.currentPage} / ${rState.totalPages} (${rState.progressPercent.toInt()}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSyncingState) Icons.Default.CloudSync else Icons.Default.CloudQueue,
                                contentDescription = "Sync state",
                                tint = if (isSyncingState) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                              )
                              Text(
                                  text = if (isSyncingState) "সিঙ্ক হচ্ছে..." else "ডিভাইস সিঙ্কড",
                                  fontSize = 10.sp,
                                  color = contentColor.copy(alpha = 0.5f)
                              )
                          }
                      }

                      // Subtle non-intrusive LinearProgressIndicator from user requirements
                      LinearProgressIndicator(
                          progress = { if (rState.totalPages > 0) (rState.currentPage.toFloat() / rState.totalPages.toFloat()).coerceIn(0f, 1f) else 0f },
                          color = MaterialTheme.colorScheme.primary,
                          trackColor = if (isDark) Color(0xFF1E2623) else Color(0xFFECECEC),
                          modifier = Modifier
                              .fillMaxWidth()
                              .height(3.dp)
                      )
                  }
              }
          },
          containerColor = backgroundColor,
          modifier = modifier.fillMaxSize()
      ) { innerPadding ->
          Box(
              modifier = Modifier
                  .fillMaxSize()
                  .background(backgroundColor)
                  .padding(if (isFullScreen) PaddingValues(0.dp) else innerPadding)
          ) {
              // Main Content Area
              if (decodedFileUrl.isEmpty()) {
                  // FALLBACK: User opened local test or simulated content
                  SimulatedBookReadingView(
                      title = decodedTitle,
                      themeColor = contentColor,
                      backgroundColor = backgroundColor,
                      currentPage = rState.currentPage - 1,
                      totalSimulatedPages = 6,
                      onPageChanged = { pageIndex ->
                          val newPage = pageIndex + 1
                          viewModel.onPageChanged(newPage, 6)
                      }
                  )
              } else if (fileType.lowercase() == "epub") {
                  // Handle unsupported EPUB directly within the UI gracefully
                  Box(
                      modifier = Modifier
                          .fillMaxSize()
                          .padding(24.dp),
                      contentAlignment = Alignment.Center
                  ) {
                      Card(
                          modifier = Modifier.fillMaxWidth(),
                          colors = CardDefaults.cardColors(
                              containerColor = if (isDark) Color(0xFF1E2623) else Color(0xFFF3F4F1)
                          ),
                          shape = RoundedCornerShape(16.dp)
                      ) {
                          Column(
                              modifier = Modifier.padding(24.dp),
                              horizontalAlignment = Alignment.CenterHorizontally,
                              verticalArrangement = Arrangement.spacedBy(16.dp)
                          ) {
                              Text("📖", fontSize = 48.sp)
                              Text(
                                  text = "EPUB ফাইল সনাক্ত হয়েছে",
                                  fontSize = 18.sp,
                                  fontWeight = FontWeight.Bold,
                                  color = contentColor,
                                  textAlign = TextAlign.Center
                              )
                              Text(
                                  text = "EPUB ফর্ম্যাট সরাসরি অ্যাপের ভেতরে পড়ার চেয়ে প্লে-স্টোরের জনপ্রিয় EPUB রিডার দিয়ে পড়া আরামদায়ক। আপনি ফাইলটি ডাউনলোড করতে পারেন অথবা গুগল ড্রাইভে ওপেন করতে পারেন।",
                                  fontSize = 14.sp,
                                  color = contentColor.copy(alpha = 0.8f),
                                  textAlign = TextAlign.Center,
                                  lineHeight = 20.sp
                              )
                              
                              Row(
                                  modifier = Modifier.fillMaxWidth(),
                                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                              ) {
                                  Button(
                                      onClick = { downloadPdf(context, decodedFileUrl, decodedTitle) },
                                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                      modifier = Modifier.weight(1f)
                                  ) {
                                      Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                      Spacer(modifier = Modifier.width(4.dp))
                                      Text("ডাউনলোড", fontSize = 12.sp)
                                  }
                                  
                                  Button(
                                      onClick = {
                                          try {
                                              val intent = Intent(Intent.ACTION_VIEW, convertGoogleDriveLink(decodedFileUrl).toUri())
                                              context.startActivity(intent)
                                          } catch (e: Exception) {
                                              Toast.makeText(context, "ড্রাইভ খোলা যাচ্ছে না", Toast.LENGTH_SHORT).show()
                                          }
                                      },
                                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                      modifier = Modifier.weight(1f)
                                  ) {
                                      Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                      Spacer(modifier = Modifier.width(4.dp))
                                      Text("ড্রাইভে দেখুন", fontSize = 12.sp)
                                  }
                              }
                          }
                      }
                  }
              } else {
                  val cacheFile = remember(decodedFileUrl, bookId) { File(context.cacheDir, "book_${bookId}.pdf") }
                  if (isPdfReady && cacheFile.exists() && cacheFile.length() > 0) {
                      Box(modifier = Modifier.fillMaxSize()) {
                          NativePdfReader(
                              file = cacheFile,
                              initialPage = rState.currentPage,
                              isZooming = isZooming,
                              onZoomStateChanged = { isZooming = it },
                              onPageChanged = { page, total ->
                                  viewModel.onPageChanged(page, total)
                              },
                              theme = readTheme,
                              modifier = Modifier.fillMaxSize()
                          )
                      }
                  } else {
                      // Native PDF Reader
                   val cacheFile = remember(decodedFileUrl, bookId) { File(context.cacheDir, "book_${bookId}.pdf") }
                  val directDocUrl = convertGoogleDriveLink(decodedFileUrl)
                  val docsViewerUrl = ""

                  Box(modifier = Modifier.fillMaxSize()) {
                      if (hasError) {
                          // Error fallback panel
                          Box(
                              modifier = Modifier
                                  .fillMaxSize()
                                  .padding(24.dp),
                              contentAlignment = Alignment.Center
                          ) {
                              Column(
                                  horizontalAlignment = Alignment.CenterHorizontally,
                                  verticalArrangement = Arrangement.spacedBy(12.dp)
                              ) {
                                  Text("⚠️", fontSize = 40.sp)
                                  Text(
                                      text = "বইটি লোড করতে সমস্যা হচ্ছে",
                                      fontSize = 16.sp,
                                      fontWeight = FontWeight.Bold,
                                      color = contentColor
                                  )
                                  Text(
                                      text = "ইন্টারনেট সংযোগ চেক করুন অথবা সরাসরি ব্রাউজার বা গুগল ড্রাইভ ব্যবহার করে বইটি দেখুন।",
                                      fontSize = 12.sp,
                                      color = Color.Gray,
                                      textAlign = TextAlign.Center,
                                      modifier = Modifier.padding(horizontal = 16.dp)
                                  )
                                  Spacer(modifier = Modifier.height(8.dp))
                                  Button(
                                      onClick = {
                                          hasError = false
                                          isWebLoading = true
                                          retryTrigger++
                                      },
                                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                  ) {
                                      Text("আবার চেষ্টা করুন")
                                  }
                                  
                                  OutlinedButton(
                                      onClick = {
                                          val intent = Intent(Intent.ACTION_VIEW, directDocUrl.toUri())
                                          context.startActivity(intent)
                                      }
                                  ) {
                                      Text("ব্রাউজারে সরাসরি খুলুন", color = contentColor)
                                  }
                              }
                          }
                      } else {
                          // Embedded interactive PDF WebView
                          key(retryTrigger) {
                              // AndroidView WebView replaced
                              if (true) {
                                  Spacer(modifier = Modifier)
                              }
                              AndroidView(
                                  factory = { ctx ->
                                      ObservableWebView(ctx).apply {
                                          settings.apply {
                                              javaScriptEnabled = true
                                              builtInZoomControls = true
                                              displayZoomControls = false
                                              domStorageEnabled = true
                                              useWideViewPort = true
                                              loadWithOverviewMode = true
                                              setSupportZoom(true)
                                              allowFileAccess = true
                                          }
                                          webViewClient = object : WebViewClient() {
                                              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                  super.onPageStarted(view, url, favicon)
                                                  isWebLoading = true
                                              }

                                              override fun onPageFinished(view: WebView?, url: String?) {
                                                  super.onPageFinished(view, url)
                                                  isWebLoading = false
                                              }

                                              override fun onReceivedError(
                                                  view: WebView?,
                                                  request: WebResourceRequest?,
                                                  error: WebResourceError?
                                              ) {
                                                  super.onReceivedError(view, request, error)
                                                  if (request?.isForMainFrame == true) {
                                                      hasError = true
                                                  }
                                              }
                                          }
                                          webChromeClient = WebChromeClient()

                                          // REQUIREMENT 1: Update reading progress on scroll
                                          setOnTouchListener { _, event ->
                                               scaleGestureDetector.onTouchEvent(event)
                                               false
                                           }

                                           onScrollChangedListener = { _, scrollY, range, viewHeight -> onScroll.value(scrollY, range, viewHeight) }
                                            val dummyOnScrollChangedListener = { _: Int, scrollY: Int, range: Int, viewHeight: Int ->
                                              val maxScroll = range - viewHeight
                                              if (maxScroll > 0) {
                                                  val progressPercent = scrollY.toFloat() / maxScroll.toFloat()
                                                  val calculatedPage = (1 + progressPercent * (rState.totalPages - 1)).toInt().coerceIn(1, rState.totalPages)
                                                  if (calculatedPage != rState.currentPage) {
                                                      Unit
                                                  }
                                              }
                                          }

                                          loadUrl(docsViewerUrl)
                                      }
                                  },
                                  modifier = Modifier.fillMaxSize()
                              )
                          }
                      }

                      // Foreground Custom Progressive Loading bar
                      if (isWebLoading && !hasError) {
                          Box(
                              modifier = Modifier
                                  .fillMaxSize()
                                  .background(backgroundColor.copy(alpha = 0.9f)),
                              contentAlignment = Alignment.Center
                          ) {
                              Column(
                                  horizontalAlignment = Alignment.CenterHorizontally,
                                  verticalArrangement = Arrangement.spacedBy(16.dp)
                              ) {
                                  CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                  Text(
                                      text = "বইটি লোড হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন...",
                                      fontSize = 14.sp,
                                      color = contentColor,
                                      fontWeight = FontWeight.Medium,
                                      textAlign = TextAlign.Center,
                                      modifier = Modifier.padding(horizontal = 24.dp)
                                  )
                                  Spacer(modifier = Modifier.height(12.dp))
                                  // Quick Direct download or fallback link while loading
                                  OutlinedButton(
                                      onClick = {
                                          val intent = Intent(Intent.ACTION_VIEW, directDocUrl.toUri())
                                          context.startActivity(intent)
                                      },
                                      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                  ) {
                                      Text("লোড না হলে ড্রাইভে দেখুন (External Web View)")
                                  }
                              }
                          }
                      }
                  }
                  }
              }
              // Floating Full Screen toggle control overlay (bottom-right corner)
              IconButton(
                  onClick = { isFullScreen = !isFullScreen },
                  modifier = Modifier
                      .padding(24.dp)
                      .size(48.dp)
                      .align(Alignment.BottomEnd)
                      .background(
                          Brush.horizontalGradient(
                              colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                          ),
                          shape = CircleShape
                      )
              ) {
                  Icon(
                      imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                      contentDescription = "Toggle full screen read layout",
                      tint = Color.White,
                      modifier = Modifier.size(24.dp)
                  )
              }
          }
      }
}

enum class ReadTheme {
    LIGHT, SEPIA, DARK
}

/**
 * Utility function to convert standard Google Drive file URL into direct downloadable stream link
 */
fun convertGoogleDriveLink(url: String): String {
    val trimmed = url.trim()
    return when {
        trimmed.contains("drive.google.com/file/d/") -> {
            val fileId = trimmed.substringAfter("file/d/").substringBefore("/")
            "https://drive.google.com/uc?export=download&id=$fileId"
        }
        trimmed.contains("drive.google.com/open?id=") -> {
            val fileId = trimmed.substringAfter("open?id=").substringBefore("&")
            "https://drive.google.com/uc?export=download&id=$fileId"
        }
        trimmed.contains("id=") && trimmed.contains("drive.google.com") -> {
            val fileId = trimmed.substringAfter("id=").substringBefore("&")
            "https://drive.google.com/uc?export=download&id=$fileId"
        }
        else -> trimmed
    }
}

/**
 * Downloads the PDF to a local cache file (if not cached already) and detects its total page count.
 * This runs fully on a background thread and uses Android's native PdfRenderer.
 */
suspend fun downloadAndGetPdfPageCount(context: Context, fileUrl: String, bookId: String): Int = withContext(Dispatchers.IO) {
    try {
        val booksDir = java.io.File(context.getExternalFilesDir("books") ?: context.filesDir, "")
        val cacheFile = File(context.cacheDir, "book_${bookId}.pdf")
        
        val localSrcFile = if (fileUrl.startsWith("/")) {
            File(fileUrl)
        } else {
            java.io.File(booksDir, "${bookId}.pdf")
        }

        if (localSrcFile.exists() && localSrcFile.length() > 0) {
            Log.d("PDF_Debug", "Found downloaded offline PDF file at: ${localSrcFile.absolutePath}")
            try {
                if (!cacheFile.exists() || cacheFile.length() != localSrcFile.length()) {
                    localSrcFile.copyTo(cacheFile, overwrite = true)
                }
            } catch (copyEx: Exception) {
                Log.e("PDF_Debug", "Error copying offline downloaded file to cache: ${copyEx.message}")
            }
        }

        if (cacheFile.exists() && cacheFile.length() > 0) {
            Log.d("PDF_Debug", "Found cached PDF file at: ${cacheFile.absolutePath}")
        } else {
            if (fileUrl.trim().isEmpty() || fileUrl.contains("placeholder_or_empty")) {
                Log.w("PDF_Debug", "Empty or placeholder fileUrl, skipping page count detection.")
                return@withContext -1
            }
            val directUrl = convertGoogleDriveLink(fileUrl)
            Log.d("PDF_Debug", "Downloading PDF to cache file: ${cacheFile.absolutePath} from resolved URL: $directUrl")
            val url = URL(directUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.inputStream.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("PDF_Debug", "Successfully downloaded PDF. Saved bytes: ${cacheFile.length()}")
        }
        
        Log.d("PDF_Debug", "Opening PDF file for page inspection: ${cacheFile.absolutePath}")
        val pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount
        renderer.close()
        pfd.close()
        
        Log.d("PDF_Debug", "PDF Path: ${cacheFile.absolutePath}")
        Log.d("PDF_Debug", "Total pages from PDF: $pageCount")
        return@withContext pageCount
    } catch (e: Exception) {
        Log.e("PDF_Debug", "Error while analyzing PDF page count using native PdfRenderer: ${e.message}", e)
        return@withContext -1
    }
}

/**
 * Download helper using standard DownloadManager with fallback to standard Chrome action intent
 */
fun downloadPdf(context: Context, url: String, title: String) {
    try {
        val directUrl = convertGoogleDriveLink(url)
        val downloadUri = directUrl.toUri()
        val request = android.app.DownloadManager.Request(downloadUri).apply {
            setAllowedNetworkTypes(
                android.app.DownloadManager.Request.NETWORK_WIFI or 
                android.app.DownloadManager.Request.NETWORK_MOBILE
            )
            setAllowedOverRoaming(false)
            setTitle("Muslims Library - $title")
            setDescription("বইটি ডাউনলোড হচ্ছে...")
            setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_DOWNLOADS, 
                "${title.replace("[^a-zA-Z0-9]".toRegex(), "_")}.pdf"
            )
            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "ডাউনলোড শুরু হয়েছে! আপনার ফোনের Notifications বার চেক করুন।", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        // Safe Direct Browser Intent fallback
        try {
            val directUrl = convertGoogleDriveLink(url)
            val intent = Intent(Intent.ACTION_VIEW, directUrl.toUri())
            context.startActivity(intent)
        } catch (ex: Exception) {
            Toast.makeText(context, "ডাউনলোড করা যাচ্ছে না: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SimulatedBookReadingView(
    title: String,
    themeColor: Color,
    backgroundColor: Color,
    currentPage: Int,
    totalSimulatedPages: Int,
    onPageChanged: (Int) -> Unit
) {
    var swipeOffsetX by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(currentPage) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffsetX < -50f) { // Swipe left, page forward
                            if (currentPage < totalSimulatedPages - 1) {
                                onPageChanged(currentPage + 1)
                            }
                        } else if (swipeOffsetX > 50f) { // Swipe right, page backward
                            if (currentPage > 0) {
                                onPageChanged(currentPage - 1)
                            }
                        }
                        swipeOffsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        swipeOffsetX += dragAmount
                    }
                )
            }
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "অধ্যায় ${currentPage + 1}",
                    fontSize = 12.sp,
                    color = themeColor.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "পড়া শেষ হয়েছে: ${(currentPage + 1) * 100 / totalSimulatedPages}%",
                    fontSize = 12.sp,
                    color = themeColor.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = themeColor.copy(alpha = 0.15f))

            Text(
                text = "بِسْمِ اللَّهِ الرَّهْمَٰনِ الرَّحِيمِ",
                fontSize = 22.sp,
                color = themeColor,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            Text(
                text = getSimulatedReadingText(currentPage),
                fontSize = 16.sp,
                color = themeColor,
                fontFamily = FontFamily.Serif,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Sleek visual tip for swipe navigation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "👈 পৃষ্ঠা উল্টাতে বামে বা ডানে সোয়াইপ করুন 👉",
                fontSize = 11.sp,
                color = themeColor.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getSimulatedReadingText(page: Int): String {
    return when (page) {
        0 -> "জ্ঞানের উৎকর্ষ সাধন এবং দ্বীনের সঠিক বুঝ অর্জন করা প্রত্যেক মুসলিমের জন্য আবশ্যিক কর্তব্য। 'আল্লাহ তায়ালা যার মঙ্গল চান তাকে দ্বীনের প্রগাঢ় অনুধাবন ও ফিকহ দান করেন।' সত্য সন্ধান ও নিয়্যতের বিশুদ্ধতার মাধ্যমেই সফলতার সূচনা হয়। সমস্ত আমল নিয়্যতের ওপর নির্ভরশীল।"
        1 -> "কুরআন অধ্যয়নের গুরুত্ব অপরিসীম। রাসুলুল্লাহ (সাঃ) এর বাণী: 'তোমাদের মধ্যে সেই ব্যক্তিই সর্বোত্তম যে নিজে কুরআন শিক্ষা করে এবং অপরকে তা শিক্ষা দেয়।' কুরআন কেবল পাঠের জন্য নয়, এটি আমাদের দৈনন্দিন জীবনের প্রতিটি কার্যকলাপে বাস্তবায়ন করতে হবে।"
        2 -> "সহীহ আত্মশুদ্ধি এবং রাসূল (সাঃ) এর সুন্নাহ অনুসরণের মাধ্যমেই একজন মুমিন প্রকৃত আধ্যাত্মিক উচ্চতা অর্জন করতে পারে। বিপদে ধৈর্যধারণ, সত্যবাদিতা এবং মানুষের সাথে সদাচরণ করা হলো ঈমানের অপরিহার্য শাখাগুলোর অন্যতম স্তম্ভ।"
        3 -> "ইসলামী আক্বীদা বা বিশ্বাস আমাদের চিন্তার বুনিয়াদ তৈরি করে। আক্বীদা বিশুদ্ধ না হলে কোন আমলই আল্লাহর দরবারে কবুল হওয়ার নিশ্চয়তা নেই। তাই সঠিক জ্ঞান অর্জন করে বিশ্বাসের ত্রুটি ও কুসংস্কার দূর করা আলেমদের অন্যতম প্রধান লক্ষ্য।"
        4 -> "রাসূলুল্লাহ (সাঃ) এর সীরাত বা জীবনী আমাদের জন্য সর্বকালের শ্রেষ্ঠ আদর্শ। তাঁর মক্কী জীবন ছিল ঈমানী দৃঢ়তা এবং ধৈর্যের মহাসাগর এবং মাদানী জীবন ছিল একটি কল্যাণমুখী সমাজ ও রাষ্ট্র গড়ার বাস্তব রূপরেখা।"
        else -> "পূর্বসূরি বুজুর্গ ও জ্ঞানসাধক পণ্ডিতগণ দ্বীনের সত্য পৌঁছাতে যে অমানুষিক পরিশ্রম ও আত্মত্যাগ করেছেন, তা চিরকাল স্বর্ণাক্ষরে লেখা থাকবে। একটি সহীহ হাদিস সংগ্রহের জন্য তারা শত ক্রোশ পথ অতিক্রম করতেন। আমাদের এই পরম সৌভাগ্য যে আজ চোখের সামনে মলাটবদ্ধ কিতাবসমূহ সহজে পড়তে পারছি।"
    }
}

@Composable
fun FallingConfetti(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ConfettiTransition")
    val elapsed by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "elapsed"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val count = 30
        val random = java.util.Random(100L)
        for (i in 0 until count) {
            val startX = random.nextFloat() * size.width
            val speedY = 100f + random.nextFloat() * 200f
            val startY = -40f
            val yOffset = (elapsed * speedY * i) % (size.height + 80f)
            val currentY = startY + yOffset
            val sizeRadius = 5f + random.nextFloat() * 10f
            val color = when (random.nextInt(4)) {
                0 -> primaryColor // Emerald
                1 -> Color(0xFF3B82F6) // Blue
                2 -> Color(0xFFFBBF24) // Gold
                else -> Color(0xFFEF4444) // Pink/Red
            }
            drawCircle(color = color, radius = sizeRadius, center = Offset(startX, currentY))
        }
    }
}

@Composable
fun CelebrationOverlay(
    bookTitle: String,
    readingTimeSpent: Long,
    sessionCount: Int,
    lastReadAt: Long,
    totalPages: Int,
    onResetProgress: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF203241A)), // Rich high-contrast dark green
        contentAlignment = Alignment.Center
    ) {
        FallingConfetti(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed checkmark icon",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "মা-শা-আল্লাহ! 🎉",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "অভিনন্দন! আপনি বইটি সম্পূর্ণ পড়েছেন!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = bookTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📊 আপনার পঠন পরিসংখ্যান (Stats)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("মোট পড়া সময়:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        val displayTime = if (readingTimeSpent < 60) "$readingTimeSpent সেকেন্ড" else "${readingTimeSpent / 60} মিনিট ${readingTimeSpent % 60} সেকেন্ড"
                        Text(displayTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("পড়ার সেশন:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        Text("$sessionCount বার", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("গড় পৃষ্ঠা / সেশন:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        val avgPages = if (sessionCount > 0) (totalPages.toFloat() / sessionCount).toInt() else totalPages
                        Text("$avgPages টি", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("সর্বশেষ পঠিত:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        val lastReadStr = try {
                            val df = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                            df.format(java.util.Date(lastReadAt))
                        } catch (e: Exception) {
                            "আজ"
                        }
                        Text(lastReadStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetProgress,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("আবার পড়ুন", fontSize = 11.sp)
                }

                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ফিরে যান", fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * A custom WebView class to detect scroll position changes and expose scroll values,
 * enabling precise automatic progress tracking under Jetpack Compose.
 */
class ObservableWebView(context: Context) : android.webkit.WebView(context) {
    var onScrollChangedListener: ((scrollX: Int, scrollY: Int, range: Int, height: Int) -> Unit)? = null

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollChangedListener?.invoke(l, t, computeVerticalScrollRange(), height)
    }
}

@Composable
fun NativePdfReader(
    file: File,
    initialPage: Int,
    isZooming: Boolean,
    onZoomStateChanged: (Boolean) -> Unit,
    onPageChanged: (page: Int, total: Int) -> Unit,
    theme: ReadTheme,
    modifier: Modifier = Modifier
) {
    var pdfRenderer by remember(file) { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember(file) { mutableStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(file) {
        Log.d("PDF_Debug", "Initializing PdfRenderer for local file: ${file.absolutePath}")
        var pfd: ParcelFileDescriptor? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            pdfRenderer = renderer
            pageCount = renderer.pageCount
            Log.d("PDF_Debug", "PdfRenderer successfully initialized. Pages: $pageCount")
        } catch (e: Exception) {
            Log.e("PDF_Debug", "Error opening local PDF file: ${e.message}", e)
            loadError = e.message
        }

        onDispose {
            Log.d("PDF_Debug", "Disposing PdfRenderer")
            try {
                pdfRenderer?.close()
                pfd?.close()
            } catch (e: Exception) {
                Log.e("PDF_Debug", "Error disposing PdfRenderer resources: ${e.message}", e)
            }
        }
    }

    if (loadError != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PDF লোড করতে সমস্যা হচ্ছে: $loadError\nঅনুগ্রহ করে আবার চেষ্টা করুন।",
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        }
    } else if (pdfRenderer != null && pageCount > 0) {
        val lazyListState = rememberLazyListState()

        LaunchedEffect(initialPage, pageCount) {
            if (initialPage in 1..pageCount) {
                Log.d("PDF_Debug", "Jumping to saved page: $initialPage")
                lazyListState.scrollToItem(initialPage - 1)
            }
        }

        val firstVisibleItemIndex by remember {
            derivedStateOf { lazyListState.firstVisibleItemIndex }
        }

        LaunchedEffect(firstVisibleItemIndex) {
            if (!isZooming && firstVisibleItemIndex >= 0) {
                val newPage = firstVisibleItemIndex + 1
                Log.d("PDF_Debug", "Scroll detected. Page updated to: $newPage / $pageCount")
                onPageChanged(newPage, pageCount)
            } else {
                Log.d("PDF_Debug", "Scroll detected but zoom is in progress, ignoring page change event.")
            }
        }

        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val originalScale = scale
                        val targetScale = (scale * zoom).coerceIn(1f, 4f)
                        
                        if (targetScale != scale) {
                            scale = targetScale
                            val nowZooming = scale > 1.05f
                            if (nowZooming != isZooming) {
                                Log.d("PDF_Debug", "Zoom gesture state changed: $nowZooming (Scale: $scale)")
                                onZoomStateChanged(nowZooming)
                            }
                        }
                        
                        if (scale > 1f) {
                            offset = offset + pan
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            ) {
                items(pageCount) { index ->
                    PdfPageItem(
                        pdfRenderer = pdfRenderer!!,
                        pageIndex = index,
                        theme = theme,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun PdfPageItem(
    pdfRenderer: PdfRenderer,
    pageIndex: Int,
    theme: ReadTheme,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                synchronized(pdfRenderer) {
                    val page = pdfRenderer.openPage(pageIndex)
                    val scaleFactor = 2.0f
                    val targetWidth = (page.width * scaleFactor).toInt()
                    val targetHeight = (page.height * scaleFactor).toInt()
                    
                    val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmap = bmp
                }
            } catch (e: Exception) {
                Log.e("PdfPageItem", "Failed to render page $pageIndex", e)
            }
        }
    }

    if (bitmap != null) {
        val colorFilter = when (theme) {
            ReadTheme.DARK -> {
                // Invert RGB whilst keeping Alpha intact
                androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                    androidx.compose.ui.graphics.ColorMatrix(
                        floatArrayOf(
                            -1.0f,  0.0f,  0.0f,  0.0f, 255.0f,
                             0.0f, -1.0f,  0.0f,  0.0f, 255.0f,
                             0.0f,  0.0f, -1.0f,  0.0f, 255.0f,
                             0.0f,  0.0f,  0.0f,  1.0f,   0.0f
                        )
                    )
                )
            }
            ReadTheme.SEPIA -> {
                // Sepia tones filter overlay
                androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                    androidx.compose.ui.graphics.ColorMatrix(
                        floatArrayOf(
                            0.95f, 0.05f, 0.0f, 0.0f, 0.0f,
                            0.0f,  0.90f, 0.0f, 0.0f, 0.0f,
                            0.0f,  0.0f,  0.80f, 0.0f, 0.0f,
                            0.0f,  0.0f,  0.0f,  1.0f, 0.0f
                        )
                    )
                )
            }
            ReadTheme.LIGHT -> null
        }

        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Page ${pageIndex + 1}",
            colorFilter = colorFilter,
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(2.dp, shape = RoundedCornerShape(4.dp))
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.414f)
                .background(
                    when (theme) {
                        ReadTheme.LIGHT -> Color.White
                        ReadTheme.SEPIA -> Color(0xFFFBF4E4)
                        ReadTheme.DARK -> Color(0xFF161C1A)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

