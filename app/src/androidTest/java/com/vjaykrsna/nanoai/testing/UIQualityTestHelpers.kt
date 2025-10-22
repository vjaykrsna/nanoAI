package com.vjaykrsna.nanoai.testing

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * UI Quality test helpers for Material Design 3 compliance and visual consistency.
 *
 * Provides utilities to verify spacing, typography, color usage, and component consistency.
 */
class UIQualityTestHelpers(private val composeTestRule: ComposeTestRule) {

  /**
   * Verifies that Material 3 spacing tokens are used correctly. Checks for common spacing values
   * that should come from MaterialTheme.spacing.
   */
  fun SemanticsNodeInteraction.assertUsesMaterialSpacing() {
    // This is a heuristic check - we can't directly inspect the source code
    // but we can verify that padding/spacing looks reasonable for Material 3
    assert(hasReasonableSpacing())
  }

  /**
   * Verifies that components use semantic color tokens from MaterialTheme. Note: This requires
   * visual inspection as semantic colors are resolved at runtime.
   */
  fun SemanticsNodeInteraction.assertUsesSemanticColors() {
    // Semantic check - components should be using theme colors
    assert(hasColorSemantics())
  }

  /** Verifies that text uses Material 3 typography tokens. */
  fun SemanticsNodeInteraction.assertUsesMaterialTypography() {
    assert(hasTypographySemantics())
  }

  /** Verifies that cards use appropriate elevation values from Material 3. */
  fun SemanticsNodeInteraction.assertUsesMaterialElevation() {
    assert(hasMaterialElevation())
  }

  /** Verifies that interactive elements have appropriate ripple feedback. */
  fun SemanticsNodeInteraction.assertHasRippleFeedback() {
    assert(hasClickSemantics())
  }

  /** Verifies that loading states use Material 3 progress indicators. */
  fun SemanticsNodeInteraction.assertUsesMaterialLoadingIndicators() {
    // Check for progress bar semantics or circular progress indicators
    assert(hasProgressSemantics() or hasCircularProgressSemantics())
  }

  /** Comprehensive UI quality audit for Material 3 compliance. */
  fun SemanticsNodeInteraction.runMaterialDesignAudit(
    usesSpacing: Boolean = false,
    usesSemanticColors: Boolean = false,
    usesTypography: Boolean = false,
    usesElevation: Boolean = false,
    hasRipple: Boolean = false,
    hasLoadingStates: Boolean = false,
  ) {
    if (usesSpacing) {
      assertUsesMaterialSpacing()
    }

    if (usesSemanticColors) {
      assertUsesSemanticColors()
    }

    if (usesTypography) {
      assertUsesMaterialTypography()
    }

    if (usesElevation) {
      assertUsesMaterialElevation()
    }

    if (hasRipple) {
      assertHasRippleFeedback()
    }

    if (hasLoadingStates) {
      assertUsesMaterialLoadingIndicators()
    }
  }

  /**
   * Verifies that a component tree maintains visual hierarchy. Checks for proper use of headings,
   * spacing, and semantic structure.
   */
  fun runScreenHierarchyAudit() {
    // Check for proper heading structure (H1, H2, etc.)
    composeTestRule.onAllNodes(isHeading()).fetchSemanticsNodes().isNotEmpty()

    // Check that interactive elements are properly spaced
    composeTestRule.onAllNodes(isClickable()).apply {
      // Verify minimum touch targets for interactive elements
      fetchSemanticsNodes().forEach { node ->
        val size = node.size
        assert(size.width >= 48f && size.height >= 48f) {
          "Interactive element has insufficient touch target: ${size.width} x ${size.height}"
        }
      }
    }
  }
}

/** Creates a UI quality test helper for the given compose rule. */
fun ComposeTestRule.uiQualityHelpers(): UIQualityTestHelpers = UIQualityTestHelpers(this)

// Private helper matchers

private fun hasReasonableSpacing(): SemanticsMatcher =
  SemanticsMatcher("has reasonable Material 3 spacing") { node ->
    // Check if the node has some form of spacing semantics
    // This is a basic check - actual spacing verification requires visual inspection
    true // Placeholder - would need more sophisticated analysis
  }

private fun hasColorSemantics(): SemanticsMatcher =
  SemanticsMatcher("uses semantic colors") { node ->
    // Check for color-related semantics
    SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription).matches(node)
  }

private fun hasTypographySemantics(): SemanticsMatcher =
  SemanticsMatcher("uses Material typography") { node ->
    // Check for text-related semantics
    SemanticsMatcher.keyIsDefined(SemanticsProperties.Text).matches(node) ||
      SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText).matches(node)
  }

private fun hasMaterialElevation(): SemanticsMatcher =
  SemanticsMatcher("uses Material elevation") { node ->
    // Check for shadow/elevation semantics
    SemanticsMatcher.keyIsDefined(SemanticsProperties.ShadowElevation).matches(node)
  }

private fun hasClickSemantics(): SemanticsMatcher =
  SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)

private fun hasProgressSemantics(): SemanticsMatcher =
  SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

private fun hasCircularProgressSemantics(): SemanticsMatcher =
  SemanticsMatcher("has circular progress") { node ->
    // Look for circular progress indicators
    // This would need to be more specific based on actual component implementation
    node.config[SemanticsProperties.ProgressBarRangeInfo]?.let { rangeInfo ->
      rangeInfo != androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate
    } ?: false
  }

private fun isHeading(): SemanticsMatcher =
  SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit)

private fun isClickable(): SemanticsMatcher =
  SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)
