package com.example.ui.screens

import android.content.ContentResolver
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.ui.viewmodel.UploadState
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.SourceType
import com.example.ui.viewmodel.BookUploadData
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    adminViewModel: AdminViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val uiState by adminViewModel.uiState.collectAsState()

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("হাদিস") }
    var fileType by remember { mutableStateOf("pdf") } // "pdf" or "epub"

    var coverSourceType by remember { mutableStateOf(SourceType.FILE) }
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var coverImageUrl by remember { mutableStateOf("") }

    var bookSourceType by remember { mutableStateOf(SourceType.FILE) }
    var bookFileUri by remember { mutableStateOf<Uri?>(null) }
    var bookFileUrl by remember { mutableStateOf("") }

    var expandedDropdown by remember { mutableStateOf(false) }
    val categories = listOf("কুরআন", "হাদিস", "ফিকহ", "তাফসীর", "সীরাত", "অন্যান্য")

    // Image Picker Setup (GetContent)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        coverImageUri = uri
    }

    // Document Picker Setup (GetContent)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        bookFileUri = uri
    }

    // Listen to outcome states
    LaunchedEffect(uiState) {
        if (uiState is UploadState.Success) {
            val isCoverProvided = if (coverSourceType == SourceType.FILE) {
                coverImageUri != null
            } else {
                coverImageUrl.isNotBlank()
            }
            val message = if (isCoverProvided) {
                "বইটি কভার ইমেজসহ সফলভাবে যুক্ত করা হয়েছে!"
            } else {
                "বইটি কভার ইমেজ ছাড়াই সফলভাবে যুক্ত করা হয়েছে! (ডিফল্ট কভার ব্যবহার করা হবে)"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            adminViewModel.resetState()
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Islamic Manuscript",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF043B2B)
                )
            )
        },
        containerColor = Color(0xFFFCFDF9)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Error banner if any
            if (uiState is UploadState.Error) {
                Surface(
                    color = Color(0xFFFDE8E8),
                    border = BorderStroke(1.dp, Color(0xFFF8B4B4)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.Red)
                        Text(
                            text = (uiState as UploadState.Error).message,
                            color = Color(0xFF9B1C1C),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Metadata Card Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Manuscript Identity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF043B2B),
                        fontFamily = FontFamily.Serif
                    )

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Book Title") },
                        leadingIcon = { Icon(Icons.Default.Book, contentDescription = null, tint = Color(0xFF0A4E38)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_book_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF0A4E38),
                            focusedLabelColor = Color(0xFF0A4E38),
                            unfocusedLabelColor = Color(0xFF475569)
                        )
                    )

                    // Author
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("Author / Compiler (Alim)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0A4E38)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_book_author_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF0A4E38),
                            focusedLabelColor = Color(0xFF0A4E38),
                            unfocusedLabelColor = Color(0xFF475569)
                        )
                    )

                    // Category Selector Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF0A4E38)) },
                            trailingIcon = {
                                IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                                    Icon(
                                        imageVector = if (expandedDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedDropdown = !expandedDropdown }
                                .testTag("add_book_category_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B),
                                focusedBorderColor = Color(0xFF0A4E38),
                                focusedLabelColor = Color(0xFF0A4E38),
                                unfocusedLabelColor = Color(0xFF475569)
                            )
                        )

                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // File Type Selector Radio Section
                    Column {
                        Text(
                            text = "Format",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = fileType == "pdf",
                                    onClick = { fileType = "pdf" },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0A4E38))
                                )
                                Text("PDF Document", fontSize = 14.sp, color = Color.DarkGray)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = fileType == "epub",
                                    onClick = { fileType = "epub" },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0A4E38))
                                )
                                Text("EPUB E-Book", fontSize = 14.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }

            // Cover Image Selection Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cover Image Selection (ঐচ্ছিক)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF043B2B),
                            fontFamily = FontFamily.Serif
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(Optional)",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Cover source type selector (Upload cover vs Web URL)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { coverSourceType = SourceType.FILE },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (coverSourceType == SourceType.FILE) Color(0xFF043B2B) else Color(0xFFF1F4F2),
                                contentColor = if (coverSourceType == SourceType.FILE) Color.White else Color(0xFF475569)
                            ),
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Device", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { coverSourceType = SourceType.URL },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (coverSourceType == SourceType.URL) Color(0xFF043B2B) else Color(0xFFF1F4F2),
                                contentColor = if (coverSourceType == SourceType.URL) Color.White else Color(0xFF475569)
                            ),
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use URL", fontSize = 12.sp)
                        }
                    }

                    if (coverSourceType == SourceType.FILE) {
                        if (coverImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp, 190.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFE6EAE7), RoundedCornerShape(8.dp))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(coverImageUri),
                                    contentDescription = "Cover preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { coverImageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F4F2))
                                    .clickable { imagePickerLauncher.launch("image/*") }
                                    .border(1.dp, Color(0xFFDCE2DE), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = com.example.R.drawable.ic_book_placeholder,
                                    contentDescription = "Default cover placeholder",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(70.dp, 95.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ডিফল্ট কভার সেট করা আছে",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF043B2B)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "কাস্টম কভার ইমেজ আপলোড করতে ট্যাপ করুন",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = Color(0xFF0A4E38),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    } else {
                        // Use Cover URL
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = coverImageUrl,
                                onValueChange = { coverImageUrl = it },
                                label = { Text("Cover Image URL") },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF0A4E38)) },
                                placeholder = { Text("https://example.com/cover.jpg") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1E293B),
                                    unfocusedTextColor = Color(0xFF1E293B),
                                    focusedBorderColor = Color(0xFF0A4E38),
                                    focusedLabelColor = Color(0xFF0A4E38)
                                )
                            )

                            // Google Drive link detection
                            if (adminViewModel.supabaseService.isGoogleDriveLink(coverImageUrl)) {
                                Button(
                                    onClick = {
                                        coverImageUrl = adminViewModel.supabaseService.convertGoogleDriveLink(coverImageUrl)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E38)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Convert GDrive Cover URL", fontSize = 11.sp)
                                }
                            }

                            // URL verification inline warns
                            val isCoverUrlValid = adminViewModel.supabaseService.isValidImageUrl(coverImageUrl)
                            if (coverImageUrl.isNotBlank() && !isCoverUrlValid) {
                                Text(
                                    text = "Invalid cover image URL (must start with http:// or https://)",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (coverImageUrl.isNotBlank() && isCoverUrlValid) {
                                Text(
                                    text = "Valid cover URL detected",
                                    color = Color(0xFF0A4E38),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Preview Cover Illustration:",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(140.dp, 190.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .align(Alignment.CenterHorizontally)
                                        .border(1.dp, Color(0xFFE6EAE7), RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = coverImageUrl,
                                        contentDescription = "Cover preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF1F4F2))
                                        .border(1.dp, Color(0xFFDCE2DE), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = com.example.R.drawable.ic_book_placeholder,
                                        contentDescription = "Default cover placeholder",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(70.dp, 95.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ডিফল্ট কভার সেট করা আছে",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF043B2B)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "উপরে কভার ইমেজের লিংক দিলে এখানে কাস্টম কভারের প্রিভিউ দেখাবে।",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Book File Selector Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    var showSourceInfo by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Manuscript Document File",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF043B2B),
                                fontFamily = FontFamily.Serif
                            )
                            IconButton(onClick = { showSourceInfo = !showSourceInfo }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Info, contentDescription = "Source info", tint = Color(0xFF0A4E38), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (showSourceInfo) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF5EF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Source Types Helpful Guide", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF032B1D))
                                Text("• Device Upload: Directly upload PDF or EPUB to secure database storage.", fontSize = 12.sp, color = Color(0xFF0A4E38))
                                Text("• Google Drive: Paste anyone-can-view sharing link. The app will automatically convert it to direct CDN stream.", fontSize = 12.sp, color = Color(0xFF0A4E38))
                                Text("• Direct URL (CDN): Save workspace storage by pointing directly to external links (e.g. archive.org).", fontSize = 12.sp, color = Color(0xFF0A4E38))
                            }
                        }
                    }

                    // Book source selection buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            SourceType.FILE to "Upload",
                            SourceType.GDRIVE to "Google Drive",
                            SourceType.CDN to "CDN URL"
                        ).forEach { (type, label) ->
                            val isSelected = bookSourceType == type
                            val icon = when (type) {
                                SourceType.FILE -> Icons.Default.CloudUpload
                                SourceType.GDRIVE -> Icons.Default.AddToDrive
                                else -> Icons.Default.Link
                            }
                            Button(
                                onClick = { bookSourceType = type },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF043B2B) else Color(0xFFF1F4F2),
                                    contentColor = if (isSelected) Color.White else Color(0xFF475569)
                                ),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(19.dp)
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(label, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }

                    if (bookSourceType == SourceType.FILE) {
                        if (bookFileUri != null) {
                            val fileName = getFileName(contentResolver, bookFileUri)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEAF5EF))
                                    .border(1.dp, Color(0xFFB1DFCA), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "PdfIcon",
                                        tint = Color(0xFF0A4E38)
                                    )
                                    Column {
                                        Text(
                                            text = fileName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF032B1D)
                                        )
                                        Text(
                                            text = fileType.uppercase(),
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                IconButton(onClick = { bookFileUri = null }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove File",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F4F2))
                                    .clickable {
                                        val mime = if (fileType == "pdf") "application/pdf" else "application/epub+zip"
                                        documentPickerLauncher.launch(mime)
                                    }
                                    .border(1.dp, Color(0xFFDCE2DE), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = Color(0xFF0A4E38),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Select code or ${fileType.uppercase()} file",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else if (bookSourceType == SourceType.GDRIVE) {
                        // Google Drive section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ Make sure file sharing is enabled (Anyone with link can view)",
                                color = Color(0xFFB45309),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = bookFileUrl,
                                    onValueChange = { input ->
                                        bookFileUrl = input
                                        // Auto-convert Google Drive link if it matches
                                        if (adminViewModel.supabaseService.isGoogleDriveLink(input)) {
                                            bookFileUrl = adminViewModel.supabaseService.convertGoogleDriveLink(input)
                                        }
                                    },
                                    label = { Text("Google Drive Sharing URL") },
                                    placeholder = { Text("https://drive.google.com/file/d/FILE_ID/view?usp=sharing") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFF1E293B),
                                        unfocusedTextColor = Color(0xFF1E293B),
                                        focusedBorderColor = Color(0xFF0A4E38),
                                        focusedLabelColor = Color(0xFF0A4E38)
                                    )
                                )

                                Button(
                                    onClick = {
                                        bookFileUrl = adminViewModel.supabaseService.convertGoogleDriveLink(bookFileUrl)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B)),
                                    modifier = Modifier.height(56.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Convert", fontSize = 12.sp)
                                }
                            }

                            // Validation and Link Preview info
                            val isGDriveValid = adminViewModel.supabaseService.isGoogleDriveLink(bookFileUrl) || bookFileUrl.startsWith("https://drive.google.com/uc")
                            if (bookFileUrl.isNotBlank() && !isGDriveValid) {
                                Text(
                                    text = "Invalid GDrive Address format.",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (bookFileUrl.isNotBlank()) {
                                Text(
                                    text = "Google Drive link verified.",
                                    color = Color(0xFF0A4E38),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Direct file link: $bookFileUrl",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "File Size: Unknown (Remote CDN)",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        if (bookFileUrl.isNotBlank()) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bookFileUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0A4E38)),
                                    border = BorderStroke(1.dp, Color(0xFF0A4E38)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Link", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        // CDN URL section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ URL must be a direct download link (ends with .pdf or .epub)",
                                color = Color(0xFFB45309),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            OutlinedTextField(
                                value = bookFileUrl,
                                onValueChange = { bookFileUrl = it },
                                label = { Text("Direct CDN URL") },
                                placeholder = { Text("https://example.com/books/manual.$fileType") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1E293B),
                                    unfocusedTextColor = Color(0xFF1E293B),
                                    focusedBorderColor = Color(0xFF0A4E38),
                                    focusedLabelColor = Color(0xFF0A4E38)
                                )
                            )

                            // Format Validation
                            val isCdnValid = if (fileType == "pdf") {
                                adminViewModel.supabaseService.isValidPdfUrl(bookFileUrl)
                            } else {
                                adminViewModel.supabaseService.isValidEpubUrl(bookFileUrl)
                            }

                            if (bookFileUrl.isNotBlank() && !isCdnValid) {
                                Text(
                                    text = "Error: Link format is inoperable. Must end with .$fileType (query parameters stripped)",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (bookFileUrl.isNotBlank()) {
                                Text(
                                    text = "CDN link verified.",
                                    color = Color(0xFF0A4E38),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "File Size: Unknown (Remote CDN)",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        if (bookFileUrl.isNotBlank()) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bookFileUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0A4E38)),
                                    border = BorderStroke(1.dp, Color(0xFF0A4E38)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Link", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Submit / Action upload indicator states
            when (val state = uiState) {
                is UploadState.Uploading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            color = Color(0xFF0A4E38),
                            trackColor = Color(0xFFE6EAE7),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Uploading... ${state.progress}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A4E38)
                        )
                    }
                }
                else -> {
                    val hasCover = if (coverSourceType == SourceType.FILE) {
                        true // Optional
                    } else {
                        coverImageUrl.isBlank() || adminViewModel.supabaseService.isValidImageUrl(coverImageUrl)
                    }

                    val hasBook = when (bookSourceType) {
                        SourceType.FILE -> bookFileUri != null
                        SourceType.GDRIVE -> bookFileUrl.isNotBlank() && (adminViewModel.supabaseService.isGoogleDriveLink(bookFileUrl) || bookFileUrl.startsWith("https://drive.google.com/uc"))
                        SourceType.CDN -> {
                            bookFileUrl.isNotBlank() && if (fileType == "pdf") {
                                adminViewModel.supabaseService.isValidPdfUrl(bookFileUrl)
                            } else {
                                adminViewModel.supabaseService.isValidEpubUrl(bookFileUrl)
                            }
                        }
                        else -> false
                    }

                    val isFormValid = title.isNotBlank() && author.isNotBlank() && hasCover && hasBook

                    Button(
                        onClick = {
                            val uploadData = BookUploadData(
                                title = title,
                                author = author,
                                category = category,
                                fileType = fileType,
                                coverSourceType = coverSourceType,
                                coverImageUri = if (coverSourceType == SourceType.FILE) coverImageUri else null,
                                coverImageUrl = if (coverSourceType == SourceType.URL) coverImageUrl else null,
                                bookSourceType = bookSourceType,
                                bookFileUri = if (bookSourceType == SourceType.FILE) bookFileUri else null,
                                bookFileUrl = if (bookSourceType != SourceType.FILE) bookFileUrl else null
                            )
                            adminViewModel.uploadBook(uploadData)
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0A4E38),
                            disabledContainerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_upload_book_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Publish Manuscript",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun getFileName(contentResolver: ContentResolver, uri: Uri?): String {
    if (uri == null) return "No file selected"
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    result = cursor.getString(nameIndex)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Selected File"
}
