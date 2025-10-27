package com.vjaykrsna.nanoai.feature.library.data.daos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabase
import com.vjaykrsna.nanoai.feature.library.data.catalog.DeliveryType
import com.vjaykrsna.nanoai.feature.library.data.catalog.ModelPackageEntity
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.AfterEach

@OptIn(ExperimentalCoroutinesApi::class)
class ModelPackageDaoTest {

  private lateinit var context: Context
  private lateinit var database: NanoAIDatabase
  private lateinit var readDao: ModelPackageReadDao
  private lateinit var writeDao: ModelPackageWriteDao

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database =
      Room.inMemoryDatabaseBuilder(context, NanoAIDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    readDao = database.modelPackageReadDao()
    writeDao = database.modelPackageWriteDao()
  }

  @AfterEach
  fun tearDown() {
    database.close()
  }

  @Test
  fun insertAndGetById_returnsStoredEntity() = runTest {
    val entity = buildEntity(modelId = "model-insert")

    writeDao.insert(entity)

    val stored = readDao.getById("model-insert")
    assertThat(stored).isEqualTo(entity)
  }

  @Test
  fun replaceCatalog_replacesExistingEntries() = runTest {
    val original = buildEntity(modelId = "original")
    val kept = buildEntity(modelId = "kept")
    writeDao.insert(original)
    writeDao.insert(kept)

    val replacement = buildEntity(modelId = "replacement")
    writeDao.replaceCatalog(listOf(replacement))

    val all = readDao.getAll()
    assertThat(all.map { it.modelId }).containsExactly("replacement")
  }

  @Test
  fun updateInstallState_updatesStateAndTimestamp() = runTest {
    val entity = buildEntity(modelId = "update-state", installState = InstallState.NOT_INSTALLED)
    writeDao.insert(entity)
    val newTime = Instant.parse("2025-03-03T00:00:00Z")

    writeDao.updateInstallState("update-state", InstallState.INSTALLED, newTime)

    val stored = readDao.getById("update-state")
    assertThat(stored?.installState).isEqualTo(InstallState.INSTALLED)
    assertThat(stored?.updatedAt).isEqualTo(newTime)
  }

  @Test
  fun updateDownloadTaskId_persistsIdentifier() = runTest {
    val entity = buildEntity(modelId = "task", downloadTaskId = null)
    writeDao.insert(entity)
    val taskId = UUID.randomUUID().toString()
    val updatedAt = Instant.parse("2025-04-04T00:00:00Z")

    writeDao.updateDownloadTaskId("task", taskId, updatedAt)

    val stored = readDao.getById("task")
    assertThat(stored?.downloadTaskId).isEqualTo(taskId)
    assertThat(stored?.updatedAt).isEqualTo(updatedAt)
  }

  @Test
  fun updateIntegrityMetadata_updatesChecksumAndSignature() = runTest {
    val entity = buildEntity(modelId = "checksum", checksum = "old", signature = null)
    writeDao.insert(entity)
    val updatedAt = Instant.parse("2025-05-05T00:00:00Z")

    writeDao.updateIntegrityMetadata("checksum", "new", "sig", updatedAt)

    val stored = readDao.getById("checksum")
    assertThat(stored?.checksumSha256).isEqualTo("new")
    assertThat(stored?.signature).isEqualTo("sig")
    assertThat(stored?.updatedAt).isEqualTo(updatedAt)
  }

  @Test
  fun observeById_emitsLatestEntity() = runTest {
    val entity = buildEntity(modelId = "observe")
    writeDao.insert(entity)

    val flow = readDao.observeById("observe")
    val initial = flow.first()
    assertThat(initial?.modelId).isEqualTo("observe")

    writeDao.updateInstallState(
      "observe",
      InstallState.INSTALLED,
      Instant.parse("2025-06-06T00:00:00Z"),
    )
    val updated = flow.first()
    assertThat(updated?.installState).isEqualTo(InstallState.INSTALLED)
  }

  private fun buildEntity(
    modelId: String,
    installState: InstallState = InstallState.NOT_INSTALLED,
    downloadTaskId: String? = "task-$modelId",
    checksum: String? = "checksum-$modelId",
    signature: String? = "signature-$modelId",
    createdAt: Instant = Instant.parse("2025-01-01T00:00:00Z"),
    updatedAt: Instant = Instant.parse("2025-01-02T00:00:00Z"),
  ): ModelPackageEntity =
    ModelPackageEntity(
      modelId = modelId,
      displayName = "Model $modelId",
      version = "1.0",
      providerType = ProviderType.CLOUD_API,
      deliveryType = DeliveryType.CLOUD_FALLBACK,
      minAppVersion = 1,
      sizeBytes = 1024,
      capabilities = setOf("text"),
      installState = installState,
      downloadTaskId = downloadTaskId,
      manifestUrl = "https://example.com/$modelId",
      checksumSha256 = checksum,
      signature = signature,
      createdAt = createdAt,
      updatedAt = updatedAt,
    )
}
