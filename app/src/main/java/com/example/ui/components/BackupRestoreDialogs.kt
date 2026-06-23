package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.BackupUiState
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.AuthState

@Composable
fun BackupRestoreDialogs(
    profileViewModel: ProfileViewModel,
    userEmail: String
) {
    val backupStatus by profileViewModel.backupStatus.collectAsState()

    when (val status = backupStatus) {
        is BackupUiState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { profileViewModel.resetBackupStatus() }) {
                        Text("বাতিল করুন", color = MaterialTheme.colorScheme.primary)
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text(
                            "অপেক্ষা করুন...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Text("আপনার ডাটা ক্লাউডে অত্যন্ত নিরাপদে রিস্টোর করা হচ্ছে। অনুগ্রহ করে অ্যাপ বন্ধ করবেন না।")
                }
            )
        }
        is BackupUiState.Success -> {
            AlertDialog(
                onDismissRequest = { profileViewModel.resetBackupStatus() },
                confirmButton = {
                    Button(
                        onClick = { profileViewModel.resetBackupStatus() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("ঠিক আছে", color = Color.White)
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            "সফল হয়েছে!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                },
                text = {
                    Text("অভিনন্দন! আপনার ব্যাকআপ/রিস্টোর প্রক্রিয়াটি সফলভাবে সম্পন্ন হয়েছে।")
                }
            )
        }
        is BackupUiState.Error -> {
            val errorMessage = status.message
            AlertDialog(
                onDismissRequest = { profileViewModel.resetBackupStatus() },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { profileViewModel.resetBackupStatus() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("বন্ধ করুন", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                profileViewModel.performRestore(userEmail)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("পুনরায় চেষ্টা করুন", color = Color.White)
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            "ত্রুটি ঘটেছে",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Red
                        )
                    }
                },
                text = {
                    Text(errorMessage)
                }
            )
        }
        else -> {}
    }
}

@Composable
fun AuthRestoringDialog(authState: AuthState) {
    if (authState is AuthState.Restoring) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        "ডাটা রিস্টোর হচ্ছে...",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            },
            text = { Text("আপনার ডাটা ক্লাউড থেকে ডাউনলোড এবং রিস্টোর করা হচ্ছে। অনুগ্রহ করে একটু অপেক্ষা করুন...") }
        )
    }
}
