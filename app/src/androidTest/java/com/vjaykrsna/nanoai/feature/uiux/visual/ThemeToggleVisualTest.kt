package com.vjaykrsna.nanoai.feature.uiux.visual

import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule

/**
 * Visual regression snapshots for the Theme toggle component.
 *
 * Captures both light and dark variants for manual accessibility review. Outputs are stored under
 * `<app internal storage>/files/visual/uiux/` and can be retrieved via adb pull.
 */
@LargeTest
object ThemeToggleVisualTest {

  // Converted to an object because this file only contains utility/test-snapshot helpers
  // and had no instance tests. Removed unused TAG and ROOT_TAG constants (detekt warnings).
  @JvmStatic val environmentRule = TestEnvironmentRule()
}
