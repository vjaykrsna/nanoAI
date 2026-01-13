package com.vjaykrsna.nanoai.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Combined cold start and first-hop navigation benchmark aligned to the 1.5s/100ms budgets. */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
  @get:Rule val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun coldStartThenNavigatePrimaryModes() {
    benchmarkRule.measureRepeated(
      packageName = PACKAGE_NAME,
      metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
      compilationMode = CompilationMode.Partial(),
      iterations = ITERATIONS,
      startupMode = StartupMode.COLD,
      setupBlock = { pressHome() },
    ) {
      startActivityAndWait()
      device.wait(Until.hasObject(By.res(PACKAGE_NAME, "home_hub")), WAIT_FOR_HOME_MS)

      openMode("sidebar_nav_chat", By.desc("Chat screen with message history and input"))
      openMode(
        "sidebar_nav_library",
        By.desc("Model library screen with enhanced management controls"),
      )
      openMode(
        "sidebar_nav_settings",
        By.desc("Settings screen with API providers and privacy options"),
      )
    }
  }

  private fun MacrobenchmarkScope.openMode(navResourceId: String, targetSelector: BySelector) {
    val navIcon = device.findObject(By.res(PACKAGE_NAME, "topbar_nav_icon"))
    navIcon?.click()
    device.wait(Until.hasObject(By.res(PACKAGE_NAME, navResourceId)), WAIT_FOR_NAV_ICON_MS)
    device.findObject(By.res(PACKAGE_NAME, navResourceId))?.click()
    device.wait(Until.hasObject(targetSelector), WAIT_FOR_SCREEN_MS)
  }

  companion object {
    private const val PACKAGE_NAME = "com.vjaykrsna.nanoai"
    private const val ITERATIONS = 3
    private const val WAIT_FOR_HOME_MS = 5_000L
    private const val WAIT_FOR_NAV_ICON_MS = 2_000L
    private const val WAIT_FOR_SCREEN_MS = 3_000L
  }
}
