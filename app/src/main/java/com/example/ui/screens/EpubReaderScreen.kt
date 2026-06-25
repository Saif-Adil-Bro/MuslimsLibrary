package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.EpubReaderViewModel
import java.io.ByteArrayInputStream
import java.net.URLDecoder

enum class EpubTheme {
    LIGHT, DARK, SEPIA
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubReaderScreen(
    bookId: String,
    bookTitle: String,
    fileUrl: String,
    fileType: String,
    userId: String,
    onBackClick: () -> Unit,
    viewModel: EpubReaderViewModel = viewModel()
) {
    val context = LocalContext.current
    val book by viewModel.book.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentChapterIndex by viewModel.currentChapterIndex.collectAsState()
    val scrollOffset by viewModel.scrollOffset.collectAsState()

    val decodedTitle = remember(bookTitle) {
        try { URLDecoder.decode(bookTitle, "UTF-8") } catch (e: Exception) { bookTitle }
    }

    // Settings State
    val prefs = context.getSharedPreferences("EpubPrefs_$userId", Context.MODE_PRIVATE)
    var readTheme by remember { 
        mutableStateOf(EpubTheme.values()[prefs.getInt("theme", EpubTheme.LIGHT.ordinal)]) 
    }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("fontSize", 18f)) }
    
    var isFullScreen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showToc by remember { mutableStateOf(false) }

    LaunchedEffect(fileUrl) {
        viewModel.loadBook(context, bookId, fileUrl)
    }

    DisposableEffect(fontSize, readTheme) {
        onDispose {
            prefs.edit()
                .putFloat("fontSize", fontSize)
                .putInt("theme", readTheme.ordinal)
                .apply()
        }
    }

    val backgroundColor = when (readTheme) {
        EpubTheme.LIGHT -> MaterialTheme.colorScheme.background
        EpubTheme.DARK -> MaterialTheme.colorScheme.onBackground
        EpubTheme.SEPIA -> Color(0xFFFBF0D9)
    }
    
    val textColor = when (readTheme) {
        EpubTheme.LIGHT -> MaterialTheme.colorScheme.onSurface
        EpubTheme.DARK -> MaterialTheme.colorScheme.background
        EpubTheme.SEPIA -> Color(0xFF432A15)
    }
    
    fun Color.toHex(): String {
        val argb = this.toArgb()
        return String.format("#%06X", 0xFFFFFF and argb)
    }
    
    val bgHex = backgroundColor.toHex()
    val textHex = textColor.toHex()

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = !isFullScreen, enter = slideInVertically(), exit = slideOutVertically()) {
                TopAppBar(
                    title = { 
                        Column {
                            Text(decodedTitle, maxLines = 1, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            val totalChapters = book?.spine?.spineReferences?.size ?: 1
                            val currentProg = if (totalChapters > 0) ((currentChapterIndex + scrollOffset) / totalChapters * 100).toInt() else 0
                            Text("Progress: $currentProg%", fontSize = 12.sp, color = textColor.copy(alpha = 0.7f))
                        }
                    },
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
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        val totalChapters = book?.spine?.spineReferences?.size ?: 1
                        val progressFloat = if (totalChapters > 0) (currentChapterIndex + scrollOffset) / totalChapters else 0f
                        LinearProgressIndicator(
                            progress = { progressFloat },
                            modifier = Modifier.fillMaxWidth().height(4.dp).padding(bottom = 8.dp),
                            color = Color(0xFF667EEA),
                            trackColor = textColor.copy(alpha = 0.2f),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (currentChapterIndex > 0) viewModel.updateProgress(context, bookId, currentChapterIndex - 1, 0f) },
                                enabled = currentChapterIndex > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA))
                            ) { Text("আগের অধ্যায়") }
                            
                            Text("${currentChapterIndex + 1} / $totalChapters", fontSize = 14.sp)
                            
                            Button(
                                onClick = { if (currentChapterIndex < totalChapters - 1) viewModel.updateProgress(context, bookId, currentChapterIndex + 1, 0f) },
                                enabled = currentChapterIndex < totalChapters - 1,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA))
                            ) { Text("পরের অধ্যায়") }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
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
                    Text("ত্রুটি:", color = Color.Red, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = textColor, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.loadBook(context, bookId, fileUrl) }) {
                        Text("পুনরায় চেষ্টা করুন")
                    }
                }
            } else {
                book?.let { loadedBook ->
                    val spineRefs = loadedBook.spine.spineReferences
                    if (currentChapterIndex in spineRefs.indices) {
                        val resource = spineRefs[currentChapterIndex].resource
                        val rawHtml = try { String(resource.data, Charsets.UTF_8) } catch (e: Exception) { "" }
                        
                        val injectedCss = """
                            <style>
                              body {
                                background-color: $bgHex !important;
                                color: $textHex !important;
                                font-size: ${fontSize}px !important;
                                font-family: serif !important;
                                line-height: 1.8 !important;
                                padding: 10px 20px !important;
                                word-wrap: break-word !important;
                                text-align: justify !important;
                              }
                              img {
                                max-width: 100% !important;
                                height: auto !important;
                                display: block !important;
                                margin: 10px auto !important;
                                border-radius: 8px;
                              }
                              ::selection {
                                background: #667EEA;
                                color: white;
                              }
                            </style>
                            <script>
                              function reportScroll() {
                                  var h = document.documentElement;
                                  var b = document.body;
                                  var st = 'scrollTop';
                                  var sh = 'scrollHeight';
                                  var maxScroll = ((h[sh]||b[sh]) - h.clientHeight);
                                  var scrollPos = maxScroll > 0 ? (h[st]||b[st]) / maxScroll : 0.0;
                                  if(window.Android) { window.Android.onScroll(scrollPos); }
                              }
                              window.addEventListener('scroll', reportScroll);
                              window.addEventListener('touchend', function() { setTimeout(reportScroll, 300); });
                              
                              function restoreScroll(percentage) {
                                  setTimeout(function() {
                                      var h = document.documentElement;
                                      var b = document.body;
                                      var sh = 'scrollHeight';
                                      var targetScroll = percentage * ((h[sh]||b[sh]) - h.clientHeight);
                                      window.scrollTo(0, targetScroll);
                                  }, 200);
                              }
                            </script>
                        """.trimIndent()
                        
                        val finalHtml = if (rawHtml.contains("</head>")) {
                            rawHtml.replace("</head>", "$injectedCss</head>")
                        } else if (rawHtml.contains("<body>")) {
                            rawHtml.replace("<body>", "<body>$injectedCss")
                        } else {
                            "$injectedCss$rawHtml"
                        }
                        
                        val baseUrl = "http://localhost/" + resource.href

                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    
                                    addJavascriptInterface(object {
                                        @JavascriptInterface
                                        fun onScroll(percentage: Float) {
                                            viewModel.updateProgress(context, bookId, currentChapterIndex, percentage)
                                        }
                                        
                                        @JavascriptInterface
                                        fun toggleFullScreen() {
                                            isFullScreen = !isFullScreen
                                        }
                                    }, "Android")
                                }
                            },
                            update = { webView ->
                                val currentHtmlHash = finalHtml.hashCode()
                                if (webView.tag != currentHtmlHash) {
                                    webView.tag = currentHtmlHash
                                    
                                    webView.webViewClient = object : WebViewClient() {
                                        override fun shouldInterceptRequest(
                                            view: WebView,
                                            request: WebResourceRequest
                                        ): WebResourceResponse? {
                                            val urlStr = request.url.toString()
                                            if (urlStr.startsWith("http://localhost/")) {
                                                val path = request.url.path?.removePrefix("/") ?: return null
                                                val res = loadedBook.resources.getByHref(path) 
                                                    ?: loadedBook.resources.all.find { it.href.endsWith(path) }
                                                
                                                if (res != null) {
                                                    val mime = res.mediaType?.name ?: "image/jpeg"
                                                    return WebResourceResponse(
                                                        mime,
                                                        "UTF-8",
                                                        ByteArrayInputStream(res.data)
                                                    )
                                                }
                                            }
                                            return super.shouldInterceptRequest(view, request)
                                        }
                                        
                                        override fun onPageFinished(view: WebView, url: String?) {
                                            super.onPageFinished(view, url)
                                            view.evaluateJavascript("restoreScroll($scrollOffset);", null)
                                            view.evaluateJavascript("""
                                                document.body.addEventListener('click', function(e) {
                                                    if(e.target.tagName !== 'A' && window.getSelection().toString().length === 0) {
                                                        window.Android.toggleFullScreen();
                                                    }
                                                });
                                            """.trimIndent(), null)
                                        }
                                    }
                                    
                                    webView.loadDataWithBaseURL(baseUrl, finalHtml, "text/html", "UTF-8", null)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("ফন্ট সাইজ (${fontSize.toInt()})", fontWeight = FontWeight.Bold)
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 12f..36f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF667EEA), activeTrackColor = Color(0xFF667EEA))
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("থিম পরিবর্তন", fontWeight = FontWeight.Bold)
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
                    Text("সূচিপত্র", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                itemsIndexed(book?.spine?.spineReferences ?: emptyList()) { index, _ ->
                    val isCurrent = index == currentChapterIndex
                    Text(
                        text = "অধ্যায় ${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateProgress(context, bookId, index, 0f)
                                showToc = false
                            }
                            .padding(vertical = 12.dp),
                        fontSize = 16.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) Color(0xFF667EEA) else MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
