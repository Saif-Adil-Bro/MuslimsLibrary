package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SupabaseService
import com.example.data.SupabaseUser
import com.example.data.repository.AuthRepository
import com.example.data.local.AppDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    object Idle : ProfileUiState
    object Loading : ProfileUiState
    data class Success(val user: SupabaseUser) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

sealed interface BackupUiState {
    object Idle : BackupUiState
    object Loading : BackupUiState
    object Success : BackupUiState
    data class Error(val message: String) : BackupUiState
}

data class ProfileStats(
    val booksRead: Int = 0,
    val booksInProgress: Int = 0,
    val totalFavorites: Int = 0,
    val totalNotes: Int = 0,
    val totalPins: Int = 0
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val supabaseService: SupabaseService,
    private val backupManager: com.example.data.backup.BackupManager,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _backupStatus = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupStatus: StateFlow<BackupUiState> = _backupStatus.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Boolean>(false)
    val uploadProgress: StateFlow<Boolean> = _uploadProgress.asStateFlow()

    private val _selectedDefaultAvatarUrl = MutableStateFlow<String?>(null)
    val selectedDefaultAvatarUrl: StateFlow<String?> = _selectedDefaultAvatarUrl.asStateFlow()

    private val _pendingCustomImageUri = MutableStateFlow<Uri?>(null)
    val pendingCustomImageUri: StateFlow<Uri?> = _pendingCustomImageUri.asStateFlow()

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats.asStateFlow()

    private var statsJob: kotlinx.coroutines.Job? = null

    fun loadStatistics(userId: String) {
        if (userId.isBlank()) {
            android.util.Log.w("ProfileStats", "User ID is blank!")
            return
        }

        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            try {
                // Determine the correct identifier used for local sync data (which is userEmail)
                val userEmail = authRepository.getCurrentUserEmail()
                val firebaseUid = authRepository.getCurrentUserUid()
                
                android.util.Log.d("ProfileStats", "Current user email from AuthRepository: $userEmail")
                android.util.Log.d("ProfileStats", "Firebase UID: $firebaseUid")
                android.util.Log.d("ProfileStats", "Passed user identifier: $userId")
                
                // Since local DB entries are stored under 'userEmail' (or fallback), prioritize userEmail,
                // falling back to passed userId or Firebase UID if email is blank.
                val uidToUse = when {
                    !userEmail.isNullOrBlank() -> userEmail
                    userId.contains("@") -> userId
                    !firebaseUid.isNullOrBlank() -> firebaseUid
                    else -> userId
                }
                
                android.util.Log.d("ProfileStats", "Using UID/Email for Room queries reactively: $uidToUse")
                
                // Set up visual reactive flow combination of database progress, favorites, pins and notes tables
                // Catch errors on individual flows to ensure subscription doesn't drop due to any single table query mismatch
                combine(
                    appDatabase.progressDao().getAllProgressFlow(uidToUse).catch { e ->
                        android.util.Log.e("ProfileStats", "Error resolving progress flow: ${e.message}", e)
                        emit(emptyList())
                    },
                    appDatabase.favoriteDao().getFavoritesFlow(uidToUse).catch { e ->
                        android.util.Log.e("ProfileStats", "Error resolving favorites flow: ${e.message}", e)
                        emit(emptyList())
                    },
                    appDatabase.pinDao().getPinnedBooksFlow(uidToUse).catch { e ->
                        android.util.Log.e("ProfileStats", "Error resolving pins flow: ${e.message}", e)
                        emit(emptyList())
                    },
                    appDatabase.noteDao().getNotesForUserFlow(uidToUse).catch { e ->
                        android.util.Log.e("ProfileStats", "Error resolving notes flow: ${e.message}", e)
                        emit(emptyList())
                    }
                ) { progress, favorites, pins, notes ->
                    val booksCompleted = progress.count { it.status.lowercase() == "completed" || it.progressPercentage >= 100.0 }
                    val booksReading = progress.count { it.status.lowercase() == "reading" || (it.progressPercentage > 0.0 && it.progressPercentage < 100.0) }
                    
                    ProfileStats(
                        booksRead = booksCompleted,
                        booksInProgress = booksReading,
                        totalFavorites = favorites.count { !it.isDeleted },
                        totalNotes = notes.count { !it.isDeleted },
                        totalPins = pins.count { !it.isDeleted }
                    )
                }.collect { updatedStats ->
                    _stats.value = updatedStats
                    android.util.Log.d("ProfileStats", "Reactive stats stream updated: $updatedStats")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileStats", "Error holding reactive stats stream: ${e.message}", e)
            }
        }
    }

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

    fun performBackup(userId: String) {
        _backupStatus.value = BackupUiState.Loading
        viewModelScope.launch {
            try {
                backupManager.uploadBackup(userId)
                _backupStatus.value = BackupUiState.Success
            } catch (e: SecurityException) {
                _backupStatus.value = BackupUiState.Error("ব্যবহারকারী সনাক্তকরণ ব্যর্থ হয়েছে। অনুগ্রহ করে আবার লগইন করুন।")
            } catch (e: Exception) {
                val errMsg = e.localizedMessage ?: e.message ?: ""
                val displayMsg = when {
                    errMsg.contains("Permission denied", ignoreCase = true) || errMsg.contains("403") || errMsg.contains("policy", ignoreCase = true) || errMsg.contains("unauthorized", ignoreCase = true) -> {
                        "অনুমতি অস্বীকৃত। অনুগ্রহ করে সহায়তার জন্য যোগাযোগ করুন।"
                    }
                    else -> {
                        "ব্যাকআপ ব্যর্থ হয়েছে: $errMsg"
                    }
                }
                _backupStatus.value = BackupUiState.Error(displayMsg)
            }
        }
    }

    fun performRestore(userId: String) {
        _backupStatus.value = BackupUiState.Loading
        viewModelScope.launch {
            try {
                _backupStatus.value = BackupUiState.Loading
                backupManager.downloadBackup(cloudBackupUserId = userId, roomUserId = userId)
                _backupStatus.value = BackupUiState.Success
                // Reload statistics after successful restore to update the screen counts instantly
                loadStatistics(userId)
            } catch (e: SecurityException) {
                _backupStatus.value = BackupUiState.Error("ব্যবহারকারী সনাক্তকরণ ব্যর্থ হয়েছে। অনুগ্রহ করে আবার লগইন করুন।")
            } catch (e: Exception) {
                val errMsg = e.localizedMessage ?: e.message ?: ""
                val displayMsg = when {
                    errMsg.contains("Permission denied", ignoreCase = true) || errMsg.contains("403") || errMsg.contains("policy", ignoreCase = true) || errMsg.contains("unauthorized", ignoreCase = true) -> {
                        "অনুমতি অস্বীকৃত। অনুগ্রহ করে সহায়তার জন্য যোগাযোগ করুন।"
                    }
                    errMsg.contains("FileNotFound", ignoreCase = true) || errMsg.contains("404") || errMsg.contains("not found", ignoreCase = true) -> {
                        "এই অ্যাকাউন্টের জন্য ক্লাউডে কোনো ব্যাকআপ ফাইল পাওয়া যায়নি।"
                    }
                    else -> {
                        "রিস্টোর ব্যর্থ হয়েছে: $errMsg"
                    }
                }
                _backupStatus.value = BackupUiState.Error(displayMsg)
            }
        }
    }

    fun resetBackupStatus() {
        _backupStatus.value = BackupUiState.Idle
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val supabaseService: SupabaseService,
        private val backupManager: com.example.data.backup.BackupManager,
        private val appDatabase: AppDatabase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(authRepository, supabaseService, backupManager, appDatabase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
