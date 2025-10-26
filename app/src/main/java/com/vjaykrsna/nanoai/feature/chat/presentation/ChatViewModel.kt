package com.vjaykrsna.nanoai.feature.chat.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.common.onSuccess
import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.model.MessageRole
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.chat.domain.ConversationUseCase
import com.vjaykrsna.nanoai.feature.chat.domain.SendPromptUseCase
import com.vjaykrsna.nanoai.feature.chat.domain.SwitchPersonaUseCase
import com.vjaykrsna.nanoai.feature.library.domain.Model
import com.vjaykrsna.nanoai.feature.library.domain.ModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.domain.toModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel
@Inject
constructor(
  private val sendPromptUseCase: SendPromptUseCase,
  private val switchPersonaUseCase: SwitchPersonaUseCase,
  private val conversationUseCase: ConversationUseCase,
  private val personaRepository: PersonaRepository,
  private val modelCatalogUseCase: ModelCatalogUseCase,
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
  private val flowSharingStarted: SharingStarted = SharingStarted.Eagerly
  private val _currentThreadId = MutableStateFlow<UUID?>(null)
  val currentThreadId: StateFlow<UUID?> = _currentThreadId.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorEvents = MutableSharedFlow<ChatError>()
  val errorEvents = _errorEvents.asSharedFlow()

  private val _events = MutableSharedFlow<ChatViewEvent>()
  val events = _events.asSharedFlow()

  private val _selectedImage = MutableStateFlow<Bitmap?>(null)
  val selectedImage: StateFlow<Bitmap?> = _selectedImage.asStateFlow()

  private val _recordedAudio = MutableStateFlow<ByteArray?>(null)
  val recordedAudio: StateFlow<ByteArray?> = _recordedAudio.asStateFlow()

  private val threads: StateFlow<List<ChatThread>> =
    conversationUseCase.getAllThreadsFlow().stateIn(viewModelScope, flowSharingStarted, emptyList())

  val messages: StateFlow<List<Message>> =
    _currentThreadId
      .flatMapLatest { threadId ->
        threadId?.let { conversationUseCase.getMessagesFlow(it) } ?: flowOf(emptyList())
      }
      .stateIn(viewModelScope, flowSharingStarted, emptyList())

  val currentThread: StateFlow<ChatThread?> =
    combine(_currentThreadId, threads) { threadId, threadsSnapshot ->
        threadsSnapshot.firstOrNull { it.threadId == threadId }
      }
      .stateIn(viewModelScope, flowSharingStarted, null)

  val availablePersonas: StateFlow<List<PersonaProfile>> =
    personaRepository.observeAllPersonas().stateIn(viewModelScope, flowSharingStarted, emptyList())

  val models: StateFlow<List<Model>> =
    modelCatalogUseCase
      .observeInstalledModels()
      .map { list: List<com.vjaykrsna.nanoai.core.domain.model.ModelPackage> ->
        list.map { it.toModel() }
      }
      .stateIn(viewModelScope, flowSharingStarted, emptyList())

  private val _showModelPicker = MutableStateFlow(false)
  val showModelPicker: StateFlow<Boolean> = _showModelPicker.asStateFlow()

  fun showModelPicker() {
    _showModelPicker.value = true
  }

  fun dismissModelPicker() {
    _showModelPicker.value = false
  }

  fun selectModel(model: Model) {
    val thread = currentThread.value
    if (thread == null) {
      // TODO: Handle the case where there is no active thread
      return
    }
    viewModelScope.launch(dispatcher) {
      conversationUseCase.updateThread(thread.copy(activeModelId = model.modelId)).onFailure {
        // TODO: Handle error
      }
      _showModelPicker.value = false
      _events.emit(ChatViewEvent.ModelSelected(model.displayName))
    }
  }

  fun selectThread(threadId: UUID) {
    _currentThreadId.value = threadId
  }

  fun onImageSelected(bitmap: Bitmap) {
    _selectedImage.value = bitmap
  }

  fun onAudioRecorded(audioData: ByteArray) {
    _recordedAudio.value = audioData
  }

  fun sendMessage(text: String, personaId: UUID) {
    val threadId = _currentThreadId.value
    if (threadId == null) {
      viewModelScope.launch(dispatcher) {
        _errorEvents.emit(ChatError.ThreadCreationFailed("No active thread available"))
      }
      return
    }
    viewModelScope.launch(dispatcher) {
      _isLoading.value = true
      runCatching {
          val userMessage =
            Message(
              messageId = UUID.randomUUID(),
              threadId = threadId,
              role = MessageRole.USER,
              text = text,
              source = com.vjaykrsna.nanoai.core.model.MessageSource.LOCAL_MODEL,
              latencyMs = null,
              createdAt = kotlinx.datetime.Clock.System.now(),
            )
          conversationUseCase.saveMessage(userMessage)
        }
        .onFailure { error ->
          _errorEvents.emit(ChatError.UnexpectedError("Failed to save message: ${error.message}"))
          _isLoading.value = false
          return@launch
        }

      sendPromptUseCase(threadId, text, personaId, _selectedImage.value, _recordedAudio.value)
        .onFailure { error ->
          _errorEvents.emit(ChatError.InferenceFailed(error.message ?: "Unknown error"))
        }

      _selectedImage.value = null
      _recordedAudio.value = null
      _isLoading.value = false
    }
  }

  fun switchPersona(newPersonaId: UUID, action: PersonaSwitchAction) {
    val threadId = _currentThreadId.value ?: return
    viewModelScope.launch(dispatcher) {
      switchPersonaUseCase(threadId, newPersonaId, action)
        .onSuccess { newThreadId ->
          if (action == PersonaSwitchAction.START_NEW_THREAD) {
            _currentThreadId.value = newThreadId
          }
        }
        .onFailure { error ->
          _errorEvents.emit(
            ChatError.PersonaSwitchFailed(error.message ?: "Failed to switch persona")
          )
        }
    }
  }

  fun createNewThread(personaId: UUID?, title: String? = null) {
    viewModelScope.launch(dispatcher) {
      conversationUseCase
        .createNewThread(
          personaId ?: personaRepository.getDefaultPersona()?.personaId ?: UUID.randomUUID(),
          title,
        )
        .onSuccess { threadId -> _currentThreadId.value = threadId }
        .onFailure { error ->
          _errorEvents.emit(
            ChatError.ThreadCreationFailed(error.message ?: "Failed to create thread")
          )
        }
    }
  }

  fun archiveThread(threadId: UUID) {
    viewModelScope.launch(dispatcher) {
      conversationUseCase
        .archiveThread(threadId)
        .onSuccess {
          if (_currentThreadId.value == threadId) {
            _currentThreadId.value = null
          }
        }
        .onFailure { error ->
          _errorEvents.emit(
            ChatError.ThreadArchiveFailed(error.message ?: "Failed to archive thread")
          )
        }
    }
  }

  fun deleteThread(threadId: UUID) {
    viewModelScope.launch(dispatcher) {
      conversationUseCase
        .deleteThread(threadId)
        .onSuccess {
          if (_currentThreadId.value == threadId) {
            _currentThreadId.value = null
          }
        }
        .onFailure { error ->
          _errorEvents.emit(
            ChatError.ThreadDeletionFailed(error.message ?: "Failed to delete thread")
          )
        }
    }
  }
}

sealed class ChatError {
  data class InferenceFailed(val message: String) : ChatError()

  data class PersonaSwitchFailed(val message: String) : ChatError()

  data class ThreadCreationFailed(val message: String) : ChatError()

  data class ThreadArchiveFailed(val message: String) : ChatError()

  data class ThreadDeletionFailed(val message: String) : ChatError()

  data class UnexpectedError(val message: String) : ChatError()
}

sealed class ChatViewEvent {
  data class ModelSelected(val modelName: String) : ChatViewEvent()
}
