package com.example.ui.viewmodel

import android.content.Context
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

sealed class AuthState {
    object Idle : AuthState()
    data class Loading(val message: String = "Loading...") : AuthState()
    data class Success(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userRole = MutableStateFlow("user")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.isUserLoggedIn().collectLatest { loggedIn ->
                _isLoggedIn.value = loggedIn
                val email = authRepository.getCurrentUserEmail()
                if (loggedIn && email != null) {
                    _uiState.value = AuthState.Success(email)
                    val uid = authRepository.getCurrentUserUid()
                    if (uid != null) {
                        fetchUserRole(uid)
                    }
                } else {
                    _uiState.value = AuthState.Idle
                    _userRole.value = "user"
                }
            }
        }
    }

    private fun fetchUserRole(uid: String) {
        viewModelScope.launch {
            try {
                val role = authRepository.getUserRole(uid)
                _userRole.value = role
            } catch (e: Exception) {
                _userRole.value = "user"
            }
        }
    }

    private fun onAuthenticationSuccess(email: String) {
        _uiState.value = AuthState.Success(email)
        _isLoggedIn.value = true
        val uid = authRepository.getCurrentUserUid()
        if (uid != null) {
            fetchUserRole(uid)
        }
    }

    fun login(email: String, passwordState: String) {
        if (email.isBlank() || passwordState.length < 6) {
            _uiState.value = AuthState.Error("Email must be valid and password must be at least 6 characters")
            return
        }
        _uiState.value = AuthState.Loading("Signing in...")
        viewModelScope.launch {
            authRepository.login(email, passwordState)
                .onSuccess {
                    onAuthenticationSuccess(email)
                }
                .onFailure {
                    _uiState.value = AuthState.Error(it.localizedMessage ?: "Login failed")
                }
        }
    }

    fun signUp(email: String, passwordState: String) {
        if (email.isBlank() || passwordState.length < 6) {
            _uiState.value = AuthState.Error("Email must be valid and password must be at least 6 characters")
            return
        }
        _uiState.value = AuthState.Loading("Creating account...")
        viewModelScope.launch {
            authRepository.signUp(email, passwordState)
                .onSuccess {
                    onAuthenticationSuccess(email)
                }
                .onFailure {
                    _uiState.value = AuthState.Error(it.localizedMessage ?: "Registration failed")
                }
        }
    }

    fun signInWithGoogle(context: Context, activity: ComponentActivity) {
        _uiState.value = AuthState.Loading("Connecting Google...")
        viewModelScope.launch {
            authRepository.signInWithGoogle(context, activity)
                .onSuccess {
                    val email = authRepository.getCurrentUserEmail() ?: "google_user@gmail.com"
                    onAuthenticationSuccess(email)
                }
                .onFailure {
                    _uiState.value = AuthState.Error(it.localizedMessage ?: "Google sign-in failed")
                }
        }
    }

    fun signInAnonymously() {
        _uiState.value = AuthState.Loading("Signing in as Guest...")
        viewModelScope.launch {
            authRepository.signInAnonymously()
                .onSuccess {
                    val email = authRepository.getCurrentUserEmail() ?: "Guest User"
                    onAuthenticationSuccess(email)
                }
                .onFailure {
                    _uiState.value = AuthState.Error(it.localizedMessage ?: "Anonymous sign-in failed")
                }
        }
    }

    fun deleteAccount() {
        _uiState.value = AuthState.Loading("Deleting account...")
        viewModelScope.launch {
            authRepository.deleteAccount()
                .onSuccess {
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
        viewModelScope.launch {
            authRepository.logout()
            _isLoggedIn.value = false
            _uiState.value = AuthState.Idle
            _userRole.value = "user"
        }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
