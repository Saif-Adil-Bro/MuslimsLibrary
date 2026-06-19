package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryStatsCard(
    stats: LibraryViewModel.LibraryStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("library_stats_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "আমার লাইব্রেরী",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(count = stats.totalBooks, label = "মোট বই", modifier = Modifier.weight(1f))
                    StatItem(count = stats.completedBooks, label = "সম্পূর্ণ", modifier = Modifier.weight(1f))
                    StatItem(count = stats.readingBooks, label = "চলমান", modifier = Modifier.weight(1f))
                    StatItem(count = stats.favoriteBooks, label = "ফেভারিট", modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LibraryTabFilter(
    selectedTab: LibraryViewModel.LibraryTab,
    onTabSelected: (LibraryViewModel.LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        LibraryViewModel.LibraryTab.ALL to "সব",
        LibraryViewModel.LibraryTab.DOWNLOADS to "ডাউনলোড",
        LibraryViewModel.LibraryTab.FAVORITES to "ফেভারিট",
        LibraryViewModel.LibraryTab.READING to "চলমান",
        LibraryViewModel.LibraryTab.COMPLETED to "সম্পূর্ণ",
        LibraryViewModel.LibraryTab.PINNED to "পিন করা"
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { (tab, label) ->
            val isSelected = selectedTab == tab
            Card(
                onClick = { onTabSelected(tab) },
                shape = RoundedCornerShape(50.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF667EEA) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.testTag("tab_pill_${tab.name.lowercase()}")
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color(0xFF4A5568),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun LibraryBookCard(
    book: LibraryViewModel.LibraryBook,
    onClick: () -> Unit,
    onMenuClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val translationY by animateDpAsState(
        targetValue = if (isPressed) 0.dp else (-2).dp,
        animationSpec = spring(dampingRatio = 0.8f)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = translationY)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .testTag("library_book_grid_${book.bookId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 2.dp else 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFFEDF2F7))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(book.coverUrl)
                        .placeholder(com.example.R.drawable.ic_book_placeholder)
                        .error(com.example.R.drawable.ic_book_placeholder)
                        .crossfade(true)
                        .build(),
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (book.isPinned) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF3182CE))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        if (book.isDownloaded) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF805AD5))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Downloaded",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        if (book.isFavorite) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE53E3E))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Favorite",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                .testTag("book_menu_button_${book.bookId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color(0xFF2D3748),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("পড়ুন", fontSize = 14.sp) },
                                onClick = {
                                    showMenu = false
                                    onMenuClick("read")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (book.isFavorite) "ফেভারিট থেকে সরান" else "ফেভারিটে যোগ করুন",
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onMenuClick("favorite")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (book.isPinned) "পিন থেকে সরান" else "পিন করুন",
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onMenuClick("pin")
                                }
                            )
                            if (book.isDownloaded) {
                                DropdownMenuItem(
                                    text = { Text("ডাউনলোড মুছুন", fontSize = 14.sp, color = Color.Red) },
                                    onClick = {
                                        showMenu = false
                                        onMenuClick("delete_download")
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF2D3748),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier.heightIn(min = 32.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.author,
                    fontSize = 11.sp,
                    color = Color(0xFF718096),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { book.progress / 100f },
                        color = Color(0xFF667EEA),
                        trackColor = Color(0xFFE2E8F0),
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "${book.progress}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A5568)
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryBookListItem(
    book: LibraryViewModel.LibraryBook,
    onClick: () -> Unit,
    onMenuClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("library_book_list_${book.bookId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEDF2F7))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(book.coverUrl)
                        .placeholder(com.example.R.drawable.ic_book_placeholder)
                        .error(com.example.R.drawable.ic_book_placeholder)
                        .crossfade(true)
                        .build(),
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF2D3748),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.author,
                    fontSize = 11.sp,
                    color = Color(0xFF718096),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (book.isPinned) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEBF8FF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = Color(0xFF3182CE),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                    if (book.isDownloaded) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFAF5FF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF805AD5),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                    if (book.isFavorite) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFF5F5))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE53E3E),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { book.progress / 100f },
                        color = Color(0xFF667EEA),
                        trackColor = Color(0xFFE2E8F0),
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "${book.progress}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A5568)
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("book_list_menu_button_${book.bookId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFF718096)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("পড়ুন", fontSize = 14.sp) },
                        onClick = {
                            showMenu = false
                            onMenuClick("read")
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (book.isFavorite) "ফেভারিট থেকে সরান" else "ফেভারিটে যোগ করুন",
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            showMenu = false
                            onMenuClick("favorite")
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (book.isPinned) "পিন থেকে সরান" else "পিন করুন",
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            showMenu = false
                            onMenuClick("pin")
                        }
                    )
                    if (book.isDownloaded) {
                        DropdownMenuItem(
                            text = { Text("ডাউনলোড মুছুন", fontSize = 14.sp, color = Color.Red) },
                            onClick = {
                                showMenu = false
                                onMenuClick("delete_download")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryEmptyState(
    message: String = "আপনার লাইব্রেরী খালি",
    buttonText: String = "বই ডাউনলোড বা ফেভারিট করুন",
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = Color(0xFFCBD5E0)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color(0xFF2D3748),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "পড়া শুরু করতে হোম স্ক্রিনে যান এবং আপনার পছন্দের বই যুক্ত করুন!",
            color = Color(0xFF718096),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onButtonClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = buttonText, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
