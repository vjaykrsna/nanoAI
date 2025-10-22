package com.vjaykrsna.nanoai.feature.library.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabase
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.impl.ModelCatalogRepositoryImpl
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.model.catalog.DeliveryType
import com.vjaykrsna.nanoai.model.leap.LeapModelRemoteDataSource
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import io.mockk.mockk
import java.io.File
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ModelCatalogRepositoryImplTest {

  @get:Rule(order = 0) val environmentRule = TestEnvironmentRule()

  private lateinit var context: Context
  private lateinit var database: NanoAIDatabase
  private lateinit var repository: ModelCatalogRepositoryImpl
  private lateinit var leapModelRemoteDataSource: LeapModelRemoteDataSource

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database =
      Room.inMemoryDatabaseBuilder(context, NanoAIDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    leapModelRemoteDataSource = mockk(relaxed = true)
    repository =
      ModelCatalogRepositoryImpl(
        database.modelPackageReadDao(),
        database.modelPackageWriteDao(),
        database.chatThreadDao(),
        leapModelRemoteDataSource,
        context,
        kotlinx.datetime.Clock.System,
      )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun replaceCatalog_preservesExistingIntegrityAndState() = runTest {
    val existing =
      sampleModel(
        modelId = "model-integrity",
        installState = InstallState.INSTALLED,
        attributes =
          SampleModelAttributes(
            downloadTaskId = UUID.fromString("f8f0b23e-1111-4e75-9f6a-2fd2e3c4f5a6"),
            checksum = "existing-checksum",
            signature = "existing-signature",
            updatedAt = Instant.parse("2025-10-08T12:00:00Z"),
          ),
      )
    repository.upsertModel(existing)

    val incoming =
      existing.copy(
        installState = InstallState.NOT_INSTALLED,
        downloadTaskId = null,
        checksumSha256 = "",
        signature = "",
        updatedAt = Instant.parse("2025-10-10T00:00:00Z"),
      )

    repository.replaceCatalog(listOf(incoming))

    val persisted = repository.getModel(existing.modelId)
    assertThat(persisted?.installState).isEqualTo(existing.installState)
    assertThat(persisted?.downloadTaskId).isEqualTo(existing.downloadTaskId)
    assertThat(persisted?.checksumSha256).isEqualTo(existing.checksumSha256)
    assertThat(persisted?.signature).isEqualTo(existing.signature)
    assertThat(persisted?.updatedAt).isEqualTo(Instant.parse("2025-10-10T00:00:00Z"))
  }

  @Test
  fun recordOfflineFallback_updatesStatusFlow() = runTest {
    repository.recordRefreshSuccess(source = "RemoteSource", modelCount = 5)

    repository.recordOfflineFallback(reason = "IOException", cachedCount = 2, message = "503")

    val status =
      repository.observeRefreshStatus().first { candidate -> candidate.lastFallbackReason != null }
    assertThat(status.lastFallbackReason).isEqualTo("IOException")
    assertThat(status.lastFallbackCachedCount).isEqualTo(2)
    assertThat(status.lastFallbackMessage).isEqualTo("503")
    assertThat(status.lastFallbackAt).isNotNull()
    assertThat(status.lastSuccessSource).isEqualTo("RemoteSource")
    assertThat(status.lastSuccessCount).isEqualTo(5)
  }

  @Test
  fun deleteModelFiles_removesAllArtifacts() = runTest {
    val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
    val modelId = "model-delete"
    File(modelsDir, "$modelId.bin").writeText("binary")
    File(modelsDir, "$modelId.tmp").writeText("temp")
    File(modelsDir, "$modelId.metadata").writeText("meta")
    val nestedDir = File(modelsDir, modelId).apply { mkdirs() }
    File(nestedDir, "weights.bin").writeText("weights")

    repository.deleteModelFiles(modelId)

    assertThat(File(modelsDir, "$modelId.bin").exists()).isFalse()
    assertThat(File(modelsDir, "$modelId.tmp").exists()).isFalse()
    assertThat(File(modelsDir, "$modelId.metadata").exists()).isFalse()
    assertThat(nestedDir.exists()).isFalse()
  }

  private fun sampleModel(
    modelId: String,
    installState: InstallState,
    attributes: SampleModelAttributes = SampleModelAttributes(),
  ): ModelPackage =
    ModelPackage(
      modelId = modelId,
      displayName = "Model ${'$'}modelId",
      version = "1.0",
      providerType = ProviderType.CLOUD_API,
      deliveryType = DeliveryType.CLOUD_FALLBACK,
      minAppVersion = 10,
      sizeBytes = 2048,
      capabilities = setOf("text"),
      installState = installState,
      downloadTaskId = attributes.downloadTaskId,
      manifestUrl = "https://example.com/${'$'}modelId",
      checksumSha256 = attributes.checksum,
      signature = attributes.signature,
      createdAt = Instant.parse("2025-10-08T00:00:00Z"),
      updatedAt = attributes.updatedAt,
    )

  private data class SampleModelAttributes(
    val downloadTaskId: UUID? = null,
    val checksum: String = "checksum",
    val signature: String? = null,
    val updatedAt: Instant = Instant.parse("2025-10-08T00:00:00Z"),
  )
}
