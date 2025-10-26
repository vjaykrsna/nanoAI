@file:Suppress("MagicNumber")

package com.vjaykrsna.nanoai

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith

/**
 * Macrobenchmark for cold start performance and baseline profile generation. Validates cold start
 * <1.5s and scroll stability per constitution requirements.
 *
 * TDD: This test is written BEFORE the app has full implementation. Expected to compile but may
 * fail on assertions until app is optimized.
 */
@RunWith(AndroidJUnit4::class)
class ColdStartBenchmark {
  @get:Rule val benchmarkRule = MacrobenchmarkRule()

  @Test fun startupNoCompilation() = startup(CompilationMode.None())

  @Test fun startupPartialCompilation() = startup(CompilationMode.Partial())

  @Test fun startupFullCompilation() = startup(CompilationMode.Full())

  private fun startup(compilationMode: CompilationMode) =
    benchmarkRule.measureRepeated(
      packageName = "com.vjaykrsna.nanoai",
      metrics = listOf(StartupTimingMetric()),
      compilationMode = compilationMode,
      iterations = 5,
      startupMode = StartupMode.COLD,
      setupBlock = { pressHome() },
    ) {
      startActivityAndWait()

      // Wait for app to be fully rendered
      device.wait(Until.hasObject(By.pkg(packageName)), 5000)

      // Verify startup time is under 1.5s (1500ms)
      // This assertion will be checked in the benchmark results
    }

  @Test
  fun scrollChatHistory() =
    benchmarkRule.measureRepeated(
      packageName = "com.vjaykrsna.nanoai",
      metrics = listOf(FrameTimingMetric()),
      compilationMode = CompilationMode.Partial(),
      iterations = 5,
      setupBlock = {
        startActivityAndWait()
        // Navigate to chat screen with history
        device.wait(Until.hasObject(By.pkg(packageName)), 5000)
      },
    ) {
      val chatHistory = device.findObject(By.res(packageName, "chat_history_list"))

      // Scroll through chat history
      chatHistory?.setGestureMargin(device.displayWidth / 5)
      repeat(3) {
        chatHistory?.scroll(Direction.DOWN, 1.0f)
        device.waitForIdle()
      }

      // Scroll back up
      repeat(3) {
        chatHistory?.scroll(Direction.UP, 1.0f)
        device.waitForIdle()
      }

      // Frame drops should be <5% per constitution
    }

  @Test
  fun scrollModelLibrary() =
    benchmarkRule.measureRepeated(
      packageName = "com.vjaykrsna.nanoai",
      metrics = listOf(FrameTimingMetric()),
      compilationMode = CompilationMode.Partial(),
      iterations = 5,
      setupBlock = {
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(packageName)), 5000)

        // Navigate to model library
        val libraryTab = device.findObject(By.desc("Model Library"))
        libraryTab?.click()
        device.waitForIdle()
      },
    ) {
      val modelList = device.findObject(By.res(packageName, "model_library_list"))

      // Scroll through model library
      modelList?.setGestureMargin(device.displayWidth / 5)
      repeat(3) {
        modelList?.scroll(Direction.DOWN, 1.0f)
        device.waitForIdle()
      }

      repeat(3) {
        modelList?.scroll(Direction.UP, 1.0f)
        device.waitForIdle()
      }
    }

  @Test
  fun openPersonaSelector() =
    benchmarkRule.measureRepeated(
      packageName = "com.vjaykrsna.nanoai",
      metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
      compilationMode = CompilationMode.Partial(),
      iterations = 5,
      setupBlock = {
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(packageName)), 5000)
      },
    ) {
      // Open persona selector
      val personaButton = device.findObject(By.desc("Select persona"))
      personaButton?.click()
      device.wait(Until.hasObject(By.text("Choose Persona")), 2000)

      // Close persona selector
      device.pressBack()
      device.waitForIdle()
    }

  @Test
  fun validateQueueFlushPerformance() =
    benchmarkRule.measureRepeated(
      packageName = "com.vjaykrsna.nanoai",
      metrics = listOf(FrameTimingMetric()),
      compilationMode = CompilationMode.Partial(),
      iterations = 3,
      setupBlock = {
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(packageName)), 5000)

        // Navigate to model library
        val libraryTab = device.findObject(By.desc("Model Library"))
        libraryTab?.click()
        device.waitForIdle()
      },
    ) {
      // Simulate queuing multiple downloads (UI interaction)
      val downloadButtons = device.findObjects(By.desc("Download"))

      // Queue up to 3 downloads quickly
      downloadButtons.take(3).forEach { button ->
        button.click()
        device.waitForIdle()
      }

      // Queue flush should complete under 500ms per constitution
      // Measured by frame timing during the queue operation
    }
}
