package com.example.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ContentSanitizer
import com.example.ui.viewmodel.ForumViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Private properties to prevent conflicting declarations in the library package namespace
// private val MaterialTheme.colorScheme.primary = Color(0xFF667EEA)
// private val MaterialTheme.colorScheme.tertiary = Color(0xFF764BA2)
// Removed invalid top level properties

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var tagsInput by remember { mutableStateOf("") }
    val postTags by forumViewModel.postTags.collectAsState()
    val forumCategories by forumViewModel.categories.collectAsState()
    var isPrivate by remember { mutableStateOf(false) }
    val isPublishing = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        forumViewModel.loadCategories()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (spokenText != null) {
                val text = content.text
                val selectionStart = content.selection.start
                val selectionEnd = content.selection.end
                val newText = text.substring(0, selectionStart) + spokenText + text.substring(selectionEnd)
                content = TextFieldValue(
                    text = newText,
                    selection = TextRange(selectionStart + spokenText.length)
                )
            }
        }
    }

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
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${title.length}/200",
                        fontSize = 11.sp,
                        color = if (title.length > 200) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 250) title = it },
                    placeholder = { Text("Enter post title...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Visual Wrap Flow Row for Category Select Pills
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    forumCategories.filter { it != "All" }.forEach { category ->
                        val isSelected = selectedCategory == category
                        val displayLabel = categoryMappings[category] ?: category
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                                    } else {
                                        Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                                    }
                                )
                                .clickable { selectedCategory = category }
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${content.text.length}/5000",
                        fontSize = 11.sp,
                        color = if (content.text.length > 5000) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, shape = RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Floating editor toolbar inside header of editor
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceVariant)),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val wrapText = { prefix: String, suffix: String ->
                                val text = content.text
                                val start = content.selection.start
                                val end = content.selection.end
                                if (start != end) {
                                    val before = text.substring(0, start)
                                    val selected = text.substring(start, end)
                                    val after = text.substring(end)
                                    content = TextFieldValue(before + prefix + selected + suffix + after, TextRange(end + prefix.length + suffix.length))
                                } else {
                                    val before = text.substring(0, start)
                                    val after = text.substring(start)
                                    content = TextFieldValue(before + prefix + suffix + after, TextRange(start + prefix.length))
                                }
                            }
                            
                            // Toolbar buttons append helper markdown/syntax
                            IconButton(onClick = {
                                wrapText("**", "**")
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add Media", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                wrapText("*", "*")
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Format Italic", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                }
                                try {
                                    speechRecognizerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Voice input not supported", Toast.LENGTH_SHORT).show()
                                }
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                wrapText("```\n", "\n```")
                            }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Code, contentDescription = "Code snippet", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        
                        // Editor TextField itself
                        OutlinedTextField(
                            value = content,
                            onValueChange = { if (it.text.length <= 5500) content = it },
                            placeholder = {
                                Text(
                                    text = "Share your thoughts, reference book insights, or ask your community questions here...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { newValue ->
                        if (newValue.endsWith(",") || newValue.endsWith("\n")) {
                            val tag = newValue.trim(',', '\n', ' ')
                            if (tag.isNotEmpty()) {
                                forumViewModel.addTag(tag)
                            }
                            tagsInput = ""
                        } else {
                            tagsInput = newValue
                        }
                    },
                    placeholder = { Text("e.g. quran, tafsir, salah", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true
                )
                if (postTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        postTags.forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove tag",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { forumViewModel.removeTag(tag) }
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add images or documents",
                    color = MaterialTheme.colorScheme.primary,
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
                    color = if (!isPrivate) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
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
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
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
                    color = if (isPrivate) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
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
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                // Publish submit button
                val isFormValid = title.isNotBlank() && content.text.isNotBlank()
                Button(
                    onClick = {
                        if (isFormValid) {
                            val sanitizedTitle = ContentSanitizer.sanitize(title.trim())
                            val sanitizedContent = ContentSanitizer.sanitize(content.text.trim())
                            
                            val validationError = validatePost(sanitizedTitle, sanitizedContent, selectedCategory)
                            if (validationError != null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(validationError)
                                }
                                return@Button
                            }

                            isPublishing.value = true
                            
                            // add leftover tag if exists
                            val finalTagInput = tagsInput.trim(',', '\n', ' ')
                            if (finalTagInput.isNotEmpty()) {
                                forumViewModel.addTag(finalTagInput)
                            }

                            forumViewModel.createPost(
                                userId = userId,
                                email = userEmail,
                                title = sanitizedTitle,
                                content = sanitizedContent,
                                category = selectedCategory,
                                role = userRole,
                                tags = forumViewModel.postTags.value,
                                onSuccess = {
                                    Toast.makeText(context, "Post published successfully!", Toast.LENGTH_LONG).show()
                                    isPublishing.value = false
                                    forumViewModel.clearTags()
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
                        containerColor = MaterialTheme.colorScheme.primary,
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
