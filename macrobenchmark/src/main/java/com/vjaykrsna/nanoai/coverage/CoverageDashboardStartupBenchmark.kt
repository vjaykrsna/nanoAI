@file:Suppress("MagicNumber")

package com.vjaykrsna.nanoai.coverage

import android.os.SystemClock
import androidx.benchmark.Outputs
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import kotlin.math.max
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith

private const val PACKAGE_NAME = "com.vjaykrsna.nanoai"
private const val DASHBOARD_WAIT_TIMEOUT_MS = 5_000L
private const val TARGET_MAX_LOAD_MS = 100.0

@RunWith(AndroidJUnit4::class)
class CoverageDashboardStartupBenchmark {

  @get:Rule val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun warmStartup() {
    val iterations = mutableListOf<Double>()

    benchmarkRule.measureRepeated(
      packageName = PACKAGE_NAME,
      metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
      iterations = 8,
      startupMode = StartupMode.WARM,
      compilationMode = CompilationMode.Partial(),
      setupBlock = { pressHome() },
    ) {
      startActivityAndWait()
      waitForHomeSurface()
      val loadMs = navigateToCoverageDashboard()
      iterations += loadMs
      device.pressBack()
      device.waitForIdle()
    }

    val serialized = iterations.joinToString(separator = ",") { "%.2f".format(it) }
    Outputs.writeFile("coverage_dashboard_startup_ms.txt") { file: File ->
      file.writeBytes(serialized.toByteArray())
    }

    val worstMs = iterations.maxOrNull() ?: TARGET_MAX_LOAD_MS
    assertWithMessage(
        "Coverage dashboard warm start exceeded ${TARGET_MAX_LOAD_MS} ms (worst=${"%.2f".format(worstMs)} ms)"
      )
      .that(worstMs)
      .isLessThan(TARGET_MAX_LOAD_MS)
  }

  private fun MacrobenchmarkScope.waitForHomeSurface() {
    device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), DASHBOARD_WAIT_TIMEOUT_MS)
    device.waitForIdle()
  }

  private fun MacrobenchmarkScope.navigateToCoverageDashboard(): Double {
    val startNs = SystemClock.elapsedRealtimeNanos()
    val entry =
      device.findObject(By.descContains("Coverage"))
        ?: device.findObject(By.textContains("Coverage"))
    requireNotNull(entry) { "Unable to locate coverage dashboard navigation affordance" }
    entry.click()
    device.waitForIdle()

    val dashboardVisible =
      device.wait(
        Until.hasObject(By.res(PACKAGE_NAME, "coverage_dashboard_root")),
        DASHBOARD_WAIT_TIMEOUT_MS,
      )

    if (!dashboardVisible) {
      val fallbackVisible =
        device.wait(
          Until.hasObject(By.textContains("Coverage Dashboard")),
          DASHBOARD_WAIT_TIMEOUT_MS,
        )
      require(dashboardVisible || fallbackVisible) {
        "Coverage dashboard never rendered within timeout"
      }
    }

    val durationMs = (SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000.0
    return max(durationMs, 0.0)
  }
}
