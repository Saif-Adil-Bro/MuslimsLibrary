package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.viewmodel.ProfileUiState
import com.example.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val selectedDefaultAvatarUrl by viewModel.selectedDefaultAvatarUrl.collectAsState()
    val pendingCustomImageUri by viewModel.pendingCustomImageUri.collectAsState()
    val scrollState = rememberScrollState()

    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var hasLoadedInitialValues by remember { mutableStateOf(false) }

    // Side effect to load the details to the form
    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success && !hasLoadedInitialValues) {
            val user = (uiState as ProfileUiState.Success).user
            displayName = user.displayName ?: ""
            bio = user.bio ?: ""
            hasLoadedInitialValues = true
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.selectCustomImage(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("প্রোফাইল সংশোধন", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (displayName.isBlank()) {
                                Toast.makeText(context, "অনুগ্রহ করে একটি নাম লিখুন।", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.updateProfile(displayName, bio) { success, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    if (success) {
                                        onBackClick()
                                    }
                                }
                            }
                        },
                        enabled = !uploadProgress && uiState !is ProfileUiState.Loading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "সংরক্ষণ",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF043B2B)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7F9FA))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Avatar Container click listener to choose image
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF043B2B).copy(alpha = 0.08f))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val currentAvatarUrl = if (uiState is ProfileUiState.Success) {
                        (uiState as ProfileUiState.Success).user.avatarUrl
                    } else null

                    val imageSource: Any? = when {
                        selectedDefaultAvatarUrl != null -> selectedDefaultAvatarUrl
                        pendingCustomImageUri != null -> pendingCustomImageUri
                        else -> currentAvatarUrl
                    }

                    if (imageSource != null) {
                        AsyncImage(
                            model = imageSource,
                            contentDescription = "প্রোফাইল ছবি",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val initLet = if (displayName.isNotBlank()) displayName.first().toString() else "U"
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0A4E38)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initLet.uppercase(),
                                color = Color.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Camera Icon Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "ছবি পরিবর্তন করুন",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "গ্যালারি থেকে ছবি পরিবর্তন করতে এখানে ট্যাপ করুন",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                // ---------------- Choose Default Avatar Section ----------------
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ডিফল্ট অবতার বেছে নিন (Choose Default Avatar)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF043B2B)
                        )
                        if (selectedDefaultAvatarUrl != null || pendingCustomImageUri != null) {
                            TextButton(
                                onClick = { viewModel.clearAvatarSelections() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                            ) {
                                Text("রিসেট", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    val defaultAvatars = listOf(
                        "https://api.dicebear.com/7.x/adventurer/png?seed=1",
                        "https://api.dicebear.com/7.x/adventurer/png?seed=2",
                        "https://api.dicebear.com/7.x/adventurer/png?seed=3",
                        "https://api.dicebear.com/7.x/adventurer/png?seed=4",
                        "https://api.dicebear.com/7.x/adventurer/png?seed=5",
                        "https://api.dicebear.com/7.x/adventurer/png?seed=6",
                        "https://api.dicebear.com/7.x/adventurer/png?seed=7",
                        "https://api.dicebear.com/7.x/adventurer/png?seed=8"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(defaultAvatars.size) { index ->
                            val avatarUrl = defaultAvatars[index]
                            val isSelected = selectedDefaultAvatarUrl == avatarUrl

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable {
                                        if (isSelected) {
                                            viewModel.selectDefaultAvatar(null)
                                        } else {
                                            viewModel.selectDefaultAvatar(avatarUrl)
                                        }
                                    }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(3.dp, Color(0xFF043B2B), CircleShape)
                                        } else {
                                            Modifier.border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape)
                                        }
                                    )
                                    .padding(if (isSelected) 3.dp else 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "অবতার ${index + 1}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color(0xFF043B2B).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "নির্বাচিত",
                                            tint = Color(0xFF043B2B),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Forms
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Display Name
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("প্রদর্শন নাম (Display Name)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF043B2B),
                                focusedLabelColor = Color(0xFF043B2B),
                                cursorColor = Color(0xFF043B2B)
                            )
                        )

                        // Bio
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("আমার পরিচিতি (Bio)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF043B2B),
                                focusedLabelColor = Color(0xFF043B2B),
                                cursorColor = Color(0xFF043B2B)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Saved changes button
                Button(
                    onClick = {
                        if (displayName.isBlank()) {
                            Toast.makeText(context, "অনুগ্রহ করে একটি নাম লিখুন।", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateProfile(displayName, bio) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    onBackClick()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF043B2B)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uploadProgress && uiState !is ProfileUiState.Loading
                ) {
                    Text(
                        text = "তথ্য সংরক্ষণ করুন",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Spinner Overlay for loading and upload
            if (uploadProgress || uiState is ProfileUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(enabled = false) {}, // fully block user touches
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = Color(0xFF043B2B))
                            Text(
                                text = if (uploadProgress) "ছবি আপলোড হচ্ছে..." else "প্রোফাইল সংরক্ষণ হচ্ছে...",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
