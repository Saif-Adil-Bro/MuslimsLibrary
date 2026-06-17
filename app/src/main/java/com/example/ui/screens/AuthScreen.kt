package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val debugInfo by viewModel.debugInfo.collectAsState()
    val isDebugMode by viewModel.isDebugMode.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearToastMessage()
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Validation States
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validateInputs(): Boolean {
        var isValid = true
        if (email.isBlank()) {
            emailError = "Email address cannot be empty"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Please enter a valid email address"
            isValid = false
        } else {
            emailError = null
        }

        if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordError = null
        }
        return isValid
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            onAuthSuccess()
        } else if (uiState is AuthState.Error) {
            // Display auth / sync failures clearly as a visible popup snackbar on the screen
            snackbarHostState.showSnackbar(
                message = (uiState as AuthState.Error).message,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
        }
    }

    // Emerald to Teal breathtaking gradient for premium atmospheric feel
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF04261C), // Deep Islamic Pine Green
            Color(0xFF0A4E38), // Emerald Green accent
            Color(0xFF0E694C)  // Pure Mint Green
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Atmospheric Header Icon & Brand Title
            var brandTapCount by remember { mutableStateOf(0) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        brandTapCount++
                        if (brandTapCount >= 5) {
                            viewModel.toggleDebugMode(context)
                            brandTapCount = 0
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0A4E38).copy(alpha = 0.8f))
                        .border(1.5.dp, Color(0xFFC7F3E2).copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📖",
                        fontSize = 34.sp
                    )
                }

                Text(
                    text = "MuslimsLibrary",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5FBF7),
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Your Companion for Devotional Literature",
                    fontSize = 13.sp,
                    color = Color(0xFFC7F3E2).copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            // Central bone-white credential form card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFCFAF6) // Bone white
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // Segmented Mode Switcher (Login vs Sign Up)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF1EDE4))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isSignUp) Color(0xFF0A4E38) else Color.Transparent)
                                .clickable {
                                    isSignUp = false
                                    emailError = null
                                    passwordError = null
                                }
                                .testTag("switch_to_login_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                color = if (!isSignUp) Color.White else Color(0xFF0A4E38),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSignUp) Color(0xFF0A4E38) else Color.Transparent)
                                .clickable {
                                    isSignUp = true
                                    emailError = null
                                    passwordError = null
                                }
                                .testTag("switch_to_signup_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign Up",
                                color = if (isSignUp) Color.White else Color(0xFF0A4E38),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Email Input Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailError != null) emailError = null
                        },
                        label = { Text("Email Address") },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Email, 
                                contentDescription = "Email Icon",
                                tint = Color(0xFF0A4E38)
                            ) 
                        },
                        isError = emailError != null,
                        supportingText = emailError?.let { { Text(it) } },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1917),
                            unfocusedTextColor = Color(0xFF1C1917),
                            focusedLabelColor = Color(0xFF0A4E38),
                            unfocusedLabelColor = Color(0xFF57534E),
                            focusedBorderColor = Color(0xFF0A4E38),
                            unfocusedBorderColor = Color(0xFFD6D3D1),
                            cursorColor = Color(0xFF0A4E38),
                            errorBorderColor = Color.Red,
                            focusedLeadingIconColor = Color(0xFF0A4E38),
                            unfocusedLeadingIconColor = Color(0xFF78716C)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("email_input")
                    )

                    // Password Input Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (passwordError != null) passwordError = null
                        },
                        label = { Text("Password") },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Lock, 
                                contentDescription = "Password Icon",
                                tint = Color(0xFF0A4E38)
                            ) 
                        },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.LockOpen else Icons.Default.Lock
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle password visibility")
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = passwordError != null,
                        supportingText = passwordError?.let { { Text(it) } },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1917),
                            unfocusedTextColor = Color(0xFF1C1917),
                            focusedLabelColor = Color(0xFF0A4E38),
                            unfocusedLabelColor = Color(0xFF57534E),
                            focusedBorderColor = Color(0xFF0A4E38),
                            unfocusedBorderColor = Color(0xFFD6D3D1),
                            cursorColor = Color(0xFF0A4E38),
                            errorBorderColor = Color.Red,
                            focusedLeadingIconColor = Color(0xFF0A4E38),
                            unfocusedLeadingIconColor = Color(0xFF78716C)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("password_input")
                    )

                    // Unified loading indicator & Action trigger
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val stateValue = uiState
                        if (stateValue is AuthState.Loading) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF0A4E38),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stateValue.message,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0A4E38)
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (validateInputs()) {
                                        if (isSignUp) {
                                            viewModel.signUp(email, password)
                                        } else {
                                            viewModel.login(email, password)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0A4E38)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("submit_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (isSignUp) "Create Account" else "Sign In",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Divider line
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                        Text(
                            text = "OR",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                    }

                    // Modern "Continue with Google" Button
                    OutlinedButton(
                        onClick = {
                            if (activity != null) {
                                viewModel.signInWithGoogle(context, activity)
                            } else {
                                // Fallback safe mock login for preview environment
                                viewModel.login("developer@muslimslibrary.com", "developer123")
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF0A4E38)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFF0A4E38).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎨 ", // Simulated Google colored design logo
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Subtle "Continue as Guest" selection
                    TextButton(
                        onClick = {
                            viewModel.signInAnonymously()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .testTag("switch_auth_mode_button")
                    ) {
                        Text(
                            text = "Continue on Guest Bookshelf",
                            color = Color(0xFF0A4E38),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Snackbar style global auth feedback error
            if (uiState is AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFAEBEB)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = (uiState as AuthState.Error).message,
                            color = Color(0xFF8A1F1F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (isDebugMode && debugInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .padding(10.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    ) {
                        Text(
                            text = debugInfo,
                            color = Color(0xFFA3E2C9),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
