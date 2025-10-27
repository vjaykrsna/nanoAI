package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import io.mockk.coEvery
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ModelLibraryScreenStructureTest : BaseModelLibraryScreenTest() {

  @Test
  fun modelLibraryScreen_displaysContentDescription() {
    renderModelLibraryScreen()

    composeTestRule
      .onNodeWithContentDescription("Model library screen with enhanced management controls")
      .assertIsDisplayed()
  }

  @Test
  fun modelLibraryScreen_displaysHeader() {
    renderModelLibraryScreen()

    composeTestRule.onNodeWithText("Installed").assertExists()
    composeTestRule.onNodeWithText("Storage").assertExists()
  }

  @Test
  fun modelLibraryScreen_displaysLoadingIndicator() = runTest {
    catalogRepository.replaceCatalog(emptyList())
    coEvery { refreshUseCase.invoke() } coAnswers
      {
        delay(1_200)
        NanoAIResult.success(Unit)
      }

    renderModelLibraryScreen()

    composeTestRule.runOnIdle { viewModel.refreshCatalog() }
    composeTestRule.waitUntil(timeoutMillis = 5_000) { viewModel.isLoading.value }
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithTag(ModelLibraryUiConstants.LOADING_INDICATOR_TAG)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
  }

  @Test
  fun modelLibraryScreen_organizesModelsIntoSections() = runTest {
    val needsAttention =
      DomainTestBuilders.buildModelPackage(
        modelId = "attention-model",
        displayName = "Attention Model",
        installState = InstallState.ERROR,
      )
    val installed =
      DomainTestBuilders.buildModelPackage(
        modelId = "installed-model",
        displayName = "Installed Model",
        installState = InstallState.INSTALLED,
      )
    val available =
      DomainTestBuilders.buildModelPackage(
        modelId = "available-model",
        displayName = "Available Model",
        installState = InstallState.NOT_INSTALLED,
      )

    catalogRepository.replaceCatalog(listOf(needsAttention, installed, available))

    renderModelLibraryScreen()

    composeTestRule.runOnIdle { assertThat(viewModel.curatedSections.value.available).isNotEmpty() }

    val listNode = composeTestRule.onNodeWithTag(ModelLibraryUiConstants.LIST_TAG)
    listNode.performScrollToNode(hasText("Attention Model", substring = false))
    composeTestRule.onNodeWithText("Attention Model", substring = false).assertExists()

    listNode.performScrollToNode(hasText("Installed Model", substring = false))
    composeTestRule.onNodeWithText("Installed Model", substring = false).assertExists()

    listNode.performScrollToNode(hasText("Available Model", substring = false))
    composeTestRule.onNodeWithText("Available Model", substring = false).assertExists()
  }

  @Test
  fun modelLibraryScreen_emptyState() = runTest {
    catalogRepository.replaceCatalog(emptyList())

    renderModelLibraryScreen()

    composeTestRule.onNodeWithText("No models to show", substring = true).assertExists()
  }

  @Test
  fun modelLibraryScreen_pullToRefresh() {
    renderModelLibraryScreen()

    viewModel.refreshCatalog()
    composeTestRule.waitForIdle()
  }
}
