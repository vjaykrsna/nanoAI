@file:Suppress("MagicNumber")

package com.vjaykrsna.nanoai

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith

/**
 * Baseline profile generator for nanoAI app. Generates optimized AOT compilation profiles for
 * common user journeys.
 *
 * TDD: This generates baseline profiles BEFORE full implementation. The profile will be refined as
 * the app matures.
 *
 * Run with: ./gradlew :macrobenchmark:generateBenchmarkReleaseBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
  @get:Rule val rule = BaselineProfileRule()

  @Test
  fun generateBaselineProfile() =
    rule.collect(packageName = "com.vjaykrsna.nanoai", maxIterations = 15, stableIterations = 3) {
      pressHome()
      startActivityAndWait()

      // Wait for app to be fully rendered
      device.wait(Until.hasObject(By.pkg(packageName)), 5000)
      device.waitForIdle()

      // Journey 1: Chat interaction
      chatInteractionJourney()

      // Journey 2: Browse model library
      modelLibraryJourney()

      // Journey 3: Persona switching
      personaSwitchJourney()

      // Journey 4: Settings and export
      settingsJourney()
    }

  private fun MacrobenchmarkScope.chatInteractionJourney() {
    // Navigate to chat (should be default screen)
    device.waitForIdle()

    // Open sidebar for chat history
    val sidebarButton = device.findObject(By.desc("Open sidebar"))
    sidebarButton?.click()
    device.wait(Until.hasObject(By.text("Chat History")), 2000)
    device.waitForIdle()

    // Scroll through chat history
    val chatHistory = device.findObject(By.res(packageName, "sidebar_chat_list"))
    chatHistory?.setGestureMargin(device.displayWidth / 5)
    repeat(2) {
      chatHistory?.scroll(Direction.DOWN, 0.8f)
      device.waitForIdle()
    }

    // Close sidebar
    device.pressBack()
    device.waitForIdle()

    // Scroll through current chat
    val messageList = device.findObject(By.res(packageName, "message_list"))
    messageList?.scroll(Direction.DOWN, 0.5f)
    device.waitForIdle()

    // Open message input
    val messageInput = device.findObject(By.res(packageName, "message_input"))
    messageInput?.click()
    device.waitForIdle()
  }

  private fun MacrobenchmarkScope.modelLibraryJourney() {
    // Navigate to model library
    val libraryTab = device.findObject(By.desc("Model Library"))
    libraryTab?.click()
    device.wait(Until.hasObject(By.text("Available Models")), 2000)
    device.waitForIdle()

    // Scroll through available models
    val modelList = device.findObject(By.res(packageName, "model_library_list"))
    modelList?.setGestureMargin(device.displayWidth / 5)
    repeat(3) {
      modelList?.scroll(Direction.DOWN, 0.8f)
      device.waitForIdle()
    }

    // Tap on a model to see details
    val firstModel = device.findObject(By.res(packageName, "model_card"))
    firstModel?.click()
    device.waitForIdle()

    // Go back to list
    device.pressBack()
    device.waitForIdle()

    // Check download queue
    val queueTab = device.findObject(By.text("Queue"))
    queueTab?.click()
    device.waitForIdle()
  }

  private fun MacrobenchmarkScope.personaSwitchJourney() {
    // Navigate back to chat
    val chatTab = device.findObject(By.desc("Chat"))
    chatTab?.click()
    device.waitForIdle()

    // Open persona selector
    val personaButton = device.findObject(By.desc("Select persona"))
    personaButton?.click()
    device.wait(Until.hasObject(By.text("Choose Persona")), 2000)
    device.waitForIdle()

    // Scroll through personas
    val personaList = device.findObject(By.res(packageName, "persona_list"))
    personaList?.scroll(Direction.DOWN, 0.5f)
    device.waitForIdle()

    // Select a persona (or create new)
    val createPersonaButton = device.findObject(By.desc("Create new persona"))
    if (createPersonaButton != null) {
      createPersonaButton.click()
      device.waitForIdle()
      device.pressBack() // Cancel creation
    }

    // Close persona selector
    device.pressBack()
    device.waitForIdle()

    // View persona switch log
    val historyButton = device.findObject(By.desc("View persona history"))
    historyButton?.click()
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()
  }

  private fun MacrobenchmarkScope.settingsJourney() {
    // Navigate to settings
    val settingsTab = device.findObject(By.desc("Settings"))
    settingsTab?.click()
    device.wait(Until.hasObject(By.text("Settings")), 2000)
    device.waitForIdle()

    // Scroll through settings
    val settingsList = device.findObject(By.res(packageName, "settings_list"))
    settingsList?.scroll(Direction.DOWN, 0.8f)
    device.waitForIdle()

    // Open API provider configuration
    val apiSection = device.findObject(By.text("Cloud API Providers"))
    apiSection?.click()
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()

    // Open privacy dashboard
    val privacySection = device.findObject(By.text("Privacy"))
    privacySection?.click()
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()

    // Open export dialog
    val exportButton = device.findObject(By.text("Export Backup"))
    exportButton?.click()
    device.wait(Until.hasObject(By.text("Export Configuration")), 2000)
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()
  }
}
