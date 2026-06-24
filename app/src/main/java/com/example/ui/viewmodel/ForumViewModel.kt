package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ForumComment
import com.example.data.ForumPost
import com.example.data.ForumPostWithComments
import com.example.data.SupabaseService
import com.example.data.ForumStats
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ForumUiState {
    object Loading : ForumUiState
    data class Success(val posts: List<ForumPost>) : ForumUiState
    object Empty : ForumUiState
    data class Error(val message: String) : ForumUiState
}

sealed interface PostDetailUiState {
    object Loading : PostDetailUiState
    data class Success(val postWithComments: ForumPostWithComments) : PostDetailUiState
    data class Error(val message: String) : PostDetailUiState
}

class ForumViewModel(
    private val supabaseService: SupabaseService,
    private val guestModeManager: com.example.data.util.GuestModeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ForumUiState>(ForumUiState.Loading)
    val uiState: StateFlow<ForumUiState> = _uiState.asStateFlow()

    private val _forumStats = MutableStateFlow<ForumStats?>(null)
    val forumStats: StateFlow<ForumStats?> = _forumStats.asStateFlow()

    private val _detailState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val detailState: StateFlow<PostDetailUiState> = _detailState.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _userNamesMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val userNamesMap: StateFlow<Map<String, String>> = _userNamesMap.asStateFlow()

    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _successMessage = MutableSharedFlow<String>()
    val successMessage: SharedFlow<String> = _successMessage.asSharedFlow()

    private val _postTags = MutableStateFlow<List<String>>(emptyList())
    val postTags: StateFlow<List<String>> = _postTags.asStateFlow()

    // Rate limiting state tracking
    private val _userPostsCount = MutableStateFlow(0)
    private val _lastPostTime = MutableStateFlow<Long?>(null)

    val categories = listOf("All", "General", "Quran", "Hadith", "Fiqh", "Sira", "Others")

    init {
        loadPosts()
    }

    private fun parseIsoToMs(isoString: String?): Long {
        if (isoString == null) return 0L
        return try {
            val trimmed = isoString.substringBefore(".").substringBefore("+")
            val parts = trimmed.split("T")
            if (parts.size < 2) return 0L
            val dateParts = parts[0].split("-")
            val timeParts = parts[1].split(":")
            if (dateParts.size < 3 || timeParts.size < 2) return 0L
            
            val year = dateParts[0].toIntOrNull() ?: 2026
            val month = dateParts[1].toIntOrNull() ?: 6
            val day = dateParts[2].toIntOrNull() ?: 17
            val hour = timeParts[0].toIntOrNull() ?: 0
            val minute = timeParts[1].toIntOrNull() ?: 0
            
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            cal.set(year, month - 1, day, hour, minute, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            0L
        }
    }

    private fun getReadableErrorMessage(errorMessage: String): String {
        return when {
            errorMessage.contains("User not found", ignoreCase = true) -> "অনুগ্রহ করে প্রথমে আপনার প্রোফাইল ফিলআপ করুন।"
            errorMessage.contains("banned", ignoreCase = true) -> "আপনার অ্যাকাউন্ট সাময়িকভাবে স্থগিত করা হয়েছে।"
            errorMessage.contains("rate limit", ignoreCase = true) -> "অতি দ্রুত রিকোয়েস্ট পাঠাচ্ছেন। দয়া করে কিছুক্ষণ অপেক্ষা করুন।"
            errorMessage.contains("validation", ignoreCase = true) -> "অনুগ্রহ করে সঠিক তথ্য প্রদান করুন।"
            else -> "দুঃখিত, একটি ত্রুটি ঘটেছে। পুনরায় চেষ্টা করুন।"
        }
    }

    suspend fun canCreatePost(): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastPost = _lastPostTime.value
        
        // Check if user posted in last 5 minutes
        if (lastPost != null && (currentTime - lastPost) < 5 * 60 * 1000) {
            _errorMessage.emit("পোস্ট করার মধ্যে অনুগ্রহ করে ৫ মিনিট অপেক্ষা করুন।")
            return false
        }    
        // Check hourly limit (10 posts per hour)
        if (_userPostsCount.value >= 10) {
            _errorMessage.emit("আপনি প্রতি ঘণ্টার সর্বোচ্চ সীমা (১০টি পোস্ট) অতিক্রম করেছেন।")
            return false
        }
        
        return true
    }

    suspend fun isDuplicatePost(userId: String, title: String): Boolean {
        try {
            val recentPosts = supabaseService.fetchUserPosts(userId)
            val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            return recentPosts.any { post ->
                post.title.equals(title, ignoreCase = true) &&
                post.createdAt != null &&
                parseIsoToMs(post.createdAt) > twentyFourHoursAgo
            }
        } catch (e: Exception) {
            android.util.Log.e("ForumViewModel", "Error checking duplicates: ${e.message}")
            return false
        }
    }

    fun loadPosts() {
        _uiState.value = ForumUiState.Loading
        viewModelScope.launch {
            try {
                // Load User Profiles and compute Usernames
                try {
                    val users = supabaseService.getAllUserProfiles()
                    val nameCount = mutableMapOf<String, Int>()
                    val userNames = mutableMapOf<String, String>()
                    for (user in users) {
                        val rawName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email.split("@").first()
                        val firstName = rawName.split(" ").first().replaceFirstChar { it.uppercase() }
                        
                        val count = nameCount.getOrDefault(firstName, 0)
                        if (count == 0) {
                            userNames[user.id] = firstName
                        } else {
                            val suffix = (kotlin.math.abs(user.id.hashCode()) % 9000) + 1000
                            userNames[user.id] = "$firstName$suffix"
                        }
                        nameCount[firstName] = count + 1
                    }
                    _userNamesMap.value = userNames
                } catch (e: Exception) {
                    android.util.Log.e("ForumViewModel", "Failed to fetch user profiles for names: ${e.message}")
                }

                val categoryFilter = if (_selectedCategory.value == "All") null else _selectedCategory.value
                val posts = supabaseService.fetchForumPosts(categoryFilter)
                android.util.Log.d("ForumViewModel", "Fetched ${posts.size} posts for category: ${_selectedCategory.value}")
                
                // Show a short info toast about fetches via successMessage
                _successMessage.emit("ফোরাম লোড হয়েছে: ${posts.size} টি পোস্ট পাওয়া গেছে")
                
                if (posts.isEmpty()) {
                    _uiState.value = ForumUiState.Empty
                } else {
                    _uiState.value = ForumUiState.Success(posts)
                }

                // Load Stats
                try {
                    val stats = supabaseService.getForumStats()
                    _forumStats.value = stats
                } catch (e: Exception) {
                    android.util.Log.e("ForumViewModel", "Failed to fetch stats: ${e.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ForumViewModel", "Failed to load posts: ${e.message}", e)
                val localizedError = getReadableErrorMessage(e.localizedMessage ?: "")
                _uiState.value = ForumUiState.Error(localizedError)
                _errorMessage.emit(localizedError)
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        loadPosts()
    }

    fun addTag(tag: String) {
        val currentTags = _postTags.value
        val newTag = tag.trim().lowercase()
        if (newTag.isNotEmpty() && currentTags.size < 10 && !currentTags.contains(newTag)) {
            _postTags.value = currentTags + newTag
        }
    }

    fun removeTag(tag: String) {
        _postTags.value = _postTags.value.filter { it != tag }
    }

    fun clearTags() {
        _postTags.value = emptyList()
    }

    private suspend fun isGuestBlocked(): Boolean {
        if (guestModeManager.isGuestMode()) {
            _errorMessage.emit("গেস্ট মোডে এই সুবিধাটি উপলব্ধ নয়। দয়া করে লগইন করুন।")
            return true
        }
        return false
    }

    fun createPost(userId: String, email: String, title: String, content: String, category: String, role: String, tags: List<String>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (isGuestBlocked()) return@launch
            try {
                if (!canCreatePost()) {
                    return@launch
                }
                
                _uiState.value = ForumUiState.Loading
                
                if (isDuplicatePost(userId, title)) {
                    _errorMessage.emit("আপনি ইতিমধ্যে একই শিরোনাম দিয়ে একটি পোস্ট করেছেন।")
                    loadPosts()
                    return@launch
                }

                // Append tags to content if they exist so they are saved
                val finalContent = if (tags.isNotEmpty()) {
                    val tagsString = tags.joinToString(", ") { "#$it" }
                    "$content\n\nTags: $tagsString"
                } else content

                supabaseService.createForumPost(userId, title, finalContent, category, email, role)
                
                try {
                    supabaseService.addNotificationLocallyAndRemotely(
                        userId = "global",
                        title = "নতুন ফোরাম পোস্ট",
                        body = "ফোরামে নতুন একটি আলোচনা শুরু হয়েছে: '$title'",
                        type = "forum"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ForumViewModel", "Failed to insert global forum post notification", e)
                }
                
                _userPostsCount.value++
                _lastPostTime.value = System.currentTimeMillis()

                _successMessage.emit("পোস্টটি সফলভাবে তৈরি হয়েছে!")
                loadPosts()
                onSuccess()
            } catch (e: Exception) {
                val localizedError = getReadableErrorMessage(e.localizedMessage ?: "")
                _errorMessage.emit("পোস্ট তৈরি করা যায়নি: $localizedError")
                loadPosts()
            }
        }
    }

    fun loadPostDetails(postId: String) {
        _detailState.value = PostDetailUiState.Loading
        viewModelScope.launch {
            try {
                if (_userNamesMap.value.isEmpty()) {
                    try {
                        val users = supabaseService.getAllUserProfiles()
                        val nameCount = mutableMapOf<String, Int>()
                        val userNames = mutableMapOf<String, String>()
                        for (user in users) {
                            val rawName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email.split("@").first()
                            val firstName = rawName.split(" ").first().replaceFirstChar { it.uppercase() }
                            
                            val count = nameCount.getOrDefault(firstName, 0)
                            if (count == 0) {
                                userNames[user.id] = firstName
                            } else {
                                val suffix = (kotlin.math.abs(user.id.hashCode()) % 9000) + 1000
                                userNames[user.id] = "$firstName$suffix"
                            }
                            nameCount[firstName] = count + 1
                        }
                        _userNamesMap.value = userNames
                    } catch (e: Exception) {
                        android.util.Log.e("ForumViewModel", "Failed to load user profiles", e)
                    }
                }

                val details = supabaseService.fetchPostDetails(postId)
                _detailState.value = PostDetailUiState.Success(details)
            } catch (e: Exception) {
                val localizedError = getReadableErrorMessage(e.localizedMessage ?: "")
                _detailState.value = PostDetailUiState.Error(localizedError)
                _errorMessage.emit("Error: $localizedError")
            }
        }
    }

    fun addComment(postId: String, userId: String, email: String, content: String, role: String) {
        viewModelScope.launch {
            if (isGuestBlocked()) return@launch
            try {
                supabaseService.createForumComment(postId, userId, content, email, role)
                _successMessage.emit("মন্তব্য যুক্ত করা হয়েছে!")
                
                // Get post author to send notification
                val currentList = (_uiState.value as? ForumUiState.Success)?.posts ?: emptyList()
                val postAuthorId = currentList.find { it.id == postId }?.userId
                val postTitle = currentList.find { it.id == postId }?.title ?: "একটি ফোরাম পোস্ট"
                if (postAuthorId != null && postAuthorId != userId) {
                    try {
                        supabaseService.addNotificationLocallyAndRemotely(
                            userId = postAuthorId,
                            title = "নতুন মন্তব্য",
                            body = "আপনার '$postTitle' পোস্টে কেউ একজন নতুন মন্তব্য করেছেন।",
                            type = "forum"
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ForumViewModel", "Failed to send comment notification", e)
                    }
                }

                loadPostDetails(postId)
                
                // Refresh list of posts as well to update comments count representation
                val categoryFilter = if (_selectedCategory.value == "All") null else _selectedCategory.value
                try {
                    val posts = supabaseService.fetchForumPosts(categoryFilter)
                    if (posts.isNotEmpty()) {
                        _uiState.value = ForumUiState.Success(posts)
                    }
                } catch (ex: Exception) {
                    // Fail silently for overview refresh
                }
            } catch (e: Exception) {
                val localizedError = getReadableErrorMessage(e.localizedMessage ?: "")
                _errorMessage.emit("মন্তব্য যুক্ত করা যায়নি: $localizedError")
            }
        }
    }

    fun deletePost(postId: String, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (isGuestBlocked()) return@launch
            try {
                val success = supabaseService.deletePost(postId, userId)
                if (success) {
                    _successMessage.emit("পোস্টটি সফলভাবে মুছে ফেলা হয়েছে!")
                    
                    // Immediately filter local post list
                    val currentState = _uiState.value
                    if (currentState is ForumUiState.Success) {
                        val updatedList = currentState.posts.filter { it.id != postId }
                        _uiState.value = ForumUiState.Success(updatedList)
                    }
                    
                    onSuccess()
                } else {
                    _errorMessage.emit("পোস্টটি মুছা সম্ভব হয়নি।")
                }
            } catch (e: Exception) {
                val localizedError = getReadableErrorMessage(e.localizedMessage ?: "")
                _errorMessage.emit("পোস্ট মুছতে সমস্যা হয়েছে: $localizedError")
            }
        }
    }

    fun reportPost(postId: String, reason: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (isGuestBlocked()) return@launch
            try {
                supabaseService.reportPost(postId, reason)
                _successMessage.emit("পোস্টটি সফলভাবে রিপোর্ট করা হয়েছে!")
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.emit("রিপোর্ট করা যায়নি: ${e.localizedMessage}")
            }
        }
    }

    fun checkUserLikes(userId: String) {
        viewModelScope.launch {
            try {
                val likedIds = supabaseService.fetchUserLikedPostIds(userId)
                _likedPostIds.value = likedIds.toSet()
            } catch (e: Exception) {
                android.util.Log.e("ForumViewModel", "Failed to check user likes: ${e.message}")
            }
        }
    }

    fun toggleLike(postId: String, userId: String) {
        viewModelScope.launch {
            if (isGuestBlocked()) return@launch
            val currentList = (_uiState.value as? ForumUiState.Success)?.posts ?: emptyList()
            val wasLiked = _likedPostIds.value.contains(postId)
            
            // Optimistic Liked State
            val updatedLikedSet = if (wasLiked) {
                _likedPostIds.value - postId
            } else {
                _likedPostIds.value + postId
            }
            _likedPostIds.value = updatedLikedSet
            
            // Optimistic counts update for list
            val updatedList = currentList.map { post ->
                if (post.id == postId) {
                    val currentCount = post.likesCount ?: 0
                    val newCount = if (wasLiked) (currentCount - 1).coerceAtLeast(0) else (currentCount + 1)
                    post.copy(likesCount = newCount)
                } else {
                    post
                }
            }
            if (currentList.isNotEmpty()) {
                _uiState.value = ForumUiState.Success(updatedList)
            }
            
            // Optimistic counts update for detail screen
            val currentDetail = _detailState.value
            if (currentDetail is PostDetailUiState.Success) {
                val detailPost = currentDetail.postWithComments.post
                if (detailPost.id == postId) {
                    val currentCount = detailPost.likesCount ?: 0
                    val newCount = if (wasLiked) (currentCount - 1).coerceAtLeast(0) else (currentCount + 1)
                    val updatedDetailPost = detailPost.copy(likesCount = newCount)
                    _detailState.value = PostDetailUiState.Success(
                        currentDetail.postWithComments.copy(post = updatedDetailPost)
                    )
                }
            }

            try {
                supabaseService.toggleLike(postId, userId)
                if (!wasLiked) {
                    val postAuthorId = currentList.find { it.id == postId }?.userId
                    val postTitle = currentList.find { it.id == postId }?.title ?: "একটি ফোরাম পোস্ট"
                    if (postAuthorId != null && postAuthorId != userId) {
                        try {
                            supabaseService.addNotificationLocallyAndRemotely(
                                userId = postAuthorId,
                                title = "নতুন লাইক",
                                body = "আপনার '$postTitle' পোস্টে কেউ একজন লাইক করেছেন।",
                                type = "forum"
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("ForumViewModel", "Failed to send like notification", e)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ForumViewModel", "Failed to toggle like: ${e.message}")
                // Rollback State
                _likedPostIds.value = if (wasLiked) {
                    _likedPostIds.value + postId
                } else {
                    _likedPostIds.value - postId
                }
                if (currentList.isNotEmpty()) {
                    _uiState.value = ForumUiState.Success(currentList)
                }
                if (currentDetail is PostDetailUiState.Success && currentDetail.postWithComments.post.id == postId) {
                    _detailState.value = currentDetail
                }
                _errorMessage.emit("লাইক আপডেট করা যায়নি: ${e.localizedMessage}")
            }
        }
    }

    fun editPost(postId: String, userId: String, newTitle: String, newContent: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (isGuestBlocked()) return@launch
            try {
                val success = supabaseService.editPost(postId, userId, newTitle, newContent)
                if (success) {
                    _successMessage.emit("পোস্টটি সফলভাবে সম্পাদন করা হয়েছে!")
                    
                    // Update main list
                    val currentList = (_uiState.value as? ForumUiState.Success)?.posts ?: emptyList()
                    val updatedList = currentList.map { post ->
                        if (post.id == postId) {
                            post.copy(title = newTitle, content = newContent)
                        } else {
                            post
                        }
                    }
                    if (currentList.isNotEmpty()) {
                        _uiState.value = ForumUiState.Success(updatedList)
                    }

                    // Update detail screen
                    val currentDetail = _detailState.value
                    if (currentDetail is PostDetailUiState.Success) {
                        val detailPost = currentDetail.postWithComments.post
                        if (detailPost.id == postId) {
                            val updatedDetailPost = detailPost.copy(title = newTitle, content = newContent)
                            _detailState.value = PostDetailUiState.Success(
                                currentDetail.postWithComments.copy(post = updatedDetailPost)
                            )
                        }
                    }
                    
                    onSuccess()
                } else {
                    _errorMessage.emit("পোস্ট সম্পাদন করা সম্ভব হয়নি।")
                }
            } catch (e: Exception) {
                val localizedError = getReadableErrorMessage(e.localizedMessage ?: "")
                _errorMessage.emit("পোস্ট সম্পাদনে ত্রুটি: $localizedError")
            }
        }
    }

    fun editComment(commentId: String, userId: String, newContent: String, postId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (isGuestBlocked()) return@launch
            try {
                val success = supabaseService.editComment(commentId, userId, newContent)
                if (success) {
                    _successMessage.emit("মন্তব্যটি সফলভাবে সম্পাদন করা হয়েছে!")
                    
                    // Update detail screen's comments
                    val currentDetail = _detailState.value
                    if (currentDetail is PostDetailUiState.Success) {
                        val comments = currentDetail.postWithComments.comments
                        val updatedComments = comments.map { comment ->
                            if (comment.id == commentId) {
                                comment.copy(content = newContent)
                            } else {
                                comment
                            }
                        }
                        _detailState.value = PostDetailUiState.Success(
                            currentDetail.postWithComments.copy(comments = updatedComments)
                        )
                    }
                    onSuccess()
                } else {
                    _errorMessage.emit("মন্তব্য সম্পাদন করা সম্ভব হয়নি।")
                }
            } catch (e: Exception) {
                val localizedError = getReadableErrorMessage(e.localizedMessage ?: "")
                _errorMessage.emit("মন্তব্য সম্পাদনে ত্রুটি: $localizedError")
            }
        }
    }

    fun deleteComment(commentId: String, userId: String, postId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (isGuestBlocked()) return@launch
            try {
                val success = supabaseService.deleteComment(commentId, userId)
                if (success) {
                    _successMessage.emit("মন্তব্যটি সফলভাবে মুছে ফেলা হয়েছে!")
                    
                    // Update detail screen
                    val currentDetail = _detailState.value
                    if (currentDetail is PostDetailUiState.Success) {
                        val comments = currentDetail.postWithComments.comments
                        val updatedComments = comments.filter { it.id != commentId }
                        
                        // Decrement comments count by 1 (or we can reload but local optimistic update is beautiful)
                        val post = currentDetail.postWithComments.post
                        val currentCount = post.repliesCount ?: 0
                        val newCount = (currentCount - 1).coerceAtLeast(0)
                        val updatedPost = post.copy(repliesCount = newCount)
                        
                        _detailState.value = PostDetailUiState.Success(
                            currentDetail.postWithComments.copy(post = updatedPost, comments = updatedComments)
                        )
                    }
                    
                    // Decrement comments count in the posts list as well
                    val currentList = (_uiState.value as? ForumUiState.Success)?.posts ?: emptyList()
                    val updatedList = currentList.map { p ->
                        if (p.id == postId) {
                            val currentCount = p.repliesCount ?: 0
                            val newCount = (currentCount - 1).coerceAtLeast(0)
                            p.copy(repliesCount = newCount)
                        } else {
                            p
                        }
                    }
                    if (currentList.isNotEmpty()) {
                        _uiState.value = ForumUiState.Success(updatedList)
                    }
                    
                    onSuccess()
                } else {
                    _errorMessage.emit("মন্তব্যটি মুছা সম্ভব হয়নি।")
                }
            } catch (e: Exception) {
                val localizedError = getReadableErrorMessage(e.localizedMessage ?: "")
                _errorMessage.emit("মন্তব্য মুছতে ত্রুটি: $localizedError")
            }
        }
    }

    class Factory(
        private val supabaseService: SupabaseService,
        private val guestModeManager: com.example.data.util.GuestModeManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ForumViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ForumViewModel(supabaseService, guestModeManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
