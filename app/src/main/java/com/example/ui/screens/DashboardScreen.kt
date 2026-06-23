package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import com.example.data.SupabaseBook
import com.example.ui.components.*
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ForumViewModel
import com.example.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    libraryViewModel: LibraryViewModel,
    homeViewModel: HomeViewModel,
    adminViewModel: AdminViewModel,
    forumViewModel: ForumViewModel,
    profileViewModel: ProfileViewModel,
    authorViewModel: com.example.ui.viewmodel.AuthorViewModel,
    localSyncRepository: com.example.data.repository.LocalSyncRepository,
    userEmail: String,
    userUid: String = "",
    userRole: String,
    onLogoutClick: () -> Unit,
    onBookClick: (SupabaseBook) -> Unit,
    onNavigateToAddBook: () -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToAllBooks: (sortBy: String, categoryFilter: String?) -> Unit,
    onNavigateToCategory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit = {},
    modifier: Modifier = Modifier,
    debugInfo: String = "",
    isDebugMode: Boolean = false,
    onToggleDebug: () -> Unit = {},
    backupManager: com.example.data.backup.BackupManager? = null,
    isGuestMode: Boolean = false,
    onNotificationsClick: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    unreadNotificationsCount: Int = 0
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var tabHistory by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var drawerScreenTitle by rememberSaveable { mutableStateOf("") }
    val searchQuery by homeViewModel.searchQuery.collectAsState()

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(Unit) {
        if (!isGuestMode) {
            profileViewModel.loadProfile()
        }
    }

    val profileUiState by profileViewModel.uiState.collectAsState()
    val nameFromEmail = userEmail.split("@").first().replaceFirstChar { it.uppercase() }
    val realDisplayName = if (isGuestMode) {
        "গেস্ট ইউজার"
    } else {
        when (val state = profileUiState) {
            is com.example.ui.viewmodel.ProfileUiState.Success -> state.user.displayName ?: nameFromEmail
            else -> nameFromEmail
        }
    }
    val realEmail = if (isGuestMode) {
        "গেস্ট মোড (অফলাইন)"
    } else {
        when (val state = profileUiState) {
            is com.example.ui.viewmodel.ProfileUiState.Success -> state.user.email
            else -> userEmail
        }
    }

    val isBackHandlerEnabled = drawerState.isOpen || drawerScreenTitle.isNotEmpty() || selectedTab != 0 || tabHistory.isNotEmpty()

    BackHandler(enabled = isBackHandlerEnabled) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (drawerScreenTitle.isNotEmpty()) {
            drawerScreenTitle = ""
        } else if (tabHistory.isNotEmpty()) {
            val last = tabHistory.last()
            tabHistory = tabHistory.dropLast(1)
            selectedTab = last
        } else if (selectedTab != 0) {
            selectedTab = 0
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                modifier = Modifier.width(320.dp)
            ) {
                    val sidebarViewModel: com.example.ui.navigation.sidebar.SidebarViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    val profile = com.example.ui.navigation.sidebar.UserProfile(
                        name = realDisplayName,
                        email = realEmail,
                        initials = realDisplayName.firstOrNull()?.uppercase() ?: "U"
                    )
                    
                    val mainSectionItems = mutableListOf(
                        com.example.ui.navigation.sidebar.MenuItem("home", "ড্যাশবোর্ড", Icons.Filled.Home),
                        com.example.ui.navigation.sidebar.MenuItem("my_books", "আমার বই", Icons.Filled.Book),
                        com.example.ui.navigation.sidebar.MenuItem("category", "ক্যাটাগরি", Icons.Filled.Category),
                        com.example.ui.navigation.sidebar.MenuItem("downloads", "ডাউনলোড", Icons.Filled.Download)
                    )
                    
                    if (userRole.equals("admin", ignoreCase = true)) {
                        mainSectionItems.add(
                            com.example.ui.navigation.sidebar.MenuItem("admin_panel", "অ্যাডমিন প্যানেল", Icons.Filled.Security)
                        )
                    }

                    val otherSectionItems = mutableListOf(
                        com.example.ui.navigation.sidebar.MenuItem("settings", "সেটিংস", Icons.Filled.Settings),
                        com.example.ui.navigation.sidebar.MenuItem("help", "সাহায্য/সম্পর্কে", Icons.Filled.Info)
                    )
                    
                    if (!isGuestMode) {
                        otherSectionItems.add(
                            com.example.ui.navigation.sidebar.MenuItem("logout", "লগআউট", Icons.AutoMirrored.Filled.ExitToApp, isDestructive = true)
                        )
                    }

                    val sections = listOf(
                        com.example.ui.navigation.sidebar.MenuSection(title = null, items = mainSectionItems),
                        com.example.ui.navigation.sidebar.MenuSection(title = "অন্যান্য", items = otherSectionItems)
                    )

                    com.example.ui.components.sidebar.SidebarDrawer(
                        profile = profile,
                        sections = sections,
                        viewModel = sidebarViewModel,
                        footerText = "আমার হিসাব",
                        versionText = "ভার্সন ১.০.০",
                        onMenuItemClick = { route ->
                            scope.launch { drawerState.close() }
                            if (route == "logout") {
                                onLogoutClick()
                                return@SidebarDrawer
                            }
                            when (route) {
                                "home" -> {
                                    drawerScreenTitle = ""
                                    selectedTab = 0
                                }
                                "category" -> {
                                    onNavigateToCategory()
                                }
                                "admin_panel" -> {
                                    onNavigateToAdminDashboard()
                                }
                                "my_books" -> {
                                    drawerScreenTitle = "🚧 আমার বই"
                                }
                                "downloads" -> {
                                    drawerScreenTitle = "🚧 ডাউনলোড"
                                }
                                "settings" -> {
                                    onNavigateToSettings()
                                }
                                "help" -> {
                                    drawerScreenTitle = "🚧 সাহায্য"
                                }
                            }
                        }
                    )
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column {
                     StickyHeader(
                        onMenuClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        },
                        onFavoriteClick = {
                            drawerScreenTitle = "🚧 প্রিয় বই"
                        },
                        onNotificationsClick = onNotificationsClick,
                        isGuestMode = isGuestMode,
                        unreadCount = unreadNotificationsCount
                    )
                    // Sticky search bar is only visible on standard Home tab content
                    if (selectedTab == 0 && drawerScreenTitle.isEmpty()) {
                        StickySearchBar(
                            query = searchQuery,
                            onQueryChange = { homeViewModel.onSearchQueryChanged(it) },
                            onSearchClick = { homeViewModel.refreshBooks() }
                        )
                    }
                    if (isGuestMode && selectedTab == 0 && drawerScreenTitle.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFFDE7) // Warm premium cream
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "গেস্ট সেশন সচল আছে 📖",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF5D4037)
                                    )
                                    Text(
                                        text = "লগইন করুন এবং আপনার পড়াশোনার রেকর্ড ক্লাউডে সুরক্ষিত করুন!",
                                        fontSize = 12.sp,
                                        color = Color(0xFF795548)
                                    )
                                }
                                Button(
                                    onClick = onLogoutClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("লগইন করুন", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                BottomNavBar(
                    selectedTab = if (drawerScreenTitle.isNotEmpty()) -1 else selectedTab,
                    onTabSelected = { tab ->
                        drawerScreenTitle = "" // Clear drawer placeholders on tab switches
                        if (selectedTab != tab) {
                            tabHistory = tabHistory.filter { it != selectedTab } + selectedTab
                            selectedTab = tab
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (drawerScreenTitle.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EmptyPlaceholder(
                            title = drawerScreenTitle,
                            message = "আমরা শীঘ্রই এই বিভাগটিকে সরাসরি মূল অ্যাপ্লিকেশনের সংগে লাইভ করতে কাজ করছি।"
                        )
                        // If it's download, let's also offer the option to open local folder downloads
                        if (drawerScreenTitle.contains("ডাউনলোড")) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onNavigateToDownloads,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("ডাউনলোডকৃত অফলাইন বই দেখুন", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                } else {
                    when (selectedTab) {
                        0 -> {
                            HomeScreen(
                                homeViewModel = homeViewModel,
                                userEmail = userEmail,
                                role = userRole,
                                onBookClick = onBookClick,
                                onNavigateToAllBooks = onNavigateToAllBooks,
                                onNavigateToCategory = onNavigateToCategory
                            )
                        }
                        1 -> {
                            LibraryScreen(
                                libraryViewModel = libraryViewModel,
                                userId = userEmail,
                                onBookClick = { bookId ->
                                    onBookClick(
                                        SupabaseBook(
                                            id = bookId,
                                            title = "",
                                            author = "",
                                            category = ""
                                        )
                                    )
                                },
                                onGoToHomeClick = {
                                    if (selectedTab != 0) {
                                        tabHistory = tabHistory.filter { it != selectedTab } + selectedTab
                                        selectedTab = 0
                                    }
                                }
                            )
                        }
                        2 -> {
                            ForumScreen(
                                forumViewModel = forumViewModel,
                                userId = userUid,
                                userEmail = userEmail,
                                userRole = userRole,
                                onNavigateToCreatePost = onNavigateToCreatePost,
                                onNavigateToPostDetail = onNavigateToPostDetail
                            )
                        }
                        3 -> {
                            AuthorScreen(
                                viewModel = authorViewModel,
                                onBookClick = onBookClick,
                                onBackClick = {
                                    if (tabHistory.isNotEmpty()) {
                                        val last = tabHistory.last()
                                        tabHistory = tabHistory.dropLast(1)
                                        selectedTab = last
                                    } else {
                                        selectedTab = 0
                                    }
                                }
                            )
                        }
                        4 -> {
                            ProfileScreen(
                                viewModel = profileViewModel,
                                localSyncRepository = localSyncRepository,
                                userId = userEmail,
                                onEditProfileClick = {
                                    onNavigateToProfile() // Navigates to edit/details view
                                },
                                onLogoutClick = onLogoutClick,
                                onBackClick = {
                                    if (tabHistory.isNotEmpty()) {
                                        val last = tabHistory.last()
                                        tabHistory = tabHistory.dropLast(1)
                                        selectedTab = last
                                    } else {
                                        selectedTab = 0
                                    }
                                },
                                onAdminDashboardClick = onNavigateToAdminDashboard,
                                onNotificationSettingsClick = onNavigateToNotificationSettings,
                                isGuestMode = isGuestMode
                            )
                        }
                    }
                }

                // Keep existing float debug overlay if enabled
                if (isDebugMode && debugInfo.isNotEmpty()) {
                    var isLogsVisible by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .fillMaxWidth(0.95f)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.92f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔧 Debug Diagnostic Console",
                                        color = Color.Green,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row {
                                        TextButton(
                                            onClick = { isLogsVisible = !isLogsVisible },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(if (isLogsVisible) "Hide" else "Show", color = Color.White, fontSize = 10.sp)
                                        }
                                        TextButton(
                                            onClick = onToggleDebug,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Close Info", color = Color.Red, fontSize = 10.sp)
                                        }
                                    }
                                }
                                if (isLogsVisible) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = debugInfo,
                                        color = Color.LightGray,
                                        fontSize = 9.sp,
                                        lineHeight = 12.sp,
                                        modifier = Modifier.heightIn(max = 120.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
