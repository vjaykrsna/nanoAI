package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ModelLibraryScreenFilteringTest : BaseModelLibraryScreenTest() {

  @Test
  fun modelLibraryScreen_displaysModels() = runTest {
    val model1 =
      DomainTestBuilders.buildModelPackage(
        modelId = "model-1",
        displayName = "Test Model 1",
        providerType = ProviderType.MEDIA_PIPE,
      )
    val model2 =
      DomainTestBuilders.buildModelPackage(
        modelId = "model-2",
        displayName = "Test Model 2",
        providerType = ProviderType.CLOUD_API,
      )

    catalogRepository.replaceCatalog(listOf(model1, model2))

    renderModelLibraryScreen()

    composeTestRule.onNodeWithText("Test Model 1", substring = true).assertExists()
    composeTestRule.onNodeWithText("Test Model 2", substring = true).assertExists()
  }

  @Test
  fun modelLibraryScreen_filterBySearchQuery() = runTest {
    val model1 =
      DomainTestBuilders.buildModelPackage(modelId = "model-1", displayName = "Qwen Model")
    val model2 =
      DomainTestBuilders.buildModelPackage(modelId = "model-2", displayName = "Gemma Model")

    catalogRepository.replaceCatalog(listOf(model1, model2))

    renderModelLibraryScreen()

    composeTestRule.onNodeWithTag(ModelLibraryUiConstants.SEARCH_FIELD_TAG).performTextInput("Qwen")

    composeTestRule.waitUntil {
      composeTestRule
        .onAllNodesWithText("Qwen Model", substring = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    composeTestRule.onNodeWithText("Qwen Model", substring = true).assertExists()
    composeTestRule.onAllNodesWithText("Gemma Model", substring = true).assertCountEquals(0)
  }

  @Test
  fun modelLibraryScreen_filterByProvider() = runTest {
    val hfModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "hf-model",
        displayName = "HF Model",
        providerType = ProviderType.MEDIA_PIPE,
      )
    val googleModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "google-model",
        displayName = "Google Model",
        providerType = ProviderType.CLOUD_API,
      )

    catalogRepository.replaceCatalog(listOf(hfModel, googleModel))

    renderModelLibraryScreen()

    composeTestRule.onNodeWithText("HF Model", substring = true).assertExists()
    composeTestRule.onNodeWithText("Google Model", substring = true).assertExists()

    composeTestRule
      .onNodeWithText("Media pipe", substring = false)
      .assertHasClickAction()
      .performClick()

    composeTestRule.waitUntil {
      composeTestRule
        .onAllNodesWithText("Google Model", substring = true)
        .fetchSemanticsNodes()
        .isEmpty()
    }

    composeTestRule.onNodeWithText("HF Model", substring = true).assertExists()
    composeTestRule.onAllNodesWithText("Google Model", substring = true).assertCountEquals(0)
  }

  @Test
  fun modelLibraryScreen_filterByCapability() = runTest {
    val chatModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "chat-model",
        displayName = "Chat Model",
        capabilities = setOf("chat"),
      )
    val embeddingModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "embedding-model",
        displayName = "Embedding Model",
        capabilities = setOf("embeddings"),
      )

    catalogRepository.replaceCatalog(listOf(chatModel, embeddingModel))

    renderModelLibraryScreen()

    composeTestRule.onNodeWithText("Chat Model", substring = true).assertExists()
    composeTestRule.onNodeWithText("Embedding Model", substring = true).assertExists()

    // Expand filter panel
    composeTestRule.onNodeWithTag(ModelLibraryUiConstants.FILTER_TOGGLE_TAG).performClick()

    composeTestRule.onNodeWithText("Chat", substring = false).assertHasClickAction().performClick()

    composeTestRule.waitUntil {
      composeTestRule
        .onAllNodesWithText("Embedding Model", substring = true)
        .fetchSemanticsNodes()
        .isEmpty()
    }

    composeTestRule.onNodeWithText("Chat Model", substring = true).assertExists()
    composeTestRule.onAllNodesWithText("Embedding Model", substring = true).assertCountEquals(0)
  }

  @Test
  fun modelLibraryScreen_clearFilters() = runTest {
    val model = DomainTestBuilders.buildModelPackage(modelId = "test-model")
    catalogRepository.replaceCatalog(listOf(model))

    renderModelLibraryScreen()

    composeTestRule.onNodeWithTag(ModelLibraryUiConstants.SEARCH_FIELD_TAG).performTextInput("Test")

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText("Clear filters", substring = false)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    composeTestRule.onAllNodesWithText("Clear filters", substring = false)[0].performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText("Clear filters", substring = false)
        .fetchSemanticsNodes()
        .isEmpty()
    }

    composeTestRule.runOnIdle { assertThat(viewModel.filters.value.currentSearchQuery()).isEmpty() }
  }

  @Test
  fun modelLibraryScreen_filterChips_haveAccessibility() = runTest {
    val model = DomainTestBuilders.buildModelPackage(modelId = "test-model")
    catalogRepository.replaceCatalog(listOf(model))

    renderModelLibraryScreen()

    composeTestRule.onNodeWithTag(ModelLibraryUiConstants.FILTER_TOGGLE_TAG).performClick()
    composeTestRule.onNodeWithText("All providers", substring = false).assertExists()
    composeTestRule.onNodeWithText("Recommended", substring = false).assertExists()
  }
}
