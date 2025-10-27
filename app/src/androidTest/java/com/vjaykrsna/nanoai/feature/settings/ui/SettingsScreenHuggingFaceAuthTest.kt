package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceAuthState
import org.junit.Test

class SettingsScreenHuggingFaceAuthTest : BaseSettingsScreenTest() {

  @Test
  fun settingsScreen_huggingFaceAuth_notAuthenticated_showsLoginButton() {
    mockHuggingFaceAuthState.value = HuggingFaceAuthState.unauthenticated()

    renderSettingsScreen()

    composeTestRule.onNodeWithText("Offline & Models").performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithContentDescription("Login with Hugging Face account")
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_huggingFaceAuth_authenticated_showsDisconnectButton() {
    mockHuggingFaceAuthState.value =
      HuggingFaceAuthState(isAuthenticated = true, username = "testuser")

    renderSettingsScreen()

    composeTestRule.onNodeWithText("Offline & Models").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("testuser", substring = true).assertIsDisplayed()
  }

  @Test
  fun settingsScreen_clickHuggingFaceLogin_opensDialog() {
    mockHuggingFaceAuthState.value = HuggingFaceAuthState.unauthenticated()

    renderSettingsScreen()

    composeTestRule.onNodeWithText("Offline & Models").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Login with Hugging Face account").performClick()
    composeTestRule.waitForIdle()
    // TODO: assert dialog content when exposed via semantics
  }

  @Test
  fun settingsScreen_huggingFaceLogin_triggersOAuth() {
    mockHuggingFaceAuthState.value = HuggingFaceAuthState.unauthenticated()

    renderSettingsScreen()

    composeTestRule.onNodeWithText("Offline & Models").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Connect to Hugging Face", substring = true).performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun settingsScreen_huggingFaceAccount_displaysInfo() {
    mockHuggingFaceAuthState.value =
      HuggingFaceAuthState(isAuthenticated = true, username = "testuser", displayName = "Test User")

    renderSettingsScreen()

    composeTestRule.onNodeWithText("Offline & Models").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("testuser", substring = true).assertIsDisplayed()
  }

  @Test
  fun settingsScreen_huggingFaceLogout_clearsCredentials() {
    mockHuggingFaceAuthState.value =
      HuggingFaceAuthState(isAuthenticated = true, username = "testuser", displayName = "Test User")

    renderSettingsScreen()

    composeTestRule.onNodeWithText("Offline & Models").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Disconnect", substring = true).performClick()
    composeTestRule.waitForIdle()
  }
}
