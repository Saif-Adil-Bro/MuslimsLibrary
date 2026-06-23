package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FontSizeDialog(
    selectedSize: String,
    onSizeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sizes = listOf(
        Triple("small", "ছোট", 12.sp),
        Triple("medium", "মাঝারি", 16.sp),
        Triple("large", "বড়", 20.sp)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ফন্ট সাইজ নির্বাচন করুন") },
        text = {
            Column {
                sizes.forEach { size ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSizeSelected(size.first) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = size.second,
                            fontSize = size.third,
                            color = if (size.first == selectedSize) MaterialTheme.colorScheme.primary else Color.Unspecified
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = size.first == selectedSize,
                            onClick = { onSizeSelected(size.first) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                
                // Slider for fine control
                Spacer(modifier = Modifier.height(16.dp))
                Text("স্লাইডার দিয়ে সামঞ্জস্য করুন:", fontSize = 14.sp)
                Slider(
                    value = when (selectedSize) {
                        "small" -> 0f
                        "medium" -> 1f
                        "large" -> 2f
                        else -> 1f
                    },
                    onValueChange = { value ->
                        val newSize = when {
                            value < 0.5f -> "small"
                            value < 1.5f -> "medium"
                            else -> "large"
                        }
                        onSizeSelected(newSize)
                    },
                    steps = 1,
                    valueRange = 0f..2f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
