package com.example.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.edit
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed class AuthState {
    object Idle : AuthState()
    data class Loading(val message: String = "Loading...") : AuthState()
    object Restoring : AuthState()
    data class Success(val email: String, val uid: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val backupManager: com.example.data.backup.BackupManager
) : ViewModel() {

    private var isLoginFlowActive = false

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userRole = MutableStateFlow("user")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _debugInfo = MutableStateFlow("")
    val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()

    private val _isDebugMode = MutableStateFlow(false)
    val isDebugMode: StateFlow<Boolean> = _isDebugMode.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isInitialCheckDone = MutableStateFlow(false)
    val isInitialCheckDone: StateFlow<Boolean> = _isInitialCheckDone.asStateFlow()

    private val _isAutoGuestLogin = MutableStateFlow(false)
    val isAutoGuestLogin: StateFlow<Boolean> = _isAutoGuestLogin.asStateFlow()

    private val _isFromGuestMode = MutableStateFlow(false)
    val isFromGuestMode: StateFlow<Boolean> = _isFromGuestMode.asStateFlow()

    fun goToLoginPage() {
        _isLoggedIn.value = false
        _uiState.value = AuthState.Idle
        _isInitialCheckDone.value = true
    }

    fun setFromGuestMode(fromGuest: Boolean) {
        _isFromGuestMode.value = fromGuest
        if (_uiState.value !is AuthState.Success) {
            _uiState.value = AuthState.Idle
        }
    }

    fun initDebugMode(context: Context) {
        val prefs = context.getSharedPreferences("app_debug_prefs", Context.MODE_PRIVATE)
        _isDebugMode.value = prefs.getBoolean("debug_mode_enabled", false)
    }

    fun toggleDebugMode(context: Context) {
        val prefs = context.getSharedPreferences("app_debug_prefs", Context.MODE_PRIVATE)
        val newValue = !prefs.getBoolean("debug_mode_enabled", false)
        prefs.edit {putBoolean("debug_mode_enabled", newValue)}
        _isDebugMode.value = newValue
        _toastMessage.value = if (newValue) "🛠️ Debug Mode Enabled!" else "🛠️ Debug Mode Disabled."
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    private fun updateDebugConsole(queryUid: String, role: String, finalRole: String) {
        val firebaseUid = authRepository.getCurrentUserUid() ?: "None"
        val supabaseUid = authRepository.getSupabaseUid() ?: "None"
        val queryDetails = authRepository.getLastQueryJson()
        
        _debugInfo.value = """
            Firebase UID: $firebaseUid
            Supabase UID: $supabaseUid
            Query UID: $queryUid
            Parsed Role: $role
            Final Role: $finalRole
            
            Database Response / Query Details:
            $queryDetails
        """.trimIndent()
    }

    private fun updateDebugConsoleError(queryUid: String, errorMsg: String) {
        val firebaseUid = authRepository.getCurrentUserUid() ?: "None"
        val supabaseUid = authRepository.getSupabaseUid() ?: "None"
        val queryDetails = authRepository.getLastQueryJson()
        
        _debugInfo.value = """
            Firebase UID: $firebaseUid
            Supabase UID: $supabaseUid
            Query UID: $queryUid
            Parsed Role: Error
            Final Role: user
            Error: $errorMsg
            
            Database Response / Query Details:
            $queryDetails
        """.trimIndent()
    }

    init {
        viewModelScope.launch {
            authRepository.isUserLoggedIn().collectLatest { loggedIn ->
                val email = authRepository.getCurrentUserEmail()
                if (loggedIn && email != null) {
                    _isAutoGuestLogin.value = false
                    val uid = authRepository.getCurrentUserUid()
                    if (uid != null) {
                        try {
                            authRepository.ensureProfileSynced()
                        } catch (e: Exception) {
                            android.util.Log.e("AuthViewModel", "Init profile sync error", e)
                        }
                        _debugInfo.value = "Fetching role for UID: $uid..."
                        if (_isDebugMode.value) {
                            _toastMessage.value = "Fetching Role..."
                        }
                        try {
                            val role = withTimeout(15_000) {
                                authRepository.getUserRole(uid)
                            }
                            _userRole.value = role
                            android.util.Log.d("AuthViewModel", "Init role fetched: $role")
                            updateDebugConsole(queryUid = uid, role = role, finalRole = role)
                            if (_isDebugMode.value) {
                                _toastMessage.value = "Role loaded: $role\nLogs on-screen."
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AuthViewModel", "Init role fetch failed", e)
                            _userRole.value = "user"
                            updateDebugConsoleError(queryUid = uid, errorMsg = e.message ?: "Unknown error")
                            if (_isDebugMode.value) {
                                _toastMessage.value = "Role error: ${e.message}"
                            }
                        }
                    } else {
                        _userRole.value = "user"
                        _debugInfo.value = "UID is null"
                        if (_isDebugMode.value) {
                            _toastMessage.value = "UID is null"
                        }
                    }
                    if (!isLoginFlowActive) {
                        _uiState.value = AuthState.Success(email, uid ?: "")
                    }
                    _isLoggedIn.value = true
                    _isInitialCheckDone.value = true
                } else if (!_isInitialCheckDone.value) {
                    _isAutoGuestLogin.value = true
                    try {
                        authRepository.signInAnonymously()
                            .onSuccess {
                                android.util.Log.d("AuthViewModel", "Auto guest login successful")
                                val uid = authRepository.getCurrentUserUid()
                                if (uid != null) {
                                    _userRole.value = "user"
                                    _uiState.value = AuthState.Success(
                                        authRepository.getCurrentUserEmail() ?: "Guest User",
                                        uid
                                    )
                                    _isLoggedIn.value = true
                                }
                                _isAutoGuestLogin.value = false
                                _isInitialCheckDone.value = true
                            }
                            .onFailure { error ->
                                android.util.Log.e("AuthViewModel", "Auto guest login failed", error)
                                _isAutoGuestLogin.value = false
                                _isInitialCheckDone.value = true
                                _uiState.value = AuthState.Idle
                                _isLoggedIn.value = false
                            }
                    } catch (e: Exception) {
                        _isAutoGuestLogin.value = false
                        _isInitialCheckDone.value = true
                    }
                } else {
                    _uiState.value = AuthState.Idle
                    _userRole.value = "user"
                    _isLoggedIn.value = false
                    _debugInfo.value = "Not logged in"
                }
            }
        }
    }

    private fun fetchUserRole(uid: String) {
        viewModelScope.launch {
            _debugInfo.value = "Fetching role for UID: $uid..."
            if (_isDebugMode.value) {
                _toastMessage.value = "Fetching Role..."
            }
            try {
                val role = withTimeout(15_000) {
                    authRepository.getUserRole(uid)
                }
                _userRole.value = role
                android.util.Log.d("AuthViewModel", "fetchUserRole fetched: $role")
                updateDebugConsole(queryUid = uid, role = role, finalRole = role)
                if (_isDebugMode.value) {
                    _toastMessage.value = "Role loaded: $role"
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "fetchUserRole failed", e)
                _userRole.value = "user"
                updateDebugConsoleError(queryUid = uid, errorMsg = e.message ?: "Unknown error")
                if (_isDebugMode.value) {
                    _toastMessage.value = "Role check failed: ${e.message}"
                }
            }
        }
    }

    private val _shouldAutoRestore = MutableStateFlow(false)
    val shouldAutoRestore: StateFlow<Boolean> = _shouldAutoRestore.asStateFlow()

    fun onAutoRestoreHandled() {
        _shouldAutoRestore.value = false
    }

    private suspend fun onAuthenticationSuccess(email: String, context: Context? = null) {
        val uid = authRepository.getCurrentUserUid()
        android.util.Log.d("AuthViewModel", "Login success, UID: $uid")
        if (uid != null) {
            context?.let {
                it.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit {
                        putString("user_id", uid)
                    }
            }
            try {
                authRepository.ensureProfileSynced()
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Login profile sync error", e)
            }
            _debugInfo.value = "Fetching role for UID: $uid..."
            if (_isDebugMode.value) {
                _toastMessage.value = "Fetching Role..."
            }
            try {
                // Add timeout to prevent hanging
                val role = withTimeout(15_000) {
                    authRepository.getUserRole(uid)
                }
                _userRole.value = role
                android.util.Log.d("AuthViewModel", "Role fetched: $role")
                updateDebugConsole(queryUid = uid, role = role, finalRole = role)
                if (_isDebugMode.value) {
                    _toastMessage.value = "Role loaded: $role"
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error fetching user role", e)
                // If role fetch fails, default to 'user' but still allow login
                _userRole.value = "user"
                _toastMessage.value = "Login successful (using default role)"
                updateDebugConsoleError(queryUid = uid, errorMsg = e.message ?: "Unknown error")
            }

            // Prepare for BackupManager clearing loose local data
            try {
                if (email != "User@muslimslibrary.org" && email != "Guest User") {
                    // Always clear previous loose local data on fresh login to prevent mixing
                    backupManager.clearLocalData()
                    _shouldAutoRestore.value = true
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error preparing backup manager", e)
            }

        } else {
            _userRole.value = "user"
            _debugInfo.value = "UID is null"
            if (_isDebugMode.value) {
                _toastMessage.value = "UID is null"
            }
        }
        isLoginFlowActive = false
        _uiState.value = AuthState.Success(email, uid ?: "")
        _isLoggedIn.value = true
    }

    private fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return true // assume true if not supplied to prevent blocking
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            @Suppress("DEPRECATION")
            val activeNetwork = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            activeNetwork != null && activeNetwork.isConnected
        } catch (e: Exception) {
            true // fallback to true to prevent locking users out if permission checks etc. fail
        }
    }

    fun login(email: String, passwordState: String, context: Context? = null) {
        android.util.Log.d("AuthViewModel", "Login started for: $email")
        if (!isNetworkAvailable(context)) {
            _uiState.value = AuthState.Error("No internet connection. Please check your network status and try again.")
            _toastMessage.value = "No internet connection."
            return
        }
        if (email.isBlank() || passwordState.length < 6) {
            _uiState.value = AuthState.Error("Email must be valid and password must be at least 6 characters")
            return
        }
        isLoginFlowActive = true
        _uiState.value = AuthState.Loading("Signing in...")
        
        viewModelScope.launch {
            try {
                authRepository.login(email, passwordState)
                    .onSuccess {
                        onAuthenticationSuccess(email, context)
                    }
                    .onFailure { error ->
                        isLoginFlowActive = false
                        android.util.Log.e("AuthViewModel", "Login failed for: $email", error)
                        _uiState.value = AuthState.Error(error.message ?: "Login failed")
                        _toastMessage.value = "Login failed: ${error.message}"
                    }
            } catch (e: Exception) {
                isLoginFlowActive = false
                android.util.Log.e("AuthViewModel", "Unexpected login exception", e)
                _uiState.value = AuthState.Error("Unexpected error: ${e.message}")
            }
        }
    }

    fun signUp(email: String, passwordState: String, context: Context? = null) {
        android.util.Log.d("AuthViewModel", "Sign up started for: $email")
        if (!isNetworkAvailable(context)) {
            _uiState.value = AuthState.Error("No internet connection. Please check your network status and try again.")
            _toastMessage.value = "No internet connection."
            return
        }
        if (email.isBlank() || passwordState.length < 6) {
            _uiState.value = AuthState.Error("Email must be valid and password must be at least 6 characters")
            return
        }
        isLoginFlowActive = true
        _uiState.value = AuthState.Loading("Creating account...")
        viewModelScope.launch {
            try {
                authRepository.signUp(email, passwordState)
                    .onSuccess {
                        onAuthenticationSuccess(email, context)
                    }
                    .onFailure { error ->
                        isLoginFlowActive = false
                        android.util.Log.e("AuthViewModel", "Sign up failed for: $email", error)
                        _uiState.value = AuthState.Error("Sign up failed: ${error.message ?: "Unknown error"}")
                        _toastMessage.value = "Sign up failed: ${error.message}"
                    }
            } catch (e: Exception) {
                isLoginFlowActive = false
                android.util.Log.e("AuthViewModel", "Unexpected sign up exception", e)
                _uiState.value = AuthState.Error("Unexpected error: ${e.message}")
            }
        }
    }

    fun signInWithGoogle(context: Context, activity: ComponentActivity) {
        isLoginFlowActive = true
        _uiState.value = AuthState.Loading("Connecting Google...")
        viewModelScope.launch {
            authRepository.signInWithGoogle(context, activity)
                .onSuccess {
                    val email = authRepository.getCurrentUserEmail() ?: "google_user@gmail.com"
                    onAuthenticationSuccess(email, context)
                }
                .onFailure {
                    isLoginFlowActive = false
                    _uiState.value = AuthState.Error(it.localizedMessage ?: "Google sign-in failed")
                }
        }
    }

    fun signInAnonymously() {
        isLoginFlowActive = true
        _uiState.value = AuthState.Loading("Signing in as Guest...")
        viewModelScope.launch {
            authRepository.signInAnonymously()
                .onSuccess {
                    val email = authRepository.getCurrentUserEmail() ?: "Guest User"
                    onAuthenticationSuccess(email, null)
                }
                .onFailure {
                    isLoginFlowActive = false
                    _uiState.value = AuthState.Error(it.localizedMessage ?: "Anonymous sign-in failed")
                }
        }
    }

    fun deleteAccount() {
        _uiState.value = AuthState.Loading("Deleting account...")
        viewModelScope.launch {
            authRepository.deleteAccount()
                .onSuccess {
                    backupManager.clearLocalData()
                    _uiState.value = AuthState.Idle
                    _isLoggedIn.value = false
                    _userRole.value = "user"
                }
                .onFailure {
                    _uiState.value = AuthState.Error(it.localizedMessage ?: "Account deletion failed")
                }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _uiState.value = AuthState.Idle
        _userRole.value = "user"
        viewModelScope.launch {
            backupManager.clearLocalData()
            authRepository.logout()
        }
    }

    class Factory(
        private val repository: AuthRepository,
        private val backupManager: com.example.data.backup.BackupManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(repository, backupManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
