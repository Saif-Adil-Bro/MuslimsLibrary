package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    profileViewModel: ProfileViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val theme by settingsViewModel.theme.collectAsState()
    val language by settingsViewModel.language.collectAsState()
    val fontSize by settingsViewModel.fontSize.collectAsState()
    val autoSaveProgress by settingsViewModel.autoSaveProgress.collectAsState()
    val appLockEnabled by settingsViewModel.appLockEnabled.collectAsState()
    val cacheSize by settingsViewModel.cacheSize.collectAsState()
    
    val showThemeDialog by settingsViewModel.showThemeDialog.collectAsState()
    val showLanguageDialog by settingsViewModel.showLanguageDialog.collectAsState()
    val showFontSizeDialog by settingsViewModel.showFontSizeDialog.collectAsState()
    
    val appVersion = settingsViewModel.getAppVersion()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "সেটিংস",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            // Appearance
            SettingsSection(title = "অ্য্যাপিয়ারেন্স") {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "থিম",
                    subtitle = when(theme) {
                        "dark" -> "ডার্ক থিম"
                        "light" -> "লাইট থিম"
                        else -> "সিস্টেম ডিফল্ট"
                    },
                    onClick = {
                        settingsViewModel.openThemeDialog()
                    }
                )
                SettingsItem(
                    icon = Icons.Default.FormatSize,
                    title = "ফন্ট সাইজ",
                    subtitle = when(fontSize) {
                        "small" -> "ছোট"
                        "large" -> "বড়"
                        else -> "মাঝারি"
                    },
                    onClick = {
                        settingsViewModel.openFontSizeDialog()
                    }
                )
            }

            // Language
            SettingsSection(title = "ভাষা") {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "অ্যাপ ভাষা",
                    subtitle = when(language) {
                        "en" -> "ইংরেজি"
                        "ar" -> "আরবি"
                        "ur" -> "উর্দু"
                        else -> "বাংলা"
                    },
                    onClick = { 
                        settingsViewModel.openLanguageDialog() 
                    }
                )
            }

            // Notifications
            SettingsSection(title = "নোটিফিকেশন") {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "পুশ নোটিফিকেশন",
                    subtitle = "নোটিফিকেশন সেটিংস ম্যানেজ করুন",
                    onClick = onNotificationSettingsClick
                )
            }

            // Reading Config
            SettingsSection(title = "রিডিং সেটিংস") {
                SettingsToggleItem(
                    icon = Icons.Default.Save,
                    title = "অটো-সেভ প্রোগ্রেস",
                    subtitle = "আপনি যেখানে পড়া শেষ করেছেন তা মনে রাখুন",
                    isChecked = autoSaveProgress,
                    onCheckedChange = { settingsViewModel.toggleAutoSaveProgress(it) }
                )
            }

            // Privacy and Security
            SettingsSection(title = "প্রাইভেসি ও সিকিউরিটি") {
                SettingsToggleItem(
                    icon = Icons.Default.Fingerprint,
                    title = "অ্যাপ লক (বায়োমেট্রিক)",
                    subtitle = "ফিঙ্গারপ্রিন্ট দিয়ে অ্যাপ লক করুন",
                    isChecked = appLockEnabled,
                    onCheckedChange = { settingsViewModel.toggleAppLock(it) }
                )
            }

            // Storage
            SettingsSection(title = "স্টোরেজ") {
                SettingsItem(
                    icon = Icons.Default.CleaningServices,
                    title = "ক্যাশ ক্লিয়ার",
                    subtitle = "অপ্রয়োজনীয় ডেটা মুছুন ($cacheSize)",
                    onClick = { 
                        settingsViewModel.clearCache()
                        settingsViewModel.calculateCacheSize()
                    }
                )
            }

            // Backup and Restore
            SettingsSection(title = "ব্যাকআপ ও রিস্টোর") {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "ব্যাকআপ ডাটা",
                    subtitle = "সার্ভারে আপনার ডাটা সংরক্ষণ করুন",
                    onClick = onBackupClick
                )
                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = "রিস্টোর ডাটা",
                    subtitle = "আগের ডাটা ফিরিয়ে আনুন",
                    onClick = onRestoreClick
                )
            }

            // About
            SettingsSection(title = "সম্পর্কে") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "অ্যাপ ভার্সন",
                    subtitle = appVersion,
                    showArrow = false,
                    onClick = {}
                )
                SettingsItem(
                    icon = Icons.Default.ContactSupport,
                    title = "যোগাযোগ করুন",
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:support@muslimslibrary.org".toUri()
                            putExtra(Intent.EXTRA_SUBJECT, "Support: MuslimsLibrary App")
                        }
                        context.startActivity(Intent.createChooser(emailIntent, "Send email..."))
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Share,
                    title = "অ্যাপ শেয়ার করুন",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "মুসলিমস লাইব্রেরি অ্যাপ ডাউনলোড করুন: https://play.google.com/store/apps/details?id=${context.packageName}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                )
                SettingsItem(
                    icon = Icons.Default.StarRate,
                    title = "অ্যাপ রেট করুন",
                    onClick = {
                        try {
                            val uri = "market://details?id=${context.packageName}".toUri()
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val uri = "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                            val marketIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(marketIntent)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
        
        // Dialogs
        if (showThemeDialog) {
            ThemeSelectionDialog(
                selectedTheme = theme,
                onThemeSelected = { newTheme ->
                    settingsViewModel.updateTheme(newTheme)
                    settingsViewModel.closeThemeDialog()
                },
                onDismiss = { settingsViewModel.closeThemeDialog() }
            )
        }
        
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                selectedLanguage = language,
                onLanguageSelected = { newLang ->
                    settingsViewModel.updateLanguage(newLang)
                    settingsViewModel.closeLanguageDialog()
                    (context as? Activity)?.recreate()
                },
                onDismiss = { settingsViewModel.closeLanguageDialog() }
            )
        }
        
        if (showFontSizeDialog) {
            FontSizeDialog(
                selectedSize = fontSize,
                onSizeSelected = { newSize ->
                    settingsViewModel.updateFontSize(newSize)
                    settingsViewModel.closeFontSizeDialog()
                },
                onDismiss = { settingsViewModel.closeFontSizeDialog() }
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize()
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    showArrow: Boolean = true,
    titleColor: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor ?: MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
    Divider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        thickness = 1.dp
    )
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
    Divider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        thickness = 1.dp
    )
}
