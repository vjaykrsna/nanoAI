package com.vjaykrsna.nanoai.shared.ui.sidebar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.vjaykrsna.nanoai.core.model.InferenceMode
import org.junit.Test

class SidebarOfflineTest : BaseSidebarContentTest() {

  @Test
  fun sidebarOffline_showsCachedThreads() {
    val state = defaultState(inferenceMode = InferenceMode.LOCAL_FIRST)

    renderSidebar(state)

    composeRule.onNodeWithText("Test Conversation 1").assertIsDisplayed()
    composeRule.onNodeWithText("Test Conversation 2").assertIsDisplayed()
  }

  @Test
  fun sidebarOffline_localInferenceModePreferred() {
    val state = defaultState(inferenceMode = InferenceMode.LOCAL_FIRST)

    renderSidebar(state)

    composeRule.onNodeWithContentDescription("Toggle inference preference").assertIsDisplayed()
  }
}
