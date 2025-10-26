package com.vjaykrsna.nanoai

import androidx.test.platform.app.InstrumentationRegistry
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@org.junit.jupiter.api.extension.ExtendWith(TestEnvironmentRule::class)
class ExampleInstrumentedTest {
  @Test
  fun useAppContext() {
    // Context of the app under test.
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("com.vjaykrsna.nanoai", appContext.packageName)
  }
}
