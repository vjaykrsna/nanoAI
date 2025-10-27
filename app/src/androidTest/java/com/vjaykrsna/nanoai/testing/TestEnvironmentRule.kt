package com.vjaykrsna.nanoai.testing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabase
import java.io.File
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Ensures instrumentation tests start from a clean slate by clearing persistent state and
 * re-enabling radios before each run. Keeps Compose flows deterministic across CI and local hosts.
 */
class TestEnvironmentRule : TestRule {
  private val instrumentation by lazy { InstrumentationRegistry.getInstrumentation() }
  private val context: Context by lazy { ApplicationProvider.getApplicationContext() }

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        resetState()
        try {
          base.evaluate()
        } finally {
          enableNetworks()
        }
      }
    }
  }

  private fun resetState() {
    clearDatabases()
    clearDataStore()
    enableNetworks()
  }

  private fun clearDatabases() {
    context.databaseList().forEach { databaseName ->
      runCatching { context.deleteDatabase(databaseName) }
    }

    val roomDir = context.getDatabasePath(NanoAIDatabase.DATABASE_NAME).parentFile
    roomDir
      ?.takeIf { it.exists() }
      ?.listFiles()
      ?.filter { file -> file.name.endsWith("-wal") || file.name.endsWith("-journal") }
      ?.forEach { file -> runCatching { file.delete() } }
  }

  private fun clearDataStore() {
    val dataStoreDir = File(context.filesDir, "datastore")
    if (dataStoreDir.exists()) {
      dataStoreDir.listFiles()?.forEach { file -> runCatching { file.delete() } }
    }
  }

  private fun enableNetworks() {
    val device = UiDevice.getInstance(instrumentation)
    listOf("svc wifi enable", "svc data enable", "cmd connectivity airplane-mode disable")
      .forEach { command -> runCatching { device.executeShellCommand(command) } }
  }
}
