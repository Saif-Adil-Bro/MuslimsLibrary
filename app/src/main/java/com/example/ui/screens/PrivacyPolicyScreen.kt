package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import com.example.R
import kotlinx.coroutines.launch
import java.util.Calendar

import com.example.util.AppConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val appName = stringResource(id = R.string.app_name)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isDark = isSystemInDarkTheme()

    // Colors matching HTML
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
    
    val successBg = if (isDark) Color(0xFF052E16) else Color(0xFFF0FDF4)
    val successBorder = if (isDark) Color(0xFF166534) else Color(0xFFBBF7D0)
    val successText = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534)

    val linkColor = if (isDark) Color(0xFF818CF8) else Color(0xFF667EEA)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("গোপনীয়তা নীতি", fontWeight = FontWeight.Bold) },
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
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Header Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(accentStart, accentEnd)))
                        .padding(vertical = 40.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🛡️",
                            fontSize = 64.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "গোপনীয়তা নীতি",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$appName অ্যাপে আপনার তথ্য কীভাবে সংরক্ষিত ও সুরক্ষিত রাখা হয়",
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "📅 সর্বশেষ আপডেট: ২৪ জুন, ২০২৬",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Table of Contents
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = bgSecondary),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📌 সূচিপত্র",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentStart
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val tocItems = listOf(
                            "১" to "ভূমিকা",
                            "২" to "সংগৃহীত তথ্য",
                            "৩" to "তথ্যের ব্যবহার",
                            "৪" to "থার্ড-পার্টি সার্ভিস",
                            "৫" to "ডাটা সিকিউরিটি",
                            "৬" to "আপনার অধিকার",
                            "৭" to "কুকি ও ট্র্যাকিং",
                            "৮" to "শিশুদের গোপনীয়তা",
                            "৯" to "নীতিমালা পরিবর্তন",
                            "১০" to "যোগাযোগ"
                        )
                        
                        // Let's lay them out in pairs if possible, or just vertically
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tocItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            coroutineScope.launch {
                                                // Scroll to item
                                                // Offset by 2 because 0=header, 1=toc, 2=section1 etc.
                                                listState.animateScrollToItem(index + 2)
                                            }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(accentLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.first,
                                            color = accentStart,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = item.second,
                                        color = textSecondary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 1
            item {
                PolicySection(
                    icon = "📖",
                    title = "১. ভূমিকা",
                    bgSecondary = bgSecondary,
                    borderColor = borderColor,
                    accentStart = accentStart,
                    accentEnd = accentEnd,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                ) {
                    PolicyText(text = "$appName অ্যাপে আপনাকে স্বাগতম। আপনার গোপনীয়তা রক্ষা করা আমাদের কাছে অত্যন্ত গুরুত্বপূর্ণ। এই গোপনীয়তা নীতিতে আমরা স্পষ্টভাবে ব্যাখ্যা করেছি যে, আপনি যখন আমাদের অ্যাপ ব্যবহার করেন, তখন আমরা কী ধরনের তথ্য সংগ্রহ করি, কীভাবে তা ব্যবহার করি, এবং কীভাবে তা সুরক্ষিত রাখি।", color = textSecondary)
                    Spacer(modifier = Modifier.height(14.dp))
                    PolicyText(text = "আমরা বিশ্বাস করি যে, একটি ইসলামিক জ্ঞানচর্চার প্ল্যাটফর্ম হিসেবে আমাদের দায়িত্ব শুধু নির্ভরযোগ্য কন্টেন্ট প্রদান করা নয়, বরং ব্যবহারকারীদের তথ্যের নিরাপত্তা নিশ্চিত করাও।", color = textSecondary)
                }
            }

            // Section 2
            item {
                PolicySection(
                    icon = "📊",
                    title = "২. আমরা কী কী তথ্য সংগ্রহ করি?",
                    bgSecondary = bgSecondary,
                    borderColor = borderColor,
                    accentStart = accentStart,
                    accentEnd = accentEnd,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                ) {
                    PolicyText(text = "আমরা আপনার সেবার মান উন্নত করতে নিম্নলিখিত তথ্যগুলো সংগ্রহ করতে পারি:", color = textSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FeatureList(
                        items = listOf(
                            "👤" to "অ্যাকাউন্ট তথ্য: নাম, ইমেইল অ্যাড্রেস (যখন আপনি রেজিস্ট্রেশন বা লগইন করেন)",
                            "📚" to "রিডিং ডাটা: কোন বই পড়ছেন, রিডিং প্রোগ্রেস, বুকমার্ক, ফেভারিট বই এবং নোট",
                            "📱" to "ডিভাইস তথ্য: অ্যান্ড্রয়েড ভার্সন, ডিভাইস মডেল, অ্যাপ ভার্সন",
                            "🔧" to "টেকনিক্যাল ডাটা: ক্র্যাশ রিপোর্ট, পারফরম্যান্স লগ (বাগ ফিক্স করার জন্য)",
                            "🔔" to "নোটিফিকেশন প্রেফারেন্স: কোন ধরনের নোটিফিকেশন পেতে চান"
                        ),
                        bgTertiary = bgTertiary,
                        accentStart = accentStart,
                        textSecondary = textSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    HighlightBox(
                        isSuccess = true,
                        title = "✅ গুরুত্বপূর্ণ নোট",
                        message = "আমরা কখনো আপনার ব্যক্তিগত মেসেজ, ফোন কল, বা অন্যান্য অ্যাপের ডাটা অ্যাক্সেস করি না। শুধুমাত্র অ্যাপের কার্যকারিতার জন্য প্রয়োজনীয় তথ্য সংগ্রহ করা হয়।",
                        bgColor = successBg,
                        borderColor = successBorder,
                        textColor = successText
                    )
                }
            }

            // Section 3
            item {
                PolicySection(
                    icon = "🎯",
                    title = "৩. তথ্য কীভাবে ব্যবহার করা হয়?",
                    bgSecondary = bgSecondary,
                    borderColor = borderColor,
                    accentStart = accentStart,
                    accentEnd = accentEnd,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                ) {
                    PolicyText(text = "আপনার সংগৃহীত তথ্য মূলত নিম্নলিখিত উদ্দেশ্যে ব্যবহার করা হয়:", color = textSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FeatureList(
                        items = listOf(
                            "✓" to "আপনার অ্যাকাউন্ট তৈরি ও লগইন প্রক্রিয়া সম্পন্ন করতে",
                            "✓" to "রিডিং প্রোগ্রেস, নোট এবং বুকমার্ক ক্লাউডে ব্যাকআপ ও সিঙ্ক করতে",
                            "✓" to "অ্যাপের পারফরম্যান্স উন্নত করা এবং নতুন ফিচার যোগ করতে",
                            "✓" to "প্রাসঙ্গিক নোটিফিকেশন (নতুন বই আপডেট, ফোরাম রিপ্লাই) পাঠাতে",
                            "✓" to "ব্যবহারকারীর অভিজ্ঞতা উন্নত করতে অ্যানালিটিক্স বিশ্লেষণ করতে",
                            "✓" to "কারিগরি সমস্যা সমাধান ও কাস্টমার সাপোর্ট প্রদান করতে"
                        ),
                        bgTertiary = bgTertiary,
                        accentStart = accentStart,
                        textSecondary = textSecondary
                    )
                }
            }

            // Section 4
            item {
                PolicySection(
                    icon = "🌐",
                    title = "৪. থার্ড-পার্টি সার্ভিস",
                    bgSecondary = bgSecondary,
                    borderColor = borderColor,
                    accentStart = accentStart,
                    accentEnd = accentEnd,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                ) {
                    PolicyText(text = "আমরা আমাদের অ্যাপের ব্যাকএন্ড এবং অ্যানালিটিক্সের জন্য কিছু বিশ্বস্ত থার্ড-পার্টি সার্ভিস ব্যবহার করি:", color = textSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FeatureList(
                        items = listOf(
                            "☁️" to "Supabase: ডাটাবেস এবং ক্লাউড স্টোরেজের জন্য",
                            "🔥" to "Firebase: অথেনটিকেশন, ক্লাউড মেসেজিং (নোটিফিকেশন) এবং অ্যানালিটিক্সের জন্য"
                        ),
                        bgTertiary = bgTertiary,
                        accentStart = accentStart,
                        textSecondary = textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PolicyText(text = "এই সেবা প্রদানকারীরাও তাদের নিজস্ব গোপনীয়তা নীতি অনুযায়ী আপনার তথ্য প্রক্রিয়া করতে পারে। আমরা শুধুমাত্র সেই সার্ভিসগুলো ব্যবহার করি যাদের নিরাপত্তা মান উচ্চ।", color = textSecondary)
                }
            }

            // Section 5
            item {
                PolicySection(
                    icon = "🛡️",
                    title = "৫. ডাটা সিকিউরিটি",
                    bgSecondary = bgSecondary,
                    borderColor = borderColor,
                    accentStart = accentStart,
                    accentEnd = accentEnd,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                ) {
                    PolicyText(text = "আমরা আপনার ব্যক্তিগত তথ্য সুরক্ষিত রাখতে শিল্প-মানের নিরাপত্তা প্রযুক্তি ব্যবহার করি:", color = textSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FeatureList(
                        items = listOf(
                            "🔐" to "এন্ড-টু-এন্ড এনক্রিপশন",
                            "🔒" to "নিরাপদ সার্ভার স্টোরেজ",
                            "🛡️" to "নিয়মিত সিকিউরিটি অডিট",
                            "👥" to "সীমিত অ্যাক্সেস কন্ট্রোল"
                        ),
                        bgTertiary = bgTertiary,
                        accentStart = accentStart,
                        textSecondary = textSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    HighlightBox(
                        isSuccess = false,
                        title = "⚠️ সতর্কতা",
                        message = "তবে ইন্টারনেটের মাধ্যমে কোনো তথ্য প্রেরণ বা ইলেকট্রনিক স্টোরেজ ১০০% নিরাপদ নয়, তাই আমরা সম্পূর্ণ নিরাপত্তার নিশ্চয়তা দিতে পারি না। আমরা সর্বোচ্চ চেষ্টা করি আপনার তথ্য সুরক্ষিত রাখতে।",
                        bgColor = warningBg,
                        borderColor = warningBorder,
                        textColor = warningText
                    )
                }
            }

            // Section 6
            item {
                PolicySection(
                    icon = "⚖️",
                    title = "৬. আপনার অধিকার",
                    bgSecondary = bgSecondary,
                    borderColor = borderColor,
                    accentStart = accentStart,
                    accentEnd = accentEnd,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                ) {
                    PolicyText(text = "যেকোনো সময় আপনি নিম্নলিখিত অধিকারগুলো প্রয়োগ করতে পারেন:", color = textSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FeatureList(
                        items = listOf(
                            "📤" to "আপনার সব ডাটা এক্সপোর্ট করতে পারেন",
                            "🗑️" to "আপনার অ্যাকাউন্ট এবং সব ডাটা স্থায়ীভাবে ডিলিট করতে পারেন",
                            "✏️" to "আপনার প্রোফাইল তথ্য আপডেট করতে পারেন",
                            "🔔" to "নোটিফিকেশন প্রেফারেন্স পরিবর্তন করতে পারেন",
                            "📋" to "আমাদের গোপনীয়তা নীতির কপি পেতে পারেন"
                        ),
                        bgTertiary = bgTertiary,
                        accentStart = accentStart,
                        textSecondary = textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PolicyText(text = "অ্যাকাউন্ট ডিলিট করলে আমাদের সার্ভার থেকে আপনার সব ব্যক্তিগত তথ্য স্থায়ীভাবে মুছে ফেলা হবে।", color = textSecondary)
                }
            }

            // Section 7
            item {
                PolicySection(
                    icon = "🍪",
                    title = "৭. কুকি ও ট্র্যাকিং",
                    bgSecondary = bgSecondary,
                    borderColor = borderColor,
                    accentStart = accentStart,
                    accentEnd = accentEnd,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                ) {
                    PolicyText(text = "আমরা অ্যাপের কার্যকারিতা উন্নত করতে কিছু লোকাল স্টোরেজ (SharedPreferences) ব্যবহার করি, যা কুকির মতো কাজ করে। এগুলো ব্যবহার করে:", color = textSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FeatureList(
                        items = listOf(
                            "🔑" to "আপনার লগইন সেশন মনে রাখা",
                            "⚙️" to "আপনার সেটিংস (থিম, ভাষা) সংরক্ষণ করা",
                            "📊" to "অ্যানালিটিক্স ডাটা (অ্যানোনিমাস)"
                        ),
                        bgTertiary = bgTertiary,
                        accentStart = accentStart,
                        textSecondary = textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PolicyText(text = "আমরা কোনো থার্ড-পার্টি অ্যাড নেটওয়ার্ক বা ট্র্যাকিং কুকি ব্যবহার করি না।", color = textSecondary)
                }
            }

            // Section 8
            item {
                PolicySection(
                    icon = "👶",
                    title = "৮. শিশুদের গোপনীয়তা",
                    bgSecondary = bgSecondary,
                    borderColor = borderColor,
                    accentStart = accentStart,
                    accentEnd = accentEnd,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                ) {
                    PolicyText(text = "আমাদের অ্যাপটি সব বয়সের মানুষের জন্য উন্মুক্ত এবং ইসলামিক জ্ঞানচর্চার জন্য উপযোগী। তবে আমরা জেনেশুনে ১৩ বছরের নিচে কোনো শিশুর ব্যক্তিগত তথ্য সংগ্রহ করি না।", color = textSecondary)
                    Spacer(modifier = Modifier.height(14.dp))
                    PolicyText(text = "যদি আমরা জানতে পারি যে কোনো শিশুর তথ্য সংগৃহীত হয়েছে, তাহলে আমরা তা দ্রুত মুছে ফেলব। অভিভাবকরা চাইলে তাদের সন্তানের অ্যাকাউন্ট ডিলিট করার অনুরোধ করতে পারেন।", color = textSecondary)
                }
            }

            // Section 9
            item {
                PolicySection(
                    icon = "📝",
                    title = "৯. নীতিমালা পরিবর্তন",
                    bgSecondary = bgSecondary,
                    borderColor = borderColor,
                    accentStart = accentStart,
                    accentEnd = accentEnd,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                ) {
                    PolicyText(text = "আমরা মাঝে মাঝে এই গোপনীয়তা নীতি আপডেট করতে পারি। কোনো বড় পরিবর্তন হলে আমরা:", color = textSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FeatureList(
                        items = listOf(
                            "🔔" to "অ্যাপের মাধ্যমে নোটিফিকেশন পাঠাব",
                            "📧" to "রেজিস্টার্ড ইউজারদের ইমেইলে জানাব",
                            "📅" to "'সর্বশেষ আপডেট' তারিখ পরিবর্তন করব"
                        ),
                        bgTertiary = bgTertiary,
                        accentStart = accentStart,
                        textSecondary = textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PolicyText(text = "আমরা আপনাকে পরামর্শ দিচ্ছি যে, মাঝে মাঝে এই পৃষ্ঠাটি পরীক্ষা করুন যাতে যেকোনো পরিবর্তন সম্পর্কে অবগত থাকেন।", color = textSecondary)
                }
            }

            // Section 10
            item {
                PolicySection(
                    icon = "📞",
                    title = "১০. যোগাযোগ",
                    bgSecondary = bgSecondary,
                    borderColor = borderColor,
                    accentStart = accentStart,
                    accentEnd = accentEnd,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                ) {
                    PolicyText(text = "এই গোপনীয়তা নীতি সম্পর্কে আপনার কোনো প্রশ্ন, উদ্বেগ বা মতামত থাকলে আমাদের সাথে যোগাযোগ করুন। আমরা ২৪-৪৮ ঘণ্টার মধ্যে উত্তর দেওয়ার চেষ্টা করি।", color = textSecondary)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Contact Cards
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ContactCard(
                            icon = "📞",
                            title = "ফোন",
                            linkText = AppConstants.CONTACT_PHONE,
                            bgTertiary = bgTertiary,
                            borderColor = borderColor,
                            textPrimary = textPrimary,
                            linkColor = linkColor,
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${AppConstants.CONTACT_PHONE}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle exception
                                }
                            }
                        )
                        ContactCard(
                            icon = "📧",
                            title = "ইমেইল",
                            linkText = AppConstants.CONTACT_EMAIL,
                            bgTertiary = bgTertiary,
                            borderColor = borderColor,
                            textPrimary = textPrimary,
                            linkColor = linkColor,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${AppConstants.CONTACT_EMAIL}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle exception
                                }
                            }
                        )
                        ContactCard(
                            icon = "🌐",
                            title = "ওয়েবসাইট",
                            linkText = AppConstants.WEBSITE_URL.removePrefix("https://"),
                            bgTertiary = bgTertiary,
                            borderColor = borderColor,
                            textPrimary = textPrimary,
                            linkColor = linkColor,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.WEBSITE_URL))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle exception
                                }
                            }
                        )
                        ContactCard(
                            icon = "📘",
                            title = "ফেসবুক",
                            linkText = AppConstants.FACEBOOK_HANDLE,
                            bgTertiary = bgTertiary,
                            borderColor = borderColor,
                            textPrimary = textPrimary,
                            linkColor = linkColor,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.FACEBOOK_PAGE_URL))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle exception
                                }
                            }
                        )
                    }
                }
            }

            // Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📖",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "$appName - আপনার ইসলামিক জ্ঞানচর্চার সঙ্গী",
                        color = textMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "© $currentYear $appName. সর্বস্বত্ব সংরক্ষিত।",
                        color = textMuted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentLight)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "\"ইসলামিক জ্ঞানচর্চায় আপনার সফলতা কামনা করছি।\"",
                            color = accentStart,
                            fontSize = 15.sp,
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
fun PolicySection(
    icon: String,
    title: String,
    bgSecondary: Color,
    borderColor: Color,
    accentStart: Color,
    accentEnd: Color,
    textPrimary: Color,
    textSecondary: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgSecondary),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(accentStart, accentEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 16.dp))
            content()
        }
    }
}

@Composable
fun PolicyText(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 15.sp,
        lineHeight = 24.sp
    )
}

@Composable
fun FeatureList(
    items: List<Pair<String, String>>,
    bgTertiary: Color,
    accentStart: Color,
    textSecondary: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgTertiary)
                    .border(BorderStroke(1.dp, bgTertiary), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                // To mimic border-left: 3px solid accentStart, we can add a small box on the left,
                // but setting border color only on the left is complex, so let's use a Box inside Row
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .background(accentStart)
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.first,
                    color = accentStart,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                // Parse text if it contains colon to make first part bold
                val textParts = item.second.split(":", limit = 2)
                if (textParts.size == 2) {
                    Text(
                        text = androidx.compose.ui.text.buildAnnotatedString {
                            withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(textParts[0] + ":")
                            }
                            append(textParts[1])
                        },
                        color = textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                } else {
                    Text(
                        text = item.second,
                        color = textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}

@Composable
fun HighlightBox(
    isSuccess: Boolean,
    title: String,
    message: String,
    bgColor: Color,
    borderColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(borderColor)
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = message,
                color = textColor,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ContactCard(
    icon: String,
    title: String,
    linkText: String,
    bgTertiary: Color,
    borderColor: Color,
    textPrimary: Color,
    linkColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgTertiary),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 32.sp, modifier = Modifier.padding(bottom = 10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = linkText,
                color = linkColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}
