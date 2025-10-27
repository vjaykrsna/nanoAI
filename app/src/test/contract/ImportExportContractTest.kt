package com.vjaykrsna.nanoai.contracts

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Before
import org.junit.Test

/**
 * Contract guard rails for the import/export backup format defined in test resources
 * contracts/import-export-openapi.yaml.
 *
 * These tests purposefully assert on the documented schema details before the implementation has
 * been aligned. They are expected to fail until the export bundle and sample payload include the
 * required metadata and sections.
 */
class ImportExportContractTest {
  private val json = Json { ignoreUnknownKeys = false }
  private lateinit var contractsDir: File
  private lateinit var sampleBackup: JsonObject

  @Before
  fun setup() {
    // Contract files are in test resources
    val classLoader = javaClass.classLoader
    assertThat(classLoader).isNotNull()
    val sampleUrl = classLoader!!.getResource("contracts/sample-backup.json")
    assertThat(sampleUrl).isNotNull()
    val sampleFile = File(sampleUrl!!.toURI().path ?: error("URI path is null"))
    assertThat(sampleFile.exists()).isTrue()
    sampleBackup = json.parseToJsonElement(sampleFile.readText()).jsonObject
    contractsDir = sampleFile.parentFile
  }

  @Test
  fun `export bundle should expose required metadata`() {
    val requiredKeys =
      listOf(
        "version",
        "timestamp",
        "device",
        "conversations",
        "personas",
        "apiProviders",
        "privacy",
        "modelCatalog",
      )
    requiredKeys.forEach { key -> assertThat(sampleBackup.containsKey(key)).isTrue() }
  }

  @Test
  fun `api provider entries should satisfy contract schema`() {
    val providers = sampleBackup.getArray("apiProviders")
    assertThat(providers).isNotNull()
    assertThat(providers!!.isEmpty()).isFalse()

    val provider = providers.first().jsonObject
    val requiredProviderKeys =
      listOf("id", "name", "baseUrl", "apiKey", "isDefault", "status", "quotaResetAt", "createdAt")
    requiredProviderKeys.forEach { key -> assertThat(provider.containsKey(key)).isTrue() }
  }

  @Test
  fun `openapi contract should include disclaimer acknowledgement endpoint`() {
    val contractFile = File(contractsDir, "import-export-openapi.yaml")
    assertThat(contractFile.exists()).isTrue()

    val content = contractFile.readText()
    assertThat(content).contains("/settings/disclaimer/acknowledge")
    assertThat(content).contains("summary: Record that user acknowledged disclaimer")
  }

  private fun JsonObject.getArray(key: String): JsonArray? = this[key]?.jsonArray
}
