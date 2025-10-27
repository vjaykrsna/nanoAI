package com.vjaykrsna.nanoai.feature.uiux.contracts

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceAuthState
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsUiUxState
import com.vjaykrsna.nanoai.feature.settings.ui.SettingsContentState
import com.vjaykrsna.nanoai.feature.settings.ui.SettingsScreenActions
import com.vjaykrsna.nanoai.feature.settings.ui.SettingsScreenContent
import com.vjaykrsna.nanoai.shared.ui.theme.NanoAITheme
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Contract test for Settings screen (FR-008).
 *
 * Validates the new tabbed architecture exposes core categories, contextual actions, and carries
 * the legacy provider/backup controls in their dedicated sections.
 */
@LargeTest
@OptIn(ExperimentalTestApi::class)
@Ignore("Settings contract expectations pending finalized copy; see specs/003-UI-UX/plan.md")
class SettingsScreenContractTest {
  @JvmField @Rule val environmentRule = TestEnvironmentRule()
  @JvmField @Rule val composeRule = createComposeRule()

  private val sampleProvider =
    APIProviderConfig(
      providerId = "provider-1",
      providerName = "Test Provider",
      baseUrl = "https://api.example.com",
      apiKey = "key",
      apiType = APIType.OPENAI_COMPATIBLE,
      isEnabled = true,
    )

  private val defaultState =
    SettingsContentState(
      apiProviders = listOf(sampleProvider),
      privacyPreferences = PrivacyPreference(),
      uiUxState = SettingsUiUxState(showMigrationSuccessNotification = true),
      huggingFaceState = HuggingFaceAuthState.unauthenticated(),
    )

  private val noopActions =
    SettingsScreenActions(
      onAddProviderClick = {},
      onProviderEdit = {},
      onProviderDelete = {},
      onImportBackupClick = {},
      onExportBackupClick = {},
      onTelemetryToggle = {},
      onRetentionPolicyChange = {},
      onDismissMigrationSuccess = {},
      onThemePreferenceChange = {},
      onVisualDensityChange = {},
      onHighContrastChange = {},
      onHuggingFaceLoginClick = {},
      onHuggingFaceApiKeyClick = {},
      onHuggingFaceDisconnectClick = {},
      onStatusMessageShow = {},
    )

  @Test
  fun settingsScreen_displaysTabbedCategories_andGeneralCopy() {
    composeRule.setContent {
      NanoAITheme {
        SettingsScreenContent(
          state = defaultState,
          snackbarHostState = SnackbarHostState(),
          actions = noopActions,
        )
      }
    }

    composeRule.onNodeWithText("Settings").assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription("Settings screen organized by tabs with contextual sections")
      .assertIsDisplayed()
    composeRule.onNodeWithText("Appearance").assertIsDisplayed()
    composeRule.onNodeWithText("Appearance").assertHasClickAction()
    composeRule.onNodeWithText("Appearance").performClick()
    composeRule.waitUntilExactlyOneExists(hasText("System"))
    composeRule.onNodeWithText("System").assertIsDisplayed()
    composeRule.onNodeWithText("Light").assertIsDisplayed()
    composeRule.onNodeWithText("Dark").assertIsDisplayed()
    composeRule.onNodeWithText("AMOLED").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_apisSection_showsProvidersAndFab() {
    composeRule.setContent {
      NanoAITheme {
        SettingsScreenContent(
          state = defaultState,
          snackbarHostState = SnackbarHostState(),
          actions = noopActions,
        )
      }
    }

    val apisTab = composeRule.onNodeWithText("APIs")
    apisTab.assertIsDisplayed()
    apisTab.assertHasClickAction()
    apisTab.performClick()
    composeRule.waitUntilExactlyOneExists(hasText("Test Provider"))

    composeRule.onNodeWithText("Test Provider").assertIsDisplayed()
    composeRule
      .onNodeWithText("Connect remote APIs or local runtimes to power nanoAI's modes.")
      .assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription("Add API provider")
      .assertIsDisplayed()
      .assertHasClickAction()
    composeRule.onNodeWithContentDescription("Edit Test Provider").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Delete Test Provider").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_backupSyncSection_exposesImportAndExport() {
    composeRule.setContent {
      NanoAITheme {
        SettingsScreenContent(
          state = defaultState,
          snackbarHostState = SnackbarHostState(),
          actions = noopActions,
        )
      }
    }

    val backupTab = composeRule.onNodeWithText("Backup & Sync")
    backupTab.assertIsDisplayed()
    backupTab.assertHasClickAction()
    backupTab.performClick()
    composeRule.waitUntilExactlyOneExists(hasText("Import Backup"))

    composeRule.onNodeWithText("Import Backup").assertIsDisplayed().assertHasClickAction()
    composeRule.onNodeWithText("Export Backup").assertIsDisplayed().assertHasClickAction()
  }
}
