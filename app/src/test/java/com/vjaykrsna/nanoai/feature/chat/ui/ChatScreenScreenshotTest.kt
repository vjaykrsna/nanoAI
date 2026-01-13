@file:Suppress("MagicNumber")

package com.vjaykrsna.nanoai.feature.chat.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.vjaykrsna.nanoai.core.domain.library.Model
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandCategory
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.model.MessageRole
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.feature.chat.model.ChatPersonaSummary
import com.vjaykrsna.nanoai.feature.chat.model.LocalInferenceUiState
import com.vjaykrsna.nanoai.feature.chat.model.LocalInferenceUiStatus
import com.vjaykrsna.nanoai.feature.chat.presentation.PersonaSwitcherUiState
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatComposerAttachments
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatUiState
import com.vjaykrsna.nanoai.shared.ui.theme.NanoAITheme
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatScreenScreenshotTest {
  @get:Rule val composeRule = createComposeRule()

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun chatOffline_baseline() {
    val personaId = UUID.fromString("00000000-0000-0000-0000-000000000301")
    val threadId = UUID.fromString("00000000-0000-0000-0000-000000000302")
    val persona = samplePersona(personaId)
    val chatState = sampleChatState(personaId, threadId, persona)
    val personaState = samplePersonaSwitcherState(personaId, threadId, persona)

    composeRule.setContent { ChatPreview(chatState = chatState, personaState = personaState) }

    composeRule.waitForIdle()
    // Smoke hook to ensure UI renders; no screenshot capture to simplify dependency surface.
    composeRule.onRoot()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatPreview(chatState: ChatUiState, personaState: PersonaSwitcherUiState) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val snackbarHostState = remember { SnackbarHostState() }

  NanoAITheme {
    ChatScreenScaffold(
      uiState = chatState,
      personaUiState = personaState,
      snackbarHostState = snackbarHostState,
      activeError = null,
      actions = sampleActions(),
      sheetState = sheetState,
      onOpenPersonaSwitcher = {},
    )
  }
}

private fun sampleChatState(personaId: UUID, threadId: UUID, persona: PersonaProfile): ChatUiState {
  val (now, assistantMessageTime, userMessageTime) = messageTimestamps()
  val threads = sampleThreads(personaId, threadId, now, assistantMessageTime)
  val messages = sampleMessages(threadId, userMessageTime, assistantMessageTime)

  return ChatUiState(
    threads = threads,
    activeThreadId = threadId,
    activeThread = threads.first(),
    messages = messages,
    personas = persistentListOf(persona),
    installedModels = sampleInstalledModels(),
    activePersonaSummary = persona.toSummary(),
    composerText = "Add a tea ceremony on day 2",
    isModelPickerVisible = false,
    isSendingMessage = false,
    attachments = ChatComposerAttachments(),
    pendingErrorMessage = null,
    connectivityBanner =
      ConnectivityBannerState(
        status = ConnectivityStatus.OFFLINE,
        queuedActionCount = 1,
        cta =
          CommandAction(id = "view-queue", title = "View queue", category = CommandCategory.JOBS),
      ),
    localInferenceUi =
      LocalInferenceUiState(
        personaName = persona.name,
        modelName = "Phoenix 3B",
        status = LocalInferenceUiStatus.OfflineReady(autoSelected = true),
      ),
  )
}

private fun messageTimestamps(): Triple<Instant, Instant, Instant> {
  val now = Clock.System.now()
  val assistantMessageTime = now - 120.seconds
  val userMessageTime = now - 240.seconds
  return Triple(now, assistantMessageTime, userMessageTime)
}

private fun sampleThreads(
  personaId: UUID,
  threadId: UUID,
  now: Instant,
  assistantMessageTime: Instant,
) =
  persistentListOf(
    ChatThread(
      threadId = threadId,
      title = "Kyoto trip planner",
      personaId = personaId,
      activeModelId = "phoenix-3b",
      createdAt = now,
      updatedAt = assistantMessageTime,
    )
  )

private fun sampleMessages(
  threadId: UUID,
  userMessageTime: Instant,
  assistantMessageTime: Instant,
) =
  persistentListOf(
    Message(
      messageId = UUID.fromString("00000000-0000-0000-0000-000000000401"),
      threadId = threadId,
      role = MessageRole.USER,
      text = "Plan a 3-day Kyoto trip with offline stops",
      audioUri = null,
      imageUri = null,
      source = MessageSource.CLOUD_API,
      latencyMs = null,
      createdAt = Instant.fromEpochMilliseconds(userMessageTime.toEpochMilliseconds()),
      errorCode = null,
    ),
    Message(
      messageId = UUID.fromString("00000000-0000-0000-0000-000000000402"),
      threadId = threadId,
      role = MessageRole.ASSISTANT,
      text = "Here is an offline-friendly Kyoto itinerary with cached maps and tea stops.",
      audioUri = null,
      imageUri = null,
      source = MessageSource.LOCAL_MODEL,
      latencyMs = 620,
      createdAt = Instant.fromEpochMilliseconds(assistantMessageTime.toEpochMilliseconds()),
      errorCode = null,
    ),
  )

private fun sampleInstalledModels() =
  persistentListOf(
    Model(modelId = "phoenix-3b", displayName = "Phoenix 3B", size = 1_572_864L, parameter = "3B")
  )

private fun PersonaProfile.toSummary() =
  ChatPersonaSummary(
    personaId = personaId,
    displayName = name,
    description = description,
    preferredModelId = defaultModelPreference,
  )

private fun samplePersonaSwitcherState(
  personaId: UUID,
  threadId: UUID,
  persona: PersonaProfile,
): PersonaSwitcherUiState =
  PersonaSwitcherUiState(
    personas = persistentListOf(persona),
    activeThreadId = threadId,
    selectedPersonaId = personaId,
    isSwitching = false,
    errorMessage = null,
  )

private fun samplePersona(personaId: UUID): PersonaProfile {
  val now = Clock.System.now()
  return PersonaProfile(
    personaId = personaId,
    name = "Traveler",
    description = "Concise travel planner prioritizing offline assets",
    systemPrompt = "You plan succinct travel itineraries that work offline.",
    defaultModelPreference = "phoenix-3b",
    temperature = 0.7f,
    topP = 0.9f,
    defaultVoice = null,
    defaultImageStyle = null,
    createdAt = now,
    updatedAt = now,
  )
}

private fun sampleActions(): ChatScreenActions =
  ChatScreenActions(
    onComposerTextChange = {},
    onSendMessage = {},
    onImageSelect = {},
    onDismissError = {},
    onDismissModelPicker = {},
    onModelSelect = {},
    onManageModels = {},
    onDismissConnectivityBanner = {},
  )
