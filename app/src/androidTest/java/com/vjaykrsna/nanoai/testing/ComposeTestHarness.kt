package com.vjaykrsna.nanoai.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.vjaykrsna.nanoai.shared.ui.theme.NanoAITheme

/**
 * Compose test harness providing reusable accessibility matchers and test helpers.
 *
 * Usage example:
 * ```
 * val harness = ComposeTestHarness(composeTestRule)
 * harness.assertAccessibilityOf("button") {
 *   contentDescription("Submit form")
 * }
 * ```
 */
class ComposeTestHarness(private val composeTestRule: ComposeTestRule) {
  /** Matches a node by its semantic content description. */
  fun hasContentDescription(description: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(description))

  /** Matches a node that has any content description set. */
  fun hasAnyContentDescription(): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)

  /** Matches a node marked as disabled. */
  fun isDisabled(): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Disabled, Unit)

  /** Matches a node marked as a heading (for TalkBack navigation). */
  fun isHeading(): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit)

  /** Matches a node with progress bar semantics (loading indicators). */
  fun hasProgressBar(): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

  /**
   * Matches a progress bar with indeterminate state (e.g., CircularProgressIndicator without max).
   */
  fun hasIndeterminateProgress(): SemanticsMatcher =
    SemanticsMatcher("has indeterminate progress") { node ->
      val rangeInfo = node.config[SemanticsProperties.ProgressBarRangeInfo]
      rangeInfo == ProgressBarRangeInfo.Indeterminate
    }

  /** Matches a clickable node. */
  fun isClickable(): SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)

  /** Matches a node that has text input semantics. */
  fun isTextInput(): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)

  /** Matches a node with the specified test tag. */
  fun hasTag(tag: String): SemanticsMatcher = hasTestTag(tag)

  /** Asserts accessibility properties of a node identified by test tag. */
  fun assertAccessibilityOf(testTag: String, builder: AccessibilityAssertionBuilder.() -> Unit) {
    val assertions = AccessibilityAssertionBuilder().apply(builder).build()
    val node = composeTestRule.onNode(hasTestTag(testTag))

    assertions.forEach { assertion ->
      when (assertion) {
        is AccessibilityAssertion.ContentDescription -> {
          node.assert(hasContentDescription(assertion.description))
        }
        is AccessibilityAssertion.HasContentDescription -> {
          node.assert(hasAnyContentDescription())
        }
        is AccessibilityAssertion.IsDisabled -> {
          node.assert(isDisabled())
        }
        is AccessibilityAssertion.IsClickable -> {
          node.assert(isClickable())
        }
        is AccessibilityAssertion.IsHeading -> {
          node.assert(isHeading())
        }
      }
    }
  }

  /** Waits for a node to appear with the specified test tag. */
  fun waitForNode(testTag: String, timeoutMillis: Long = 5000) {
    composeTestRule.waitUntil(timeoutMillis) {
      composeTestRule.onAllNodes(hasTestTag(testTag)).fetchSemanticsNodes().isNotEmpty()
    }
  }

  /** Waits for a node to disappear with the specified test tag. */
  fun waitForNodeToDisappear(testTag: String, timeoutMillis: Long = 5000) {
    composeTestRule.waitUntil(timeoutMillis) {
      composeTestRule.onAllNodes(hasTestTag(testTag)).fetchSemanticsNodes().isEmpty()
    }
  }
}

/** Builder for accessibility assertions. */
class AccessibilityAssertionBuilder {
  private val assertions = mutableListOf<AccessibilityAssertion>()

  fun contentDescription(description: String) {
    assertions.add(AccessibilityAssertion.ContentDescription(description))
  }

  fun hasContentDescription() {
    assertions.add(AccessibilityAssertion.HasContentDescription)
  }

  fun isDisabled() {
    assertions.add(AccessibilityAssertion.IsDisabled)
  }

  fun isClickable() {
    assertions.add(AccessibilityAssertion.IsClickable)
  }

  fun isHeading() {
    assertions.add(AccessibilityAssertion.IsHeading)
  }

  internal fun build(): List<AccessibilityAssertion> = assertions
}

/** Sealed class representing different accessibility assertions. */
sealed class AccessibilityAssertion {
  data class ContentDescription(val description: String) : AccessibilityAssertion()

  data object HasContentDescription : AccessibilityAssertion()

  data object IsDisabled : AccessibilityAssertion()

  data object IsClickable : AccessibilityAssertion()

  data object IsHeading : AccessibilityAssertion()
}

/**
 * Simple theme wrapper for Compose tests. Uses the app's theme with default parameters suitable for
 * testing.
 */
@Composable
fun TestingTheme(content: @Composable () -> Unit) {
  NanoAITheme(dynamicColor = false, content = content)
}
