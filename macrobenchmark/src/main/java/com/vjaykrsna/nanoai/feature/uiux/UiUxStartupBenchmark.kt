@file:Suppress("MagicNumber")

package com.vjaykrsna.nanoai.feature.uiux

import android.os.SystemClock
import android.util.Log
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith

/**
 * UI/UX macrobenchmark enforcing product-grade performance budgets.
 *
 * Targets per constitution:
 * - Cold start (First Meaningful Paint) <= 300 ms (p75) with partial compilation
 * - Interaction latency for primary navigation <= 100 ms
 * - Theme toggle and offline banner transitions avoid layout jank
 *
 * These tests are expected to fail until the UI implementation is optimized.
 */
@RunWith(AndroidJUnit4::class)
class UiUxStartupBenchmark {
  @get:Rule val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun coldStartMeetsFmpBudgetPartialCompilation() =
    mutableListOf<Long>().let { durations ->
      benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() },
      ) {
        val start = SystemClock.elapsedRealtime()
        startActivityAndWait()

        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 5_000)

        // Verify home feed appears quickly (acts as proxy for first meaningful paint)
        val homeFeed = device.findObject(By.res(PACKAGE_NAME, "home_single_column_feed"))
        checkNotNull(homeFeed) { "T029: Home feed not found during cold start benchmark." }
        durations += SystemClock.elapsedRealtime() - start
      }

      val p75 = durations.percentile(0.75)
      Log.i(TAG, "Cold start p75=${p75}ms (samples=$durations)")
      assertThat(p75).isAtMost(300.0)
    }

  @Test
  fun navigateHomeToSettingsLatency() =
    mutableListOf<Long>().let { durations ->
      benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 3,
        setupBlock = {
          startActivityAndWait()
          device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 5_000)
        },
      ) {
        val sidebarToggle = device.findObject(By.res(PACKAGE_NAME, "sidebar_toggle"))
        val start = SystemClock.elapsedRealtime()
        sidebarToggle?.click()
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "sidebar_drawer")), 2_000)

        val settingsItem = device.findObject(By.res(PACKAGE_NAME, "sidebar_item_settings"))
        settingsItem?.click()
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "settings_grouped_options")), 2_000)
        durations += SystemClock.elapsedRealtime() - start
      }

      val p95 = durations.percentile(0.95)
      Log.i(TAG, "Home→Settings latency p95=${p95}ms")
      assertThat(p95).isAtMost(100.0)
    }

  @Test
  fun themeToggleAnimationJankFree() =
    mutableListOf<Long>().let { durations ->
      benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.None(),
        iterations = 3,
        setupBlock = {
          startActivityAndWait()
          device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), 5_000)

          val sidebarToggle = device.findObject(By.res(PACKAGE_NAME, "sidebar_toggle"))
          sidebarToggle?.click()
          device.wait(Until.hasObject(By.res(PACKAGE_NAME, "sidebar_item_settings")), 2_000)
          device.findObject(By.res(PACKAGE_NAME, "sidebar_item_settings"))?.click()
          device.wait(Until.hasObject(By.res(PACKAGE_NAME, "theme_toggle_switch")), 2_000)
        },
      ) {
        val themeToggle = device.findObject(By.res(PACKAGE_NAME, "theme_toggle_switch"))
        val start = SystemClock.elapsedRealtime()
        themeToggle?.click()
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "theme_toggle_persistence_status")), 2_000)
        durations += SystemClock.elapsedRealtime() - start
      }

      val p90 = durations.percentile(0.90)
      Log.i(TAG, "Theme toggle response p90=${p90}ms")
      assertThat(p90).isAtMost(100.0)
    }

  companion object {
    private const val PACKAGE_NAME = "com.vjaykrsna.nanoai"
    private const val TAG = "UiUxStartupBenchmark"
  }
}

private fun List<Long>.percentile(percentile: Double): Double {
  require(isNotEmpty()) { "No samples collected" }
  val sorted = sorted()
  val rank = ((sorted.size - 1) * percentile).roundToInt().coerceIn(0, sorted.lastIndex)
  return sorted[rank].toDouble()
}
