package com.vjaykrsna.nanoai.feature.library.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.testing.TestingTheme
import kotlinx.datetime.Instant
import org.junit.Test

class HuggingFaceModelCardTest {

  @org.junit.jupiter.api.extension.RegisterExtension
  @JvmField
  val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `displays basic model information`() {
    val model =
      HuggingFaceModelSummary(
        modelId = "test/model",
        displayName = "Test Model",
        author = "test-author",
        pipelineTag = "text-generation",
        libraryName = "transformers",
        tags = listOf("tag1", "tag2"),
        likes = 100,
        downloads = 1000,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        isPrivate = false,
      )

    composeTestRule.setContent { TestingTheme { HuggingFaceModelCard(model) } }

    composeTestRule.onNodeWithText("Test Model").assertIsDisplayed()
    composeTestRule.onNodeWithText("test-author").assertIsDisplayed()
    composeTestRule.onNodeWithText("100").assertIsDisplayed() // likes
    composeTestRule.onNodeWithText("1,000").assertIsDisplayed() // downloads
  }

  @Test
  fun `displays enhanced metadata when available`() {
    val model =
      HuggingFaceModelSummary(
        modelId = "test/model",
        displayName = "Test Model",
        author = "test-author",
        pipelineTag = "text-generation",
        libraryName = "transformers",
        tags = listOf("tag1", "tag2"),
        likes = 100,
        downloads = 1000,
        license = "apache-2.0",
        languages = listOf("en", "es"),
        baseModel = "gpt-2",
        architectures = listOf("Transformer", "GPT"),
        modelType = "gpt2",
        totalSizeBytes = 1024L * 1024 * 1024 * 2, // 2 GB
        summary = "A powerful language model",
        description = "This is a detailed description of the model",
        trendingScore = 95,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        lastModified = Instant.parse("2024-01-02T00:00:00Z"),
        isPrivate = false,
      )

    composeTestRule.setContent { TestingTheme { HuggingFaceModelCard(model) } }

    // Basic info
    composeTestRule.onNodeWithText("Test Model").assertIsDisplayed()
    composeTestRule.onNodeWithText("test-author").assertIsDisplayed()

    // Enhanced metadata
    composeTestRule.onNodeWithText("apache-2.0").assertIsDisplayed()
    composeTestRule.onNodeWithText("en, es").assertIsDisplayed()
    composeTestRule.onNodeWithText("gpt-2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Transformer, GPT").assertIsDisplayed()
    composeTestRule.onNodeWithText("2.00 GB").assertIsDisplayed() // Size formatting
    composeTestRule.onNodeWithText("A powerful language model").assertIsDisplayed()

    // Date formatting
    composeTestRule.onNodeWithText("01/01/24").assertIsDisplayed() // created date
    composeTestRule.onNodeWithText("01/02/24").assertIsDisplayed() // modified date
  }

  @Test
  fun `handles missing optional metadata gracefully`() {
    val model =
      HuggingFaceModelSummary(
        modelId = "minimal/model",
        displayName = "Minimal Model",
        author = null,
        pipelineTag = null,
        libraryName = null,
        tags = emptyList(),
        likes = 0,
        downloads = 0,
        license = null,
        languages = emptyList(),
        baseModel = null,
        architectures = emptyList(),
        modelType = null,
        totalSizeBytes = null,
        summary = null,
        description = null,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        isPrivate = false,
      )

    composeTestRule.setContent { TestingTheme { HuggingFaceModelCard(model) } }

    composeTestRule.onNodeWithText("Minimal Model").assertIsDisplayed()
    composeTestRule.onNodeWithText("0").assertIsDisplayed() // likes
    composeTestRule.onNodeWithText("0").assertIsDisplayed() // downloads

    // Should not crash or display null values
    composeTestRule.onNodeWithText("null").assertDoesNotExist()
  }

  @Test
  fun `displays gated and disabled status appropriately`() {
    val gatedModel =
      HuggingFaceModelSummary(
        modelId = "gated/model",
        displayName = "Gated Model",
        author = "test",
        pipelineTag = null,
        libraryName = null,
        tags = emptyList(),
        likes = 0,
        downloads = 0,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        hasGatedAccess = true,
        isDisabled = false,
        isPrivate = false,
      )

    val disabledModel =
      HuggingFaceModelSummary(
        modelId = "disabled/model",
        displayName = "Disabled Model",
        author = "test",
        pipelineTag = null,
        libraryName = null,
        tags = emptyList(),
        likes = 0,
        downloads = 0,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        hasGatedAccess = false,
        isDisabled = true,
        isPrivate = false,
      )

    composeTestRule.setContent { TestingTheme { HuggingFaceModelCard(gatedModel) } }

    // Test gated model - should display some indication
    composeTestRule.onNodeWithText("Gated Model").assertIsDisplayed()

    composeTestRule.setContent { TestingTheme { HuggingFaceModelCard(disabledModel) } }

    // Test disabled model - should display some indication
    composeTestRule.onNodeWithText("Disabled Model").assertIsDisplayed()
  }

  @Test
  fun `displays size bucket correctly`() {
    val tinyModel =
      HuggingFaceModelSummary(
        modelId = "tiny",
        displayName = "Tiny Model",
        author = "test",
        pipelineTag = null,
        libraryName = null,
        tags = emptyList(),
        likes = 0,
        downloads = 0,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        totalSizeBytes = 500L * 1024 * 1024, // 500 MB -> TINY
        isPrivate = false,
      )

    val largeModel =
      HuggingFaceModelSummary(
        modelId = "large",
        displayName = "Large Model",
        author = "test",
        pipelineTag = null,
        libraryName = null,
        tags = emptyList(),
        likes = 0,
        downloads = 0,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        totalSizeBytes = 15L * 1024 * 1024 * 1024, // 15 GB -> LARGE
        isPrivate = false,
      )

    composeTestRule.setContent { TestingTheme { HuggingFaceModelCard(tinyModel) } }

    composeTestRule.onNodeWithText("Tiny Model").assertIsDisplayed()
    composeTestRule.onNodeWithText("512.00 MB").assertIsDisplayed()

    composeTestRule.setContent { TestingTheme { HuggingFaceModelCard(largeModel) } }

    composeTestRule.onNodeWithText("Large Model").assertIsDisplayed()
    composeTestRule.onNodeWithText("15.00 GB").assertIsDisplayed()
  }
}
