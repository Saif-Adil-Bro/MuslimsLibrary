package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ContentSanitizer
import com.example.ui.viewmodel.ForumViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun validatePost(title: String, content: String, category: String): String? {
    if (title.length < 3) return "শিরোনামটি কমপক্ষে ৩ অক্ষরের হতে হবে।"
    if (title.length > 200) return "শিরোনামটি ২০০ অক্ষরের বেশি হতে পারবে না।"
    if (content.length < 10) return "মূল বিষয়বস্তু কমপক্ষে ১০ অক্ষরের হতে হবে।"
    if (content.length > 5000) return "মূল বিষয়বস্তু ৫০০০ অক্ষরের বেশি হতে পারবে না।"
    if (category.isBlank()) return "দয়া করে একটি ক্যাটাগরি সিলেক্ট করুন।"
    
    val bannedWords = listOf("spam", "scam", "fake", "অশ্লীল", "খারাপ")
    val lowerTitle = title.lowercase()
    val lowerContent = content.lowercase()
    
    if (bannedWords.any { lowerTitle.contains(it) }) {
        return "শিরোনামে উপযুক্ত শব্দ ব্যবহার করুন (নিষিদ্ধ শব্দ পাওয়া গেছে)।"
    }
    if (bannedWords.any { lowerContent.contains(it) }) {
        return "পোস্টে উপযুক্ত শব্দ ব্যবহার করুন (নিষিদ্ধ শব্দ পাওয়া গেছে)।"
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    forumViewModel: ForumViewModel,
    userEmail: String,
    userRole: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("General") }
    var content by remember { mutableStateOf("") }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    val isPublishing = remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load available post options (exclude "All" from creation)
    val creationCategories = forumViewModel.categories.filter { it != "All" }
    
    val categoryMappings = mapOf(
        "General" to "সাধারণ আলোচনা",
        "Quran" to "আল-কুরআন",
        "Hadith" to "আল-হাদিস",
        "Fiqh" to "ইসলামিক ফিকহ",
        "Sira" to "সীরাতুন্নবী",
        "Others" to "অন্যান্য"
    )

    LaunchedEffect(Unit) {
        forumViewModel.errorMessage.collectLatest { error ->
            isPublishing.value = false
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "নতুন পোস্ট লিখুন",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF043B2B)
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFCFDF9))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title field
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "পোস্টের শিরোনাম *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF043B2B)
                    )
                    Text(
                        text = "${title.length}/200",
                        fontSize = 11.sp,
                        color = if (title.length > 200) Color.Red else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 250) title = it },
                    placeholder = { Text("একটি সংক্ষিপ্ত চমৎকার শিরোনাম দিন", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0A4E38),
                        focusedLabelColor = Color(0xFF0A4E38)
                    ),
                    singleLine = true
                )
            }

            // Category selector dropdown
            Column {
                Text(
                    text = "ক্যাটাগরি নির্ধারণ করুন *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF043B2B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFC0C7C3), RoundedCornerShape(12.dp))
                            .clickable { isCategoryDropdownExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = categoryMappings[selectedCategory] ?: selectedCategory,
                            fontSize = 14.sp,
                            color = Color(0xFF032B1D)
                        )
                        Icon(
                            imageVector = if (isCategoryDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF043B2B)
                        )
                    }

                    DropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(Color.White)
                    ) {
                        creationCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = categoryMappings[category] ?: category,
                                        fontSize = 14.sp,
                                        color = Color(0xFF032B1D)
                                    )
                                },
                                onClick = {
                                    selectedCategory = category
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Content body multi-line field
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "মূল বক্তব্য বা প্রশ্ন লিখুন *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF043B2B)
                    )
                    Text(
                        text = "${content.length}/5000",
                        fontSize = 11.sp,
                        color = if (content.length > 5000) Color.Red else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 6000) content = it },
                    placeholder = {
                        Text(
                            text = "আপনার চিন্তা, প্রশ্ন বা কোনো বইয়ের চমৎকার রিভিও এখানে বিস্তারিত লিখুন...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0A4E38)
                    ),
                    maxLines = 15
                )
            }

            // Warn if suspicious links are detected automatically
            if (ContentSanitizer.containsSuspiciousLinks(content)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Red.copy(alpha = 0.05f))
                        .border(1.dp, Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "সতর্কতা: আপনার পোস্টে ৩টির বেশি লিংক রয়েছে। এটি স্প্যাম হিসেবে ফ্ল্যাগ করা হতে পারে!",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(16.dp))

            // Publish submit button
            val isFormValid = title.isNotBlank() && content.isNotBlank()
            Button(
                onClick = {
                    if (isFormValid) {
                        val sanitizedTitle = ContentSanitizer.sanitize(title.trim())
                        val sanitizedContent = ContentSanitizer.sanitize(content.trim())
                        
                        val validationError = validatePost(sanitizedTitle, sanitizedContent, selectedCategory)
                        if (validationError != null) {
                            scope.launch {
                                snackbarHostState.showSnackbar(validationError)
                            }
                            return@Button
                        }

                        isPublishing.value = true
                        forumViewModel.createPost(
                            userId = userEmail,
                            email = userEmail,
                            title = sanitizedTitle,
                            content = sanitizedContent,
                            category = selectedCategory,
                            role = userRole,
                            onSuccess = {
                                Toast.makeText(context, "পোস্টটি সফলভাবে প্রকাশিত হয়েছে!", Toast.LENGTH_LONG).show()
                                isPublishing.value = false
                                onBackClick()
                            }
                        )
                    }
                },
                enabled = isFormValid && !isPublishing.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0A4E38),
                    disabledContainerColor = Color(0xFFC0C7C3)
                )
            ) {
                if (isPublishing.value) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("পোস্ট করা হচ্ছে...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "প্রকাশ করুন",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
