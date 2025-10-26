@file:Suppress("MagicNumber")

package com.vjaykrsna.nanoai.macrobenchmark

import android.os.SystemClock
import android.util.Log
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith

/** Macrobenchmark coverage for navigation and mode switching latency budgets. */
@RunWith(AndroidJUnit4::class)
class NavigationBenchmarks {
  @get:Rule val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun homeHubLaunchWithinBudget() =
    mutableListOf<Long>().let { durations ->
      benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        setupBlock = {
          startActivityAndWait()
          device.wait(Until.hasObject(By.res(PACKAGE_NAME, "topbar_nav_icon")), 5_000)

          // Navigate away from home to ensure we're measuring the return interaction.
          device.findObject(By.res(PACKAGE_NAME, "topbar_nav_icon"))?.click()
          device.wait(Until.hasObject(By.res(PACKAGE_NAME, "sidebar_item_settings")), 2_000)
          device.findObject(By.res(PACKAGE_NAME, "sidebar_item_settings"))?.click()
          device.wait(
            Until.hasObject(By.desc("Settings screen with API providers and privacy options")),
            2_000,
          )
        },
      ) {
        val navIcon =
          device.findObject(By.res(PACKAGE_NAME, "topbar_nav_icon"))
            ?: error("Navigation icon not found; ensure compact layout is active.")

        val start = SystemClock.elapsedRealtime()
        navIcon.click()
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "sidebar_nav_home")), 2_000)
        device.findObject(By.res(PACKAGE_NAME, "sidebar_nav_home"))?.click()
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "home_hub")), 2_000)
        durations += SystemClock.elapsedRealtime() - start
      }

      val p90 = durations.percentile(0.90)
      Log.i(TAG, "Home hub launch latency p90=${p90}ms")
      assertThat(p90).isAtMost(INTERACTION_BUDGET_MS)
    }

  @Test
  fun modeSwitchLatencyWithinBudget() =
    mutableListOf<Long>().let { durations ->
      benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        setupBlock = {
          startActivityAndWait()
          device.wait(Until.hasObject(By.res(PACKAGE_NAME, "home_hub")), 5_000)
        },
      ) {
        val chatQuickAction =
          device.findObject(By.desc("New Chat"))
            ?: error("Quick action 'New Chat' not found on Home Hub")

        val start = SystemClock.elapsedRealtime()
        chatQuickAction.click()
        device.wait(Until.hasObject(By.desc("Chat screen with message history and input")), 3_000)
        durations += SystemClock.elapsedRealtime() - start

        // Reset to home for the next iteration.
        val navIcon = device.findObject(By.res(PACKAGE_NAME, "topbar_nav_icon"))
        navIcon?.click()
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "sidebar_nav_home")), 2_000)
        device.findObject(By.res(PACKAGE_NAME, "sidebar_nav_home"))?.click()
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "home_hub")), 2_000)
      }

      val p90 = durations.percentile(0.90)
      Log.i(TAG, "Mode switch latency p90=${p90}ms")
      assertThat(p90).isAtMost(INTERACTION_BUDGET_MS)
    }

  @Test
  fun commandPaletteOpenLatency() =
    mutableListOf<Long>().let { durations ->
      benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        setupBlock = {
          startActivityAndWait()
          device.wait(Until.hasObject(By.res(PACKAGE_NAME, "home_hub")), 5_000)
        },
      ) {
        val commandButton =
          device.findObject(By.res(PACKAGE_NAME, "topbar_command_palette"))
            ?: error("Command palette button not found in top app bar")

        val start = SystemClock.elapsedRealtime()
        commandButton.click()
        device.wait(Until.hasObject(By.res(PACKAGE_NAME, "command_palette")), 2_000)
        durations += SystemClock.elapsedRealtime() - start

        // Dismiss palette to reset state.
        device.pressBack()
        device.wait(Until.gone(By.res(PACKAGE_NAME, "command_palette")), 2_000)
      }

      val p95 = durations.percentile(0.95)
      Log.i(TAG, "Command palette open latency p95=${p95}ms")
      assertThat(p95).isAtMost(INTERACTION_BUDGET_MS)
    }

  companion object {
    private const val PACKAGE_NAME = "com.vjaykrsna.nanoai"
    private const val TAG = "NavigationBenchmarks"
    private const val INTERACTION_BUDGET_MS = 100.0
  }
}

private fun List<Long>.percentile(percentile: Double): Double {
  require(isNotEmpty()) { "No samples collected" }
  val sorted = sorted()
  val rank = ((sorted.size - 1) * percentile).roundToInt().coerceIn(0, sorted.lastIndex)
  return sorted[rank].toDouble()
}
