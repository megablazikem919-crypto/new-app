package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SentHug
import com.example.data.SentHugRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PandaCheerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SentHugRepository

    // Expose all sent hugs reactively from database
    val sentHugs: StateFlow<List<SentHug>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SentHugRepository(database.sentHugDao())
        sentHugs = repository.allSentHugs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // User's name personalization ("cutie ji" by default)
    private val _userName = MutableStateFlow("cutie ji")
    val userName: StateFlow<String> = _userName.asStateFlow()

    // Interactive cute quote
    private val _currentCheeringQuote = MutableStateFlow("Get happy, aapke cheeks acche ni lgte udaas!")
    val currentCheeringQuote: StateFlow<String> = _currentCheeringQuote.asStateFlow()

    // Confetti event emission
    private val _confettiTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val confettiTrigger: SharedFlow<Unit> = _confettiTrigger.asSharedFlow()

    // Cheering quotes list
    private val customQuotes = listOf(
        "Get happy, aapke cheeks acche ni lgte udaas!",
        "Your smile is more precious than all the bamboo in the world. 🎋❤️",
        "Even the stars are jealous of your sparkle, cutie! ✨",
        "Take a deep breath. Everything is going to be alright. 🌈",
        "A fluffy panda hug is waiting for you whenever you need it! 🤗",
        "You're the brightest part of my day! ☀️",
        "No matter what happens, you are doing great. Keep shining!",
        "Cheeks up, cutie! Smiles suit you the absolute best! 🥰",
        "A soft little panda roll for you! 🐼 *tumble-tumble*",
        "Sending cosmic sparkles of joy directly to your heart! 💖"
    )

    private var quoteIndex = 0

    fun setUserName(name: String) {
        if (name.isNotBlank()) {
            _userName.value = name
        } else {
            _userName.value = "cutie ji"
        }
    }

    fun triggerCheer() {
        // Cycle quotes
        quoteIndex = (quoteIndex + 1) % customQuotes.size
        _currentCheeringQuote.value = customQuotes[quoteIndex]
        
        // Trigger visual confetti
        viewModelScope.launch {
            _confettiTrigger.emit(Unit)
        }
    }

    fun sendHugToFriend(friendName: String, message: String, emoji: String) {
        viewModelScope.launch {
            val finalName = if (friendName.isNotBlank()) friendName else "Best Friend"
            val finalMessage = if (message.isNotBlank()) message else "Thinking of you & sending panda vibes!"
            repository.insert(
                SentHug(
                    friendName = finalName,
                    hugMessage = finalMessage,
                    pandaEmoji = emoji
                )
            )
            // Trigger confetti for successfully spreading joy!
            _confettiTrigger.emit(Unit)
        }
    }

    fun clearHugsLog() {
        viewModelScope.launch {
            repository.clear()
        }
    }
}
