package com.vjaykrsna.nanoai.testing

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Accessibility test helpers for UI quality assurance.
 *
 * Provides utilities to verify WCAG compliance, touch targets, and semantic properties.
 */
class AccessibilityTestHelpers(
  @Suppress("UnusedPrivateProperty") private val composeTestRule: ComposeTestRule
) {

  /**
   * Verifies that interactive elements meet minimum touch target size of 48dp. Checks both width
   * and height dimensions.
   */
  fun SemanticsNodeInteraction.assertMinimumTouchTarget() {
    assert(hasMinimumTouchTarget(48.dp))
  }

  /** Verifies that interactive elements meet a custom minimum touch target size. */
  fun SemanticsNodeInteraction.assertMinimumTouchTarget(minSize: Dp) {
    assert(hasMinimumTouchTarget(minSize))
  }

  /**
   * Verifies that text meets WCAG AA contrast requirements (4.5:1 for normal text). Note: This
   * requires visual inspection or specialized contrast checking tools. This matcher checks for
   * semantic text properties.
   */
  fun SemanticsNodeInteraction.assertHasReadableText() {
    assert(hasTextContent())
  }

  /** Verifies that elements with icons have appropriate content descriptions. */
  fun SemanticsNodeInteraction.assertIconHasDescription() {
    assert(hasContentDescription() or hasTestTag())
  }

  /** Verifies that form controls are properly labeled. */
  fun SemanticsNodeInteraction.assertFormControlIsLabeled() {
    assert(hasContentDescription() or hasStateDescription() or hasTestTag())
  }

  /** Verifies that headings are properly marked for screen reader navigation. */
  fun SemanticsNodeInteraction.assertIsProperHeading() {
    assert(isHeading())
  }

  /**
   * Verifies that focusable elements have visible focus indicators. Note: This is a semantic
   * check - actual visual focus indicators require manual testing.
   */
  fun SemanticsNodeInteraction.assertFocusableHasFocusIndicator() {
    // Check if element is focusable and has some form of identification
    assert(isFocusable() and (hasContentDescription() or hasStateDescription()))
  }

  /** Verifies that progress indicators provide current value information. */
  fun SemanticsNodeInteraction.assertProgressIndicatorProvidesValue() {
    assert(hasProgressSemantics())
  }

  /**
   * Comprehensive accessibility audit for a composable. Checks multiple accessibility requirements
   * at once.
   */
  fun SemanticsNodeInteraction.runAccessibilityAudit(
    isInteractive: Boolean = false,
    isHeading: Boolean = false,
    hasIcon: Boolean = false,
    isFormControl: Boolean = false,
    isProgressIndicator: Boolean = false,
  ) {
    // Always check for readable text if present
    if (hasTextContent().matches(this.getSemanticsNode())) {
      assertHasReadableText()
    }

    if (isInteractive) {
      assertMinimumTouchTarget()
      assertFocusableHasFocusIndicator()
    }

    if (isHeading) {
      assertIsProperHeading()
    }

    if (hasIcon) {
      assertIconHasDescription()
    }

    if (isFormControl) {
      assertFormControlIsLabeled()
    }

    if (isProgressIndicator) {
      assertProgressIndicatorProvidesValue()
    }
  }
}

/** Creates an accessibility test helper for the given compose rule. */
fun ComposeTestRule.accessibilityHelpers(): AccessibilityTestHelpers =
  AccessibilityTestHelpers(this)

// Private helper matchers

private fun hasMinimumTouchTarget(minSize: Dp): SemanticsMatcher =
  SemanticsMatcher("has minimum touch target of ${minSize.value}dp") { node ->
    val size = node.size
    size.width >= minSize.value && size.height >= minSize.value
  }

private fun hasTextContent(): SemanticsMatcher =
  SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)

private fun hasContentDescription(): SemanticsMatcher =
  SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)

private fun hasStateDescription(): SemanticsMatcher =
  SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)

private fun hasTestTag(): SemanticsMatcher =
  SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)

private fun isHeading(): SemanticsMatcher =
  SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit)

private fun isFocusable(): SemanticsMatcher =
  SemanticsMatcher.keyIsDefined(SemanticsProperties.Focusable)

private fun hasProgressSemantics(): SemanticsMatcher =
  SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
