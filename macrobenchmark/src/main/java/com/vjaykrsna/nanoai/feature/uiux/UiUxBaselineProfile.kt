@file:Suppress("MagicNumber")

package com.vjaykrsna.nanoai.feature.uiux

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith

/**
 * Baseline profile generation focused on the polished UI/UX flows (home → settings).
 *
 * Run via: ./gradlew :macrobenchmark:generateBenchmarkReleaseBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
class UiUxBaselineProfile {
  @get:Rule val baselineRule = BaselineProfileRule()

  @Test
  fun generateUiUxProfile() =
    baselineRule.collect(packageName = PACKAGE_NAME, maxIterations = 12, stableIterations = 3) {
      pressHome()
      startActivityAndWait()

      // Wait for home screen to load from cold start
      device.wait(Until.hasObject(By.res(PACKAGE_NAME, "home_single_column_feed")), 3_000)
      device.wait(Until.hasObject(By.text("Recent actions")), 2_000)

      // Expand tools rail and scroll actions
      device.findObject(By.res(PACKAGE_NAME, "home_tools_toggle"))?.click()
      device.waitForIdle()

      device.findObject(By.res(PACKAGE_NAME, "home_single_column_feed"))?.let { list ->
        list.setGestureMargin(device.displayWidth / 5)
        list.scroll(Direction.DOWN, 0.7f)
        device.waitForIdle()
        list.scroll(Direction.UP, 0.7f)
        device.waitForIdle()
      }

      // Enter settings via navigation drawer and toggle theme
      device.findObject(By.desc("Open navigation drawer"))?.click()
      device.wait(Until.hasObject(By.text("Settings")), 2_000)
      device.findObject(By.text("Settings"))?.click()
      device.wait(Until.hasObject(By.res(PACKAGE_NAME, "theme_toggle_switch")), 2_000)
      device.findObject(By.res(PACKAGE_NAME, "theme_toggle_switch"))?.click()
      device.waitForIdle()

      // Return to home for steady state
      device.pressBack()
      device.wait(Until.hasObject(By.text("Recent actions")), 2_000)
    }

  companion object {
    private const val PACKAGE_NAME = "com.vjaykrsna.nanoai"
  }
}
