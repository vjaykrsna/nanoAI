package com.vjaykrsna.nanoai.feature.chat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.domain.library.Model
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.model.MessageRole
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatError
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatUiEvent
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatViewModel
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatUiState
import com.vjaykrsna.nanoai.feature.chat.ui.components.ModelPicker
import com.vjaykrsna.nanoai.feature.uiux.presentation.ChatState
import com.vjaykrsna.nanoai.feature.uiux.presentation.NanoError
import com.vjaykrsna.nanoai.feature.uiux.ui.components.composer.NanoComposerBar
import com.vjaykrsna.nanoai.feature.uiux.ui.components.feedback.NanoErrorHandler
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoRadii
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val MESSAGE_BUBBLE_WIDTH_FRACTION = 0.85f

/**
 * Main chat interface screen that displays conversation history and provides message input.
 *
 * Features:
 * - Real-time message display with user/assistant differentiation
 * - Persistent composer text across configuration changes
 * - Scroll position preservation
 * - Loading states and error handling
 * - Accessibility support with proper content descriptions
 *
 * @param modifier Modifier to apply to the screen
 * @param viewModel ChatViewModel for managing chat state and operations
 * @param onUpdateChatState Callback to notify parent about chat state changes (optional)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun ChatScreen(
  modifier: Modifier = Modifier,
  viewModel: ChatViewModel = hiltViewModel(),
  onUpdateChatState: ((ChatState?) -> Unit)? = null,
  onNavigate: (ModeId) -> Unit = {},
) {
  val uiState by viewModel.state.collectAsStateWithLifecycle()
  val sheetState = rememberModalBottomSheetState()
  val snackbarHostState = remember { SnackbarHostState() }
  var activeError by remember { mutableStateOf<NanoError?>(null) }
  val latestOnUpdateChatState = rememberUpdatedState(onUpdateChatState)

  val launchImagePicker = rememberChatImagePicker { bitmap -> viewModel.onImageSelected(bitmap) }

  LaunchedEffect(viewModel) {
    launch {
      viewModel.events.collectLatest { event ->
        when (event) {
          is ChatUiEvent.ErrorRaised -> {
            activeError = event.error.toNanoError(event.envelope)
            snackbarHostState.showSnackbar(event.envelope.userMessage)
          }
          is ChatUiEvent.ModelSelected ->
            snackbarHostState.showSnackbar("Switched to ${event.modelName}")
        }
      }
    }

    latestOnUpdateChatState.value?.let { update ->
      launch {
        viewModel.state.collectLatest { state ->
          update(
            ChatState(
              availablePersonas = state.personas,
              currentPersonaId = state.activeThread?.personaId,
            )
          )
        }
      }
    }
  }

  val actions =
    remember(viewModel, launchImagePicker, onNavigate) {
      ChatScreenActions(
        onComposerTextChange = viewModel::onComposerTextChanged,
        onSendMessage = {
          viewModel.onSendMessage()
          activeError = null
        },
        onImageSelect = launchImagePicker,
        onDismissError = {
          activeError = null
          viewModel.clearPendingError()
        },
        onDismissModelPicker = viewModel::dismissModelPicker,
        onModelSelect = viewModel::selectModel,
        onManageModels = {
          viewModel.dismissModelPicker()
          onNavigate(ModeId.LIBRARY)
        },
      )
    }

  ChatScreenScaffold(
    uiState = uiState,
    snackbarHostState = snackbarHostState,
    activeError = activeError,
    actions = actions,
    sheetState = sheetState,
    modifier = modifier,
  )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ChatScreenScaffold(
  uiState: ChatUiState,
  snackbarHostState: SnackbarHostState,
  activeError: NanoError?,
  actions: ChatScreenActions,
  sheetState: SheetState,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier.fillMaxSize().semantics {
        contentDescription = "Chat screen with message history and input"
      }
  ) {
    ChatMessageContent(
      uiState = uiState,
      snackbarHostState = snackbarHostState,
      activeError = activeError,
      actions = actions,
    )

    SnackbarHost(
      hostState = snackbarHostState,
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .padding(horizontal = 16.dp, vertical = 24.dp)
          .semantics { contentDescription = "Chat notifications and messages" },
    )

    if (uiState.isModelPickerVisible) {
      ModalBottomSheet(onDismissRequest = actions.onDismissModelPicker, sheetState = sheetState) {
        ModelPicker(
          models = uiState.installedModels,
          selectedModelId = uiState.activeThread?.activeModelId,
          onModelSelect = actions.onModelSelect,
          onManageModelsClick = actions.onManageModels,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun ChatMessageContent(
  uiState: ChatUiState,
  snackbarHostState: SnackbarHostState,
  activeError: NanoError?,
  actions: ChatScreenActions,
) {
  Column(
    modifier =
      Modifier.fillMaxSize().padding(horizontal = NanoSpacing.lg, vertical = NanoSpacing.md),
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
  ) {
    MessagesList(
      messages = uiState.messages,
      isLoading = uiState.isSendingMessage,
      modifier = Modifier.weight(1f).fillMaxWidth(),
    )

    NanoErrorHandler(
      error = activeError,
      snackbarHostState = snackbarHostState,
      modifier = Modifier.fillMaxWidth(),
      onDismiss = actions.onDismissError,
    )

    uiState.attachments.image?.let { attachment ->
      Image(
        bitmap = attachment.bitmap.asImageBitmap(),
        contentDescription = "Selected image",
        modifier = Modifier.size(128.dp),
      )
    }

    ChatComposerBar(uiState = uiState, actions = actions)
  }
}

@Composable
private fun ChatComposerBar(uiState: ChatUiState, actions: ChatScreenActions) {
  NanoComposerBar(
    value = uiState.composerText,
    onValueChange = actions.onComposerTextChange,
    modifier = Modifier.fillMaxWidth(),
    placeholder = "Type a message…",
    enabled = !uiState.isSendingMessage && uiState.activeThread != null,
    onSend = actions.onSendMessage,
    sendEnabled =
      uiState.composerText.isNotBlank() &&
        uiState.activeThread != null &&
        !uiState.isSendingMessage,
    isSending = uiState.isSendingMessage,
    onImageSelect = { actions.onImageSelect() },
    onAudioRecord = { /* TODO: Implement audio recording */ },
  )
}

@Composable
private fun MessagesList(
  messages: List<Message>,
  isLoading: Boolean,
  modifier: Modifier = Modifier,
) {
  val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  LazyColumn(
    state = listState,
    contentPadding = PaddingValues(NanoSpacing.md),
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
    reverseLayout = false,
    modifier =
      modifier.semantics {
        liveRegion = LiveRegionMode.Polite
        contentDescription = "Message history list"
      },
  ) {
    items(items = messages, key = { it.messageId.toString() }, contentType = { it.role }) { message
      ->
      MessageBubble(message = message)
    }

    if (isLoading) {
      item {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(
            modifier =
              Modifier.size(24.dp).semantics { contentDescription = "AI is generating a response" }
          )
        }
      }
    }
  }
}

@Composable
private fun MessageBubble(message: Message, modifier: Modifier = Modifier) {
  val isUser = message.role == MessageRole.USER
  val backgroundColor =
    if (isUser) {
      MaterialTheme.colorScheme.primaryContainer
    } else {
      MaterialTheme.colorScheme.secondaryContainer
    }

  val alignment = if (isUser) Alignment.End else Alignment.Start

  Column(horizontalAlignment = alignment, modifier = modifier.fillMaxWidth()) {
    Card(
      colors = CardDefaults.cardColors(containerColor = backgroundColor),
      shape = RoundedCornerShape(NanoRadii.medium),
      modifier =
        Modifier.fillMaxWidth(MESSAGE_BUBBLE_WIDTH_FRACTION).semantics {
          val roleDescription = if (isUser) "Your" else "Assistant's"
          contentDescription = "$roleDescription message: ${message.text ?: ""}"
        },
    ) {
      Column(modifier = Modifier.padding(NanoSpacing.md)) {
        message.text?.let { text -> Text(text = text, style = MaterialTheme.typography.bodyLarge) }

        Spacer(modifier = Modifier.height(NanoSpacing.xs))

        val timestamp =
          message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).let {
            "${it.hour}:${it.minute.toString().padStart(2, '0')}"
          }

        Text(
          text = timestamp,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.semantics { contentDescription = "Message sent at $timestamp" },
        )
      }
    }
  }
}

private data class ChatScreenActions(
  val onComposerTextChange: (String) -> Unit,
  val onSendMessage: () -> Unit,
  val onImageSelect: () -> Unit,
  val onDismissError: () -> Unit,
  val onDismissModelPicker: () -> Unit,
  val onModelSelect: (Model) -> Unit,
  val onManageModels: () -> Unit,
)

private fun ChatError.toNanoError(envelope: NanoAIErrorEnvelope): NanoError {
  val description = envelope.userMessage
  return when (this) {
    is ChatError.InferenceFailed ->
      NanoError.Inline(title = "Couldn't complete inference", description = description)
    is ChatError.PersonaSwitchFailed ->
      NanoError.Inline(title = "Persona switch failed", description = description)
    is ChatError.PersonaSelectionFailed ->
      NanoError.Inline(title = "No persona selected", description = description)
    is ChatError.ThreadCreationFailed ->
      NanoError.Inline(title = "Couldn't start conversation", description = description)
    is ChatError.ThreadArchiveFailed ->
      NanoError.Inline(title = "Couldn't archive conversation", description = description)
    is ChatError.ThreadDeletionFailed ->
      NanoError.Inline(title = "Couldn't delete conversation", description = description)
    is ChatError.UnexpectedError ->
      NanoError.Inline(title = "Something went wrong", description = description)
  }
}
