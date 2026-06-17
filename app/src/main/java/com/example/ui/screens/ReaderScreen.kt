package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import java.net.URLDecoder

enum class ReadTheme {
    LIGHT, SEPIA, DARK
}

/**
 * Utility function to convert standard Google Drive file URL into direct downloable stream link
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
 * Download helper using standard DownloadManager with fallback to standard Chrome action intent
 */
fun downloadPdf(context: Context, url: String, title: String) {
    try {
        val directUrl = convertGoogleDriveLink(url)
        val downloadUri = Uri.parse(directUrl)
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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(directUrl))
            context.startActivity(intent)
        } catch (ex: Exception) {
            Toast.makeText(context, "ডাউনলোড করা যাচ্ছে না: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    bookTitle: String,
    fileUrl: String = "",
    fileType: String = "pdf",
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
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

    var readTheme by remember { mutableStateOf(ReadTheme.LIGHT) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isWebLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }

    // Color theme setups
    val isDark = readTheme == ReadTheme.DARK
    val backgroundColor = when (readTheme) {
        ReadTheme.LIGHT -> Color(0xFFFCFDF9)
        ReadTheme.SEPIA -> Color(0xFFFBF4E4)
        ReadTheme.DARK -> Color(0xFF161C1A)
    }
    val contentColor = when (readTheme) {
        ReadTheme.LIGHT -> Color(0xFF043B2B)
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
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(directUrl))
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
                    backgroundColor = backgroundColor
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ডাউনলোড", fontSize = 12.sp)
                                }
                                
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(convertGoogleDriveLink(decodedFileUrl)))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "ড্রাইভ খোলা যাচ্ছে না", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B)),
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
                // PDF URL WEBVIEW EMBED
                val directDocUrl = convertGoogleDriveLink(decodedFileUrl)
                val docsViewerUrl = "https://docs.google.com/viewer?url=${Uri.encode(directDocUrl)}&embedded=true"

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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("আবার চেষ্টা করুন")
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(directDocUrl))
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
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
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
                                                errorCode: Int,
                                                description: String?,
                                                failingUrl: String?
                                            ) {
                                                super.onReceivedError(view, errorCode, description, failingUrl)
                                                hasError = true
                                            }
                                        }
                                        webChromeClient = WebChromeClient()
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
                                CircularProgressIndicator(color = Color(0xFF10B981))
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
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(directDocUrl))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
                                ) {
                                    Text("লোড না হলে ড্রাইভে দেখুন (External Web View)")
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
                            colors = listOf(Color(0xFF10B981), Color(0xFF043B2B))
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

@Composable
fun SimulatedBookReadingView(
    title: String,
    themeColor: Color,
    backgroundColor: Color
) {
    var currentPage by remember { mutableStateOf(0) }
    val totalSimulatedPages = 6

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                fontSize = 22.sp,
                color = themeColor,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
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

        // Stepper Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (currentPage > 0) currentPage-- },
                enabled = currentPage > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColor.copy(alpha = 0.1f),
                    contentColor = themeColor
                )
            ) {
                Text("পূর্ববর্তী")
            }

            Text(
                text = "পৃষ্ঠা ${currentPage + 1} / $totalSimulatedPages",
                fontSize = 14.sp,
                color = themeColor,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = { if (currentPage < totalSimulatedPages - 1) currentPage++ },
                enabled = currentPage < totalSimulatedPages - 1,
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColor.copy(alpha = 0.1f),
                    contentColor = themeColor
                )
            ) {
                Text("পরবর্তী")
            }
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
