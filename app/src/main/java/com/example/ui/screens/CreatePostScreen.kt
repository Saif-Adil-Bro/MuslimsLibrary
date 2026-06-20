package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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

// Private properties to prevent conflicting declarations in the library package namespace
private val LocalPrimaryGradientStart = Color(0xFF667EEA)
private val LocalPrimaryGradientEnd = Color(0xFF764BA2)
private val LocalPrimaryPurple = Color(0xFF6366F1)
private val LocalDarkPurple = Color(0xFF4F46E5)
private val LocalBackgroundPurplePastel = Color(0xFFF5F3FF)
private val LocalCardBackgroundWhite = Color(0xFFFFFFFF)
private val LocalTextGrayMain = Color(0xFF1F2937)
private val LocalTextGrayMuted = Color(0xFF6B7280)
private val LocalBorderLightVariant = Color(0xFFE5E7EB)

fun validatePost(title: String, content: String, category: String): String? {
    if (title.length < 3) return "Title must be at least 3 characters."
    if (title.length > 200) return "Title cannot exceed 200 characters."
    if (content.length < 10) return "Content must be at least 10 characters."
    if (content.length > 5000) return "Content cannot exceed 5000 characters."
    if (category.isBlank()) return "Please select a category."
    
    val bannedWords = listOf("spam", "scam", "fake", "badtext")
    val lowerTitle = title.lowercase()
    val lowerContent = content.lowercase()
    
    if (bannedWords.any { lowerTitle.contains(it) }) {
        return "Please use appropriate wording in the title."
    }
    if (bannedWords.any { lowerContent.contains(it) }) {
        return "Please use appropriate wording in the content."
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    forumViewModel: ForumViewModel,
    userId: String,
    userEmail: String,
    userRole: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Quran") }
    var content by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    val isPublishing = remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val categoryMappings = mapOf(
        "General" to "General",
        "Quran" to "Quran",
        "Hadith" to "Hadith",
        "Fiqh" to "Fiqh",
        "Sira" to "Q&A",
        "Others" to "Others"
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
                        text = "Create New Post",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
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
                    containerColor = LocalDarkPurple
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LocalBackgroundPurplePastel)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Title Input field with character length indicator
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Post Title *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalTextGrayMain
                    )
                    Text(
                        text = "${title.length}/200",
                        fontSize = 11.sp,
                        color = if (title.length > 200) Color.Red else LocalTextGrayMuted
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 250) title = it },
                    placeholder = { Text("Enter post title...", color = LocalTextGrayMuted, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = LocalCardBackgroundWhite,
                        unfocusedContainerColor = LocalCardBackgroundWhite,
                        focusedBorderColor = LocalPrimaryPurple,
                        unfocusedBorderColor = LocalBorderLightVariant,
                        focusedLabelColor = LocalPrimaryPurple
                    ),
                    singleLine = true
                )
            }

            // Category Selection Pills
            Column {
                Text(
                    text = "Category *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalTextGrayMain
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Visual Wrap Flow Row for Category Select Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Quran", "Hadith", "Fiqh", "Sira").forEach { category ->
                        val isSelected = selectedCategory == category
                        val displayLabel = categoryMappings[category] ?: category
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.linearGradient(colors = listOf(LocalPrimaryGradientStart, LocalPrimaryGradientEnd))
                                    } else {
                                        Brush.linearGradient(colors = listOf(Color(0xFFF3F4F6), Color(0xFFF3F4F6)))
                                    }
                                )
                                .clickable { selectedCategory = category }
                                .border(
                                    1.dp,
                                    if (isSelected) LocalPrimaryPurple else LocalBorderLightVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) Color.White else LocalTextGrayMuted
                            )
                        }
                    }
                }
            }

            // Rich Text Editor with Floating Toolbar inside a beautiful white container card
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "What would you like to discuss? *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalTextGrayMain
                    )
                    Text(
                        text = "${content.length}/5000",
                        fontSize = 11.sp,
                        color = if (content.length > 5000) Color.Red else LocalTextGrayMuted
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, shape = RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LocalCardBackgroundWhite),
                    border = BorderStroke(1.dp, LocalBorderLightVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Floating editor toolbar inside header of editor
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9FAFB))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .border(BorderStroke(0.5.dp, LocalBorderLightVariant)),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Toolbar buttons append helper markdown/syntax
                            IconButton(onClick = {
                                content += " **Bold Text** "
                                Toast.makeText(context, "Bold template added", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add Media", tint = LocalPrimaryPurple, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                content += " *Italic Text* "
                                Toast.makeText(context, "Italics template added", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Format Italic", tint = LocalPrimaryPurple, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                Toast.makeText(context, "Voice input simulated!", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = LocalPrimaryPurple, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                content += " `Code Block` "
                                Toast.makeText(context, "Code tag template added", Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Code, contentDescription = "Code snippet", tint = LocalPrimaryPurple, modifier = Modifier.size(18.dp))
                            }
                        }
                        
                        // Editor TextField itself
                        OutlinedTextField(
                            value = content,
                            onValueChange = { if (it.length <= 5500) content = it },
                            placeholder = {
                                Text(
                                    text = "Share your thoughts, reference book insights, or ask your community questions here...",
                                    color = LocalTextGrayMuted,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            maxLines = 15
                        )
                    }
                }
            }

            // Tags Input
            Column {
                Text(
                    text = "Tags (Separated by commas)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalTextGrayMain
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    placeholder = { Text("e.g. quran, tafsir, salah", color = LocalTextGrayMuted, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = LocalCardBackgroundWhite,
                        unfocusedContainerColor = LocalCardBackgroundWhite,
                        focusedBorderColor = LocalPrimaryPurple,
                        unfocusedBorderColor = LocalBorderLightVariant
                    ),
                    singleLine = true
                )
            }

            // Attachment button matching the style requested ("Add images or documents")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        Toast.makeText(context, "Attachment selected!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Attachment,
                    contentDescription = "Attachment icon",
                    tint = LocalPrimaryPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add images or documents",
                    color = LocalPrimaryPurple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Privacy Switch with gorgeous sliding circular indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Public",
                    fontSize = 14.sp,
                    fontWeight = if (!isPrivate) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isPrivate) LocalTextGrayMain else LocalTextGrayMuted
                )
                Spacer(modifier = Modifier.width(16.dp))
                
                // Switch Container
                Box(
                    modifier = Modifier
                        .size(52.dp, 28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (isPrivate) {
                                    listOf(Color(0xFFD1D5DB), Color(0xFFD1D5DB))
                                } else {
                                    listOf(LocalPrimaryGradientStart, LocalPrimaryGradientEnd)
                                }
                            )
                        )
                        .clickable { isPrivate = !isPrivate }
                        .padding(3.dp)
                ) {
                    val targetOffset by animateDpAsState(targetValue = if (isPrivate) 0.dp else 24.dp)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .offset(x = targetOffset)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Private",
                    fontSize = 14.sp,
                    fontWeight = if (isPrivate) FontWeight.Bold else FontWeight.Medium,
                    color = if (isPrivate) LocalTextGrayMain else LocalTextGrayMuted
                )
            }

            // Action Buttons: Cancel and Publish
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel Button
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, LocalBorderLightVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalTextGrayMain)
                ) {
                    Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

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
                                userId = userId,
                                email = userEmail,
                                title = sanitizedTitle,
                                content = sanitizedContent,
                                category = selectedCategory,
                                role = userRole,
                                onSuccess = {
                                    Toast.makeText(context, "Post published successfully!", Toast.LENGTH_LONG).show()
                                    isPublishing.value = false
                                    onBackClick()
                                }
                            )
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please fill out both the title and content fields!")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalPrimaryPurple,
                        disabledContainerColor = Color(0xFFC7D2FE)
                    ),
                    enabled = !isPublishing.value
                ) {
                    if (isPublishing.value) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Publish",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
