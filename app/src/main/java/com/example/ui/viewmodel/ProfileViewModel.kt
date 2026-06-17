package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SupabaseService
import com.example.data.SupabaseUser
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    object Idle : ProfileUiState
    object Loading : ProfileUiState
    data class Success(val user: SupabaseUser) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val supabaseService: SupabaseService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Boolean>(false)
    val uploadProgress: StateFlow<Boolean> = _uploadProgress.asStateFlow()

    private val _selectedDefaultAvatarUrl = MutableStateFlow<String?>(null)
    val selectedDefaultAvatarUrl: StateFlow<String?> = _selectedDefaultAvatarUrl.asStateFlow()

    private val _pendingCustomImageUri = MutableStateFlow<Uri?>(null)
    val pendingCustomImageUri: StateFlow<Uri?> = _pendingCustomImageUri.asStateFlow()

    fun selectDefaultAvatar(url: String?) {
        _selectedDefaultAvatarUrl.value = url
        _pendingCustomImageUri.value = null // Automatically deselect custom upload
    }

    fun selectCustomImage(uri: Uri?) {
        _pendingCustomImageUri.value = uri
        _selectedDefaultAvatarUrl.value = null // Automatically deselect default avatar
    }

    fun clearAvatarSelections() {
        _selectedDefaultAvatarUrl.value = null
        _pendingCustomImageUri.value = null
    }

    fun loadProfile() {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                val userId = authRepository.getSupabaseUid() ?: authRepository.getCurrentUserUid()
                if (userId.isNullOrBlank() || userId.contains("Error")) {
                    _uiState.value = ProfileUiState.Error("ব্যবহারকারী লগইন অবস্থায় নেই।")
                    return@launch
                }
                
                val user = supabaseService.getUserProfile(userId)
                if (user != null) {
                    _uiState.value = ProfileUiState.Success(user)
                    // Sync selections if any are empty
                } else {
                    _uiState.value = ProfileUiState.Error("ব্যবহারকারীর প্রোফাইল পাওয়া যায়নি।")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error("প্রোফাইল লোড করতে সমস্যা হয়েছে: ${e.localizedMessage}")
            }
        }
    }

    fun updateProfile(displayName: String, bio: String, onFinished: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getSupabaseUid() ?: authRepository.getCurrentUserUid()
                if (userId.isNullOrBlank() || userId.contains("Error")) {
                    onFinished(false, "ব্যবহারকারী পাওয়া যায়নি।")
                    return@launch
                }

                _uiState.value = ProfileUiState.Loading
                
                val defaultAvatar = _selectedDefaultAvatarUrl.value
                val customUri = _pendingCustomImageUri.value
                var avatarToSave: String? = null
                
                if (defaultAvatar != null) {
                    avatarToSave = defaultAvatar
                } else if (customUri != null) {
                    _uploadProgress.value = true
                    try {
                        avatarToSave = supabaseService.uploadAvatar(userId, customUri)
                    } catch (e: Exception) {
                        _uploadProgress.value = false
                        _uiState.value = ProfileUiState.Error("ছবি আপলোড ব্যর্থ হয়েছে: ${e.localizedMessage}")
                        onFinished(false, "ছবি আপলোড করতে সমস্যা হয়েছে।")
                        return@launch
                    }
                    _uploadProgress.value = false
                }

                val success = supabaseService.updateUserProfile(userId, displayName, bio)
                if (success) {
                    var avatarSuccess = true
                    if (avatarToSave != null) {
                        avatarSuccess = supabaseService.updateAvatarUrl(userId, avatarToSave)
                    }
                    
                    if (avatarSuccess) {
                        val updatedUser = supabaseService.getUserProfile(userId)
                        if (updatedUser != null) {
                            _uiState.value = ProfileUiState.Success(updatedUser)
                            clearAvatarSelections()
                            onFinished(true, "প্রোফাইল সফলভাবে আপডেট করা হয়েছে।")
                        } else {
                            _uiState.value = ProfileUiState.Error("প্রোফাইল পাওয়া যায়নি।")
                            onFinished(false, "প্রোফাইল আপডেট হয়েছে কিন্তু লোড করা যায়নি।")
                        }
                    } else {
                        _uiState.value = ProfileUiState.Error("প্রোফাইল ছবি পরিবর্তন সংরক্ষণ করা যায়নি।")
                        onFinished(false, "প্রোফাইল ছবি সেভ করা যায়নি।")
                    }
                } else {
                    _uiState.value = ProfileUiState.Error("প্রোফাইল আপডেট করা যায়নি।")
                    onFinished(false, "প্রোফাইল সংরক্ষণ করা সম্ভব হয়নি।")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.localizedMessage ?: "সমস্যা হয়েছে")
                onFinished(false, e.localizedMessage ?: "একটি অপ্রত্যাশিত ভুল হয়েছে।")
            }
        }
    }

    fun uploadAvatar(uri: Uri, onFinished: (Boolean, String) -> Unit) {
        _uploadProgress.value = true
        viewModelScope.launch {
            try {
                val userId = authRepository.getSupabaseUid() ?: authRepository.getCurrentUserUid()
                if (userId.isNullOrBlank() || userId.contains("Error")) {
                    _uploadProgress.value = false
                    onFinished(false, "ব্যবহারকারী পাওয়া যায়নি।")
                    return@launch
                }

                val avatarUrl = supabaseService.uploadAvatar(userId, uri)
                val updateSuccess = supabaseService.updateAvatarUrl(userId, avatarUrl)
                if (updateSuccess) {
                    val currentState = _uiState.value
                    if (currentState is ProfileUiState.Success) {
                        _uiState.value = ProfileUiState.Success(currentState.user.copy(avatarUrl = avatarUrl))
                    } else {
                        loadProfile()
                    }
                    _uploadProgress.value = false
                    onFinished(true, "প্রোফাইল ছবি সফলভাবে পরিবর্তন করা হয়েছে।")
                } else {
                    _uploadProgress.value = false
                    onFinished(false, "প্রোফাইল ছবি আপলোড হয়েছে কিন্তু লিংক সংরক্ষিত করা যায়নি।")
                }
            } catch (e: Exception) {
                _uploadProgress.value = false
                android.util.Log.e("ProfileViewModel", "Error in uploadAvatar: ${e.message}", e)
                onFinished(false, "অবতার ছবি পরিবর্তন ব্যর্থ হয়েছে।")
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val supabaseService: SupabaseService
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(authRepository, supabaseService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
