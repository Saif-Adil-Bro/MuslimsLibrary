package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseBook
import com.example.ui.theme.*
import com.example.ui.viewmodel.Author

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorDetailBottomSheet(
    author: Author?,
    onDismissRequest: () -> Unit,
    onBookClick: (SupabaseBook) -> Unit,
    modifier: Modifier = Modifier
) {
    if (author == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = null,
        modifier = modifier.testTag("author_detail_bottom_sheet_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header: Purple gradient background (#667EEA → #764BA2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppGradientStart, AppGradientEnd)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 25.dp)
            ) {
                // Close button (top right)
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(35.dp)
                        .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
                        .testTag("dismiss_author_bottom_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Avatar, Title & Book count in Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    // Circular avatar (70dp)
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = author.initial,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppGradientStart
                        )
                    }

                    // Authors names info
                    Column {
                        Text(
                            text = author.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "${author.booksCount}টি বই রয়েছে",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Body content area
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                // Section 1: পরিচিতি (Bio)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "পরিচিতি",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Divider(color = Color(0xFFF0F2F5), thickness = 2.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = author.bio ?: "কোনো পরিচিতি নেই",
                            fontSize = 14.sp,
                            color = Color(0xFF4A5568),
                            lineHeight = 22.sp
                        )
                    }
                }

                // Section 2: বইসমূহ (Books list)
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "বইসমূহ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Divider(color = Color(0xFFF0F2F5), thickness = 2.dp)
                }

                // Sub-list of books
                items(author.books) { book ->
                    BookListItem(
                        book = book,
                        onClick = {
                            onDismissRequest()
                            onBookClick(book)
                        }
                    )
                }

                // Bottom spacer safety
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
