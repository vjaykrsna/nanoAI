package com.vjaykrsna.nanoai.shared.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.shared.ui.sidebar.InferencePreferenceToggleRow
import com.vjaykrsna.nanoai.shared.ui.theme.NanoAITheme
import org.junit.Test

class InferenceToggleAccessibilityTest {
  @org.junit.jupiter.api.extension.RegisterExtension
  @JvmField
  val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun inferenceToggle_exposesContentDescription() {
    composeRule.setContent {
      NanoAITheme {
        InferencePreferenceToggleRow(
          inferenceMode = InferenceMode.LOCAL_FIRST,
          onInferenceModeChange = {},
        )
      }
    }

    composeRule.onNodeWithContentDescription("Toggle inference preference").assertIsDisplayed()
  }
}
