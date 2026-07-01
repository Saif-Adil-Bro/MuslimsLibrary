package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    appName: String = "MuslimsLibrary"
) {
    val isDark = isSystemInDarkTheme()

    // Colors matching PrivacyPolicyScreen
    val bgPrimary = if (isDark) Color(0xFF0F172A) else Color(0xFFF8F9FA)
    val bgSecondary = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val bgTertiary = if (isDark) Color(0xFF334155) else Color(0xFFF0F2F5)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1A202C)
    val textSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF4A5568)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF718096)
    val borderColor = if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0)
    
    val accentStart = Color(0xFF667EEA)
    val accentEnd = Color(0xFF764BA2)
    val accentLight = if (isDark) Color(0xFF667EEA).copy(alpha = 0.15f) else Color(0xFF667EEA).copy(alpha = 0.1f)
    
    val warningBg = if (isDark) Color(0xFF451A03) else Color(0xFFFFFBEB)
    val warningBorder = if (isDark) Color(0xFF92400E) else Color(0xFFFDE68A)
    val warningText = if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("সম্পর্কে", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgPrimary,
                    titleContentColor = textPrimary,
                    navigationIconContentColor = textPrimary
                )
            )
        },
        containerColor = bgPrimary
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Header Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(accentStart, accentEnd)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(vertical = 40.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📖",
                            fontSize = 64.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = appName,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "আপনার ইসলামিক জ্ঞানচর্চার সঙ্গী",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }
            }

            item {
                // About Section
                InfoCard(title = "📌 আমাদের সম্পর্কে", bgSecondary = bgSecondary, textSecondary = textSecondary, accentStart = accentStart, borderColor = borderColor) {
                    Text(
                        text = "$appName হলো একটি সম্পূর্ণ বিনামূল্যের ইসলামিক ডিজিটাল লাইব্রেরি অ্যাপ্লিকেশন। আমাদের লক্ষ্য হলো মুসলিম উম্মাহর কাছে সহজলভ্য ও নির্ভরযোগ্য ইসলামিক জ্ঞান পৌঁছে দেওয়া。\n\nএই অ্যাপে আপনি পাবেন কুরআন, হাদিস, ইসলামিক ইতিহাস, ফিকহ, আকিদা, সীরাত সহ বিভিন্ন বিষয়ের উপর নির্ভরযোগ্য বই ও রিসোর্স।",
                        fontSize = 15.sp,
                        color = textSecondary,
                        lineHeight = 24.sp
                    )
                }
            }

            item {
                // Mission Section
                InfoCard(title = "🎯 আমাদের লক্ষ্য", bgSecondary = bgSecondary, textSecondary = textSecondary, accentStart = accentStart, borderColor = borderColor) {
                    Text(
                        text = "আমাদের প্রধান লক্ষ্য হলো:",
                        fontSize = 15.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val missions = listOf(
                        "সহজ ও সাবলীল ভাষায় ইসলামিক জ্ঞান ছড়িয়ে দেওয়া",
                        "নির্ভরযোগ্য ও সহীহ উৎস থেকে জ্ঞান আহরণ করা",
                        "ডিজিটাল মাধ্যমে ইসলামিক শিক্ষা সবার কাছে পৌঁছানো",
                        "মুসলিম উম্মাহর জ্ঞানচর্চাকে উৎসাহিত করা",
                        "বিনামূল্যে ইসলামিক বই ও রিসোর্স প্রদান করা"
                    )
                    missions.forEach { mission ->
                        BulletPointItem(text = mission, bullet = "🎯", textSecondary = textSecondary)
                    }
                }
            }

            item {
                // Features Section
                InfoCard(title = "✨ বৈশিষ্ট্যসমূহ", bgSecondary = bgSecondary, textSecondary = textSecondary, accentStart = accentStart, borderColor = borderColor) {
                    val features = listOf(
                        "হাজার হাজার ইসলামিক বইয়ের সংগ্রহ",
                        "শক্তিশালী সার্চ ফিচার",
                        "অফলাইন পড়ার সুবিধা",
                        "বুকমার্ক ও নোট নেওয়ার সুবিধা",
                        "ডার্ক মোড সাপোর্ট",
                        "ইউজার ফ্রেন্ডলি ইন্টারফেস",
                        "ক্লাউড ব্যাকআপ ও সিঙ্ক",
                        "নিয়মিত নতুন বই যোগ করা হয়"
                    )
                    features.forEach { feature ->
                        BulletPointItem(text = feature, bullet = "✓", bulletColor = accentStart, textSecondary = textSecondary)
                    }
                }
            }

            item {
                // Version Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = bgTertiary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "অ্যাপ ভার্সন",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        Text(
                            text = "${com.example.BuildConfig.VERSION_NAME} (Latest)",
                            fontSize = 14.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item {
                // Contact Section
                InfoCard(title = " যোগাযোগ", bgSecondary = bgSecondary, textSecondary = textSecondary, accentStart = accentStart, borderColor = borderColor) {
                    Text(
                        text = "আপনার কোনো প্রশ্ন, পরামর্শ বা মতামত থাকলে আমাদের সাথে যোগাযোগ করতে পারেন:",
                        fontSize = 15.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ContactItem(icon = "📧", label = "ইমেইল:", value = AppConstants.CONTACT_EMAIL, textSecondary = textSecondary)
                    ContactItem(icon = "📞", label = "ফোন:", value = AppConstants.CONTACT_PHONE, textSecondary = textSecondary)
                    ContactItem(icon = "🌐", label = "ওয়েবসাইট:", value = AppConstants.WEBSITE_URL.removePrefix("https://"), textSecondary = textSecondary)
                    ContactItem(icon = "📱", label = "ফেসবুক:", value = AppConstants.FACEBOOK_HANDLE, textSecondary = textSecondary)
                }
            }

            item {
                // Disclaimer
                Card(
                    colors = CardDefaults.cardColors(containerColor = warningBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, warningBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚠️ সতর্কতা",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = warningText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "এই অ্যাপে প্রদত্ত সকল তথ্য শুধুমাত্র শিক্ষামূলক উদ্দেশ্যে দেওয়া হয়েছে। যেকোনো ধর্মীয় বিষয়ে বিস্তারিত জানতে রেজিস্টার্ড আলেম বা ইসলামিক স্কলারের পরামর্শ নেওয়া উচিত।",
                            fontSize = 14.sp,
                            color = warningText,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            item {
                // Footer
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "© $currentYear $appName. সর্বস্বত্ব সংরক্ষিত।",
                        fontSize = 12.sp,
                        color = textMuted,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .background(accentLight, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "ইসলামিক জ্ঞানচর্চায় আপনার সফলতা কামনা করছি।",
                            fontSize = 13.sp,
                            color = accentStart,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    bgSecondary: Color,
    textSecondary: Color,
    accentStart: Color,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgSecondary),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentStart,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun BulletPointItem(text: String, bullet: String, bulletColor: Color = Color.Unspecified, textSecondary: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = bullet,
            color = bulletColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 10.dp),
            fontSize = 15.sp
        )
        Text(
            text = text,
            fontSize = 15.sp,
            color = textSecondary,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun ContactItem(icon: String, label: String, value: String, textSecondary: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, modifier = Modifier.padding(end = 8.dp))
        Text(text = label, fontWeight = FontWeight.Bold, color = textSecondary, modifier = Modifier.padding(end = 4.dp))
        Text(text = value, color = textSecondary)
    }
}