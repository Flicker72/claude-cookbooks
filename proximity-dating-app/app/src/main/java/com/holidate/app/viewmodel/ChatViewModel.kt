package com.holidate.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.holidate.app.HoliDateApp
import com.holidate.app.data.db.ChatMessageEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Scoped to a single conversation with [peerId]. */
class ChatViewModel(app: Application, private val peerId: String) : AndroidViewModel(app) {

    private val repository = (app as HoliDateApp).container.repository

    val messages: StateFlow<List<ChatMessageEntity>> =
        repository.conversation(peerId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.sendChat(peerId, trimmed) }
    }
}
