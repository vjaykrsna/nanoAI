package com.vjaykrsna.nanoai.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.common.onSuccess
import com.vjaykrsna.nanoai.feature.chat.domain.SendPromptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for message composition in chat.
 *
 * Manages message text, attachments, and sending state.
 */
@HiltViewModel
class MessageComposerViewModel
@Inject
constructor(
  private val sendPromptUseCase: SendPromptUseCase,
  @Suppress("UnusedPrivateProperty")
  @MainImmediateDispatcher
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

  private val _messageText = MutableStateFlow("")
  val messageText: StateFlow<String> = _messageText.asStateFlow()

  private val _isSending = MutableStateFlow(false)
  val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

  private val _errors = MutableSharedFlow<MessageComposerError>()
  val errors = _errors.asSharedFlow()

  fun updateMessageText(text: String) {
    _messageText.value = text
  }

  fun sendMessage(threadId: UUID, personaId: UUID) {
    val text = _messageText.value.trim()
    if (text.isEmpty()) {
      viewModelScope.launch { _errors.emit(MessageComposerError.EmptyMessage) }
      return
    }

    _isSending.value = true
    viewModelScope.launch {
      sendPromptUseCase(threadId, text, personaId)
        .onSuccess { _messageText.value = "" }
        .onFailure { error -> _errors.emit(MessageComposerError.SendFailed(error.message)) }
      _isSending.value = false
    }
  }

  fun clearMessage() {
    _messageText.value = ""
  }
}

sealed class MessageComposerError {
  data object EmptyMessage : MessageComposerError()

  data class SendFailed(val message: String) : MessageComposerError()
}
