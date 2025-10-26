package com.vjaykrsna.nanoai.shared.ui.sidebar

import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.model.InferenceMode
import org.junit.Test

class SidebarInteractionTest : BaseSidebarContentTest() {

  @Test
  fun sidebarNewConversation_createsThread() {
    val state = defaultState()

    renderSidebar(
      state,
      interactions = SidebarInteractions(onNewThread = { newThreadClicked = true }),
    )

    composeRule.onNodeWithContentDescription("Create new conversation").performClick()

    composeRule.runOnIdle { assertThat(newThreadClicked).isTrue() }
  }

  @Test
  fun sidebarSearchField_filtersThreads() {
    val state = defaultState()

    renderSidebar(
      state,
      interactions = SidebarInteractions(onSearchQueryChange = { searchQuery = it }),
    )

    composeRule.onNodeWithContentDescription("Search conversations").performTextInput("Test 1")

    composeRule.runOnIdle { assertThat(searchQuery).isEqualTo("Test 1") }
  }

  @Test
  fun sidebarArchiveToggle_updatesState() {
    var showArchived = false
    val state = defaultState()

    renderSidebar(
      state,
      interactions = SidebarInteractions(onToggleArchive = { showArchived = !showArchived }),
    )

    composeRule.onNodeWithContentDescription("Toggle archived conversations").performClick()

    composeRule.runOnIdle { assertThat(showArchived).isTrue() }
  }

  @Test
  fun sidebarInferenceModeToggle_switchesModes() {
    var inferenceMode = InferenceMode.CLOUD_FIRST
    val state = defaultState()

    renderSidebar(
      state.copy(inferenceMode = inferenceMode),
      interactions = SidebarInteractions(onInferenceModeChange = { inferenceMode = it }),
    )

    composeRule.onNodeWithContentDescription("Toggle inference preference").performClick()

    composeRule.runOnIdle { assertThat(inferenceMode).isEqualTo(InferenceMode.LOCAL_FIRST) }
  }

  @Test
  fun sidebarTalkBack_announcesCorrectly() {
    val state = defaultState()

    renderSidebar(state)

    composeRule
      .onNodeWithContentDescription("Conversation navigation drawer")
      .assertIsDisplayed()
      .assertContentDescriptionContains("Conversation navigation drawer")

    composeRule
      .onNodeWithContentDescription("Create new conversation")
      .assertIsDisplayed()
      .assertContentDescriptionContains("Create new conversation")
  }
}
