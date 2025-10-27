package com.vjaykrsna.nanoai.feature.library.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.catalog.DeliveryType
import com.vjaykrsna.nanoai.feature.library.domain.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
import com.vjaykrsna.nanoai.shared.ui.theme.NanoAITheme
import java.util.UUID
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class ModelLibraryAccessibilityTest {
  @JvmField @Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun modelCard_downloadAndDeleteButtonsExposeContentDescriptions() {
    val model =
      ModelPackage(
        modelId = "demo",
        displayName = "Demo Model",
        version = "1.0",
        providerType = ProviderType.MEDIA_PIPE,
        deliveryType = DeliveryType.LOCAL_ARCHIVE,
        minAppVersion = 1,
        sizeBytes = 512L * 1024,
        capabilities = setOf("chat"),
        installState = InstallState.NOT_INSTALLED,
        manifestUrl = "https://example.com/demo.json",
        checksumSha256 = null,
        signature = null,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
      )

    val modelState = mutableStateOf(model)
    val installedState = mutableStateOf(false)

    composeRule.setContent {
      NanoAITheme {
        ModelCard(
          model = modelState.value,
          isInstalled = installedState.value,
          onDownload = {},
          onDelete = {},
        )
      }
    }

    composeRule.onNodeWithContentDescription("Download Demo Model").assertIsDisplayed()

    composeRule.runOnIdle {
      modelState.value = model.copy(installState = InstallState.INSTALLED)
      installedState.value = true
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithContentDescription("Delete Demo Model").assertIsDisplayed()
  }

  @Test
  fun downloadTaskItem_exposesProgressAndControls() {
    val download =
      DownloadTask(
        taskId = UUID.randomUUID(),
        modelId = "demo",
        progress = 0.5f,
        status = DownloadStatus.DOWNLOADING,
      )

    composeRule.setContent {
      NanoAITheme {
        DownloadTaskItem(
          download = download,
          onPause = {},
          onResume = {},
          onCancel = {},
          onRetry = {},
        )
      }
    }

    composeRule.onNodeWithContentDescription("Pause download").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Cancel download").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Downloading 50%").assertIsDisplayed()
  }
}
