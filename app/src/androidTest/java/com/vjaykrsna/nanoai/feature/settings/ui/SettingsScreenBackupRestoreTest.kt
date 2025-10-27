package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vjaykrsna.nanoai.feature.settings.domain.ImportSummary
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsError
import org.junit.Test

class SettingsScreenBackupRestoreTest : BaseSettingsScreenTest() {

  @Test
  fun settingsScreen_exportData_showsDialog() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Backup & Restore").performClick()
    composeTestRule.waitForIdle()
    // TODO: click export button when surfaced for testing
  }

  @Test
  fun settingsScreen_exportSuccess_showsSnackbar() {
    renderSettingsScreen()

    composeTestRule.runOnIdle { mockExportSuccess.tryEmit("Export successful") }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText(
          "Backup exported to Export successful",
          substring = true,
          useUnmergedTree = true,
        )
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }
    composeTestRule
      .onNodeWithText(
        "Backup exported to Export successful",
        substring = true,
        useUnmergedTree = true,
      )
      .assertExists()
  }

  @Test
  fun settingsScreen_importData_triggersFilePicker() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("Backup & Restore").performClick()
    composeTestRule.waitForIdle()
    // TODO: click import button when file picker hook is exposed
  }

  @Test
  fun settingsScreen_importSuccess_showsSnackbar() {
    renderSettingsScreen()

    composeTestRule.runOnIdle {
      mockImportSuccess.tryEmit(
        ImportSummary(
          personasImported = 5,
          personasUpdated = 0,
          providersImported = 2,
          providersUpdated = 1,
        )
      )
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText(
          "Imported backup: 5 personas, 3 providers",
          substring = false,
          useUnmergedTree = true,
        )
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }
    composeTestRule
      .onNodeWithText(
        "Imported backup: 5 personas, 3 providers",
        substring = false,
        useUnmergedTree = true,
      )
      .assertExists()
  }

  @Test
  fun settingsScreen_error_showsSnackbar() {
    renderSettingsScreen()

    composeTestRule.runOnIdle {
      mockErrorEvents.tryEmit(SettingsError.UnexpectedError("Test error"))
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText(
          "Unexpected error: Test error",
          substring = false,
          useUnmergedTree = true,
        )
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }
    composeTestRule
      .onNodeWithText("Unexpected error: Test error", substring = false, useUnmergedTree = true)
      .assertExists()
  }
}
