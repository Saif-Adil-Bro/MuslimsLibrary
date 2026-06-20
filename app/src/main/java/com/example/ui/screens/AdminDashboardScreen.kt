package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseAuthor
import com.example.data.SupabaseBook
import com.example.data.SupabaseCategory
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    onNavigateToAddBook: () -> Unit,
    userEmail: String = "admin@muslimslibrary.org",
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Collect states
    val totalBooks by adminViewModel.totalBooksCount.collectAsState()
    val adminBooks by adminViewModel.adminBooks.collectAsState()
    val adminAuthors by adminViewModel.adminAuthors.collectAsState()
    val adminCategories by adminViewModel.adminCategories.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("ড্যাশবোর্ড", "বইসমূহ", "লেখকগণ", "ক্যাটেগরি")

    LaunchedEffect(Unit) {
        adminViewModel.getTotalBooksCount()
        adminViewModel.loadAdminData()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF043B2B), Color(0xFF0A4E38))
                        )
                    )
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ISLAMIC DIGITAL REPOSITORY MANAGER",
                            color = Color(0xFFA3E2C9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Admin Hub",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    IconButton(
                        onClick = { 
                            adminViewModel.getTotalBooksCount()
                            adminViewModel.loadAdminData()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Stats",
                            tint = Color.White
                        )
                    }
                }

                // Custom dynamic category style TabRow
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFD4AF37),
                            height = 3.dp
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    text = title, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ) 
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFCFDF9))
        ) {
            when (selectedTab) {
                0 -> DashboardTabContent(
                    totalBooks = totalBooks,
                    userEmail = userEmail,
                    onNavigateToAddBook = onNavigateToAddBook,
                    onLogoutClick = onLogoutClick
                )
                1 -> BooksTabContent(
                    books = adminBooks,
                    categories = adminCategories,
                    onUpdateBook = { id, title, author, category, coverUrl, fileUrl ->
                        adminViewModel.updateBook(id, title, author, category, coverUrl, fileUrl)
                    },
                    onDeleteBook = { id ->
                        adminViewModel.deleteBook(id)
                    }
                )
                2 -> AuthorsTabContent(
                    authors = adminAuthors,
                    onSaveAuthor = { name, bio ->
                        adminViewModel.saveAuthor(name, bio)
                    },
                    onDeleteAuthor = { id ->
                        adminViewModel.deleteAuthor(id)
                    }
                )
                3 -> CategoriesTabContent(
                    categories = adminCategories,
                    onSaveCategory = { id, name ->
                        adminViewModel.saveCategory(id, name)
                    },
                    onDeleteCategory = { id ->
                        adminViewModel.deleteCategory(id)
                    }
                )
            }
        }
    }
}

// ==========================================
// Tab 0: Dashboard Tab
// ==========================================
@Composable
fun DashboardTabContent(
    totalBooks: Int,
    userEmail: String,
    onNavigateToAddBook: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.LibraryBooks,
                        contentDescription = null,
                        tint = Color(0xFF0D6E50),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Books",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = totalBooks.toString(),
                        fontSize = 22.sp,
                        color = Color(0xFF043B2B),
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.testTag("admin_dashboard_total_books")
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Node Status",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ONLINE",
                        fontSize = 22.sp,
                        color = Color(0xFF0A4E38),
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Database Provider status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cabin,
                    contentDescription = "Bucket",
                    tint = Color(0xFF0A4E38),
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Storage Bucket: 'books'",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF032B1D)
                    )
                    Text(
                        text = "RLS active: download = PUBLIC, upload = ADMIN only",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // My Profile Card with Logout Option
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "My Profile Icon",
                        tint = Color(0xFF0A4E38),
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "My Profile",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF032B1D)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userEmail,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ROLE: ADMIN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0A4E38)
                        )
                    }
                }

                Divider(color = Color(0xFFEEEEEE))

                Button(
                    onClick = onLogoutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("admin_profile_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logout Session",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Prominent "Add New Book" action trigger section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF4F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = Color(0xFF0A4E38),
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "Contribute to the Islamic Library",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF032B1D),
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Publish authenticated digital manuscripts, Hadith compilations, Fiqh monographs, Tafseer records, and Seerah biographies to the public repository safely.",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Button(
                    onClick = onNavigateToAddBook,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E38)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_add_book_nav_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "AddIcon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add New Book",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// Tab 1: Books Management Tab
// ==========================================
@Composable
fun BooksTabContent(
    books: List<SupabaseBook>,
    categories: List<SupabaseCategory>,
    onUpdateBook: (String, String, String, String, String?, String?) -> Unit,
    onDeleteBook: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingBook by remember { mutableStateOf<SupabaseBook?>(null) }
    var showDeleteConfirmBook by remember { mutableStateOf<SupabaseBook?>(null) }

    val filteredBooks = remember(books, searchQuery) {
        if (searchQuery.isBlank()) books else {
            books.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.author.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("বই বা লেখক দিয়ে খুঁজুন...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredBooks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("কোনো বই পাওয়া যায়নি", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredBooks) { book ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEFF2EE)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF032B1D),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "লেখক: ${book.author}",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(text = "•", color = Color.LightGray, fontSize = 11.sp)
                                    Text(
                                        text = "ক্যাটেগরি: ${book.category}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF0A4E38),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.wrapContentWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { editingBook = book },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Book",
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showDeleteConfirmBook = book },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Book",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Book Dialog
    editingBook?.let { book ->
        var editTitle by remember { mutableStateOf(book.title) }
        var editAuthor by remember { mutableStateOf(book.author) }
        var editCategory by remember { mutableStateOf(book.category) }
        var editCoverUrl by remember { mutableStateOf(book.coverImageUrl ?: "") }
        var editFileUrl by remember { mutableStateOf(book.fileUrl ?: "") }
        var isDropdownExpanded by remember { mutableStateOf(false) }

        val systemCategories = categories.map { it.name }
        val predefinedList = if (systemCategories.isEmpty()) {
            listOf("কুরআন", "হাদিস", "ফিকহ", "তাফসীর", "সীরাত", "অন্যান্য")
        } else {
            systemCategories
        }

        AlertDialog(
            onDismissRequest = { editingBook = null },
            title = { Text("বইয়ের তথ্য পরিবর্তন করুন", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("বইয়ের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editAuthor,
                        onValueChange = { editAuthor = it },
                        label = { Text("লেখক") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category selection with chips support
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = { editCategory = it },
                            label = { Text("ক্যাটেগরি (কমা দিয়ে একাধিক)") },
                            trailingIcon = {
                                IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            predefinedList.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        val currentList = editCategory.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                                        if (currentList.contains(cat)) {
                                            currentList.remove(cat)
                                        } else {
                                            currentList.add(cat)
                                        }
                                        editCategory = currentList.joinToString(", ")
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            predefinedList.forEach { cat ->
                                val isSelected = editCategory.split(",").map { it.trim() }.contains(cat)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val currentList = editCategory.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                                        if (currentList.contains(cat)) {
                                            currentList.remove(cat)
                                        } else {
                                            currentList.add(cat)
                                        }
                                        editCategory = currentList.joinToString(", ")
                                    },
                                    label = { Text(cat, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editCoverUrl,
                        onValueChange = { editCoverUrl = it },
                        label = { Text("কভার ফটো ইউআরএল") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editFileUrl,
                        onValueChange = { editFileUrl = it },
                        label = { Text("বইয়ের ফাইল ইউআরএল") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateBook(
                            book.id,
                            editTitle,
                            editAuthor,
                            editCategory,
                            if (editCoverUrl.isBlank()) null else editCoverUrl,
                            if (editFileUrl.isBlank()) null else editFileUrl
                        )
                        editingBook = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E38))
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBook = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Delete confirm dialog
    showDeleteConfirmBook?.let { book ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmBook = null },
            title = { Text("বই মুছে ফেলুন") },
            text = { Text("'${book.title}' বইটি কি লাইব্রেরি থেকে চিরতরে মুছে ফেলতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBook(book.id)
                        showDeleteConfirmBook = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmBook = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

// ==========================================
// Tab 2: Authors Management Tab
// ==========================================
@Composable
fun AuthorsTabContent(
    authors: List<SupabaseAuthor>,
    onSaveAuthor: (String, String?) -> Unit,
    onDeleteAuthor: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingAuthor by remember { mutableStateOf<SupabaseAuthor?>(null) }
    var isAddingAuthor by remember { mutableStateOf(false) }
    var confirmDeleteAuthor by remember { mutableStateOf<SupabaseAuthor?>(null) }

    val filteredAuthors = remember(authors, searchQuery) {
        if (searchQuery.isBlank()) authors else {
            authors.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("লেখক খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { isAddingAuthor = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E38)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("যোগ করুন", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredAuthors.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("কোনো লেখক পাওয়া যায়নি", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAuthors) { author ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEFF2EE)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = author.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF032B1D)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = author.bio ?: "কোনো জীবনী নেই",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.wrapContentWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { editingAuthor = author },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Author",
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { confirmDeleteAuthor = author },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Author",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Author Dialog
    if (isAddingAuthor || editingAuthor != null) {
        val author = editingAuthor
        var name by remember { mutableStateOf(author?.name ?: "") }
        var bio by remember { mutableStateOf(author?.bio ?: "") }

        AlertDialog(
            onDismissRequest = {
                isAddingAuthor = false
                editingAuthor = null
            },
            title = { Text(if (author == null) "নতুন লেখক যোগ করুন" else "লেখকের তথ্য পরিবর্তন করুন", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("লেখকের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("জীবনী/পরিচিতি") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveAuthor(name, if (bio.isBlank()) null else bio)
                        }
                        isAddingAuthor = false
                        editingAuthor = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E38))
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isAddingAuthor = false
                        editingAuthor = null
                    }
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Delete confirming
    confirmDeleteAuthor?.let { author ->
        AlertDialog(
            onDismissRequest = { confirmDeleteAuthor = null },
            title = { Text("লেখক মুছে ফেলুন") },
            text = { Text("'${author.name}' লেখককে কি মুছে ফেলতে চান? এতে তাঁর বইসমূহ ডিলিট হবে না।") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAuthor(author.id)
                        confirmDeleteAuthor = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAuthor = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

// ==========================================
// Tab 3: Categories Management Tab
// ==========================================
@Composable
fun CategoriesTabContent(
    categories: List<SupabaseCategory>,
    onSaveCategory: (String?, String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<SupabaseCategory?>(null) }
    var isAddingCategory by remember { mutableStateOf(false) }
    var confirmDeleteCategory by remember { mutableStateOf<SupabaseCategory?>(null) }

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories else {
            categories.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("ক্যাটেগরি খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { isAddingCategory = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E38)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("যোগ করুন", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredCategories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("কোনো ক্যাটেগরি পাওয়া যায়নি", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCategories) { category ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEFF2EE)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF032B1D),
                                modifier = Modifier.weight(1f)
                            )

                            Row(
                                modifier = Modifier.wrapContentWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { editingCategory = category },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Category",
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { confirmDeleteCategory = category },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Category",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Category Dialog
    if (isAddingCategory || editingCategory != null) {
        val category = editingCategory
        var name by remember { mutableStateOf(category?.name ?: "") }

        AlertDialog(
            onDismissRequest = {
                isAddingCategory = false
                editingCategory = null
            },
            title = { Text(if (category == null) "নতুন ক্যাটেগরি যোগ করুন" else "ক্যাটেগরি পরিবর্তন করুন", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("ক্যাটেগরির নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveCategory(category?.id, name)
                        }
                        isAddingCategory = false
                        editingCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E38))
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isAddingCategory = false
                        editingCategory = null
                    }
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Delete matching category
    confirmDeleteCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { confirmDeleteCategory = null },
            title = { Text("ক্যাটেগরি মুছে ফেলুন") },
            text = { Text("'${category.name}' ক্যাটেগরি কি চিরতরে মুছে ফেলতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(category.id)
                        confirmDeleteCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCategory = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
