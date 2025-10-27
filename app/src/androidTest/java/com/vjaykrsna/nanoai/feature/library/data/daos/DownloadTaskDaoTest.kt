package com.vjaykrsna.nanoai.feature.library.data.daos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabase
import com.vjaykrsna.nanoai.feature.library.data.catalog.DeliveryType
import com.vjaykrsna.nanoai.feature.library.data.catalog.ModelPackageEntity
import com.vjaykrsna.nanoai.feature.library.data.catalog.ModelPackageWriteDao
import com.vjaykrsna.nanoai.feature.library.data.entities.DownloadTaskEntity
import com.vjaykrsna.nanoai.feature.library.domain.DownloadStatus
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
class DownloadTaskDaoTest {

  private lateinit var context: Context
  private lateinit var database: NanoAIDatabase
  private lateinit var dao: DownloadTaskDao
  private lateinit var modelPackageWriteDao: ModelPackageWriteDao

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database =
      Room.inMemoryDatabaseBuilder(context, NanoAIDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    dao = database.downloadTaskDao()
    modelPackageWriteDao = database.modelPackageWriteDao()
  }

  @AfterEach
  fun tearDown() {
    database.close()
  }

  @Test
  fun insertAndGetTaskById_returnsPersistedEntity() = runTest {
    val taskId = UUID.randomUUID().toString()
    val entity = buildEntity(taskId = taskId, status = DownloadStatus.QUEUED)
    insertModelPackage(entity.modelId)

    dao.insert(entity)
    val stored = dao.getById(taskId)

    assertThat(stored).isNotNull()
    assertThat(stored?.status).isEqualTo(DownloadStatus.QUEUED)
  }

  @Test
  fun updateProgress_updatesStoredValues() = runTest {
    val taskId = UUID.randomUUID().toString()
    val entity = buildEntity(taskId = taskId)
    insertModelPackage(entity.modelId)
    dao.insert(entity)

    dao.updateProgress(taskId, progress = 0.5f, bytesDownloaded = 512L)

    val updated = dao.getById(taskId)
    assertThat(updated?.progress).isEqualTo(0.5f)
    assertThat(updated?.bytesDownloaded).isEqualTo(512L)
  }

  @Test
  fun observeQueuedDownloads_emitsLatestQueue() = runTest {
    val queuedOne = buildEntity(status = DownloadStatus.QUEUED)
    val queuedTwo = buildEntity(status = DownloadStatus.QUEUED)
    val active = buildEntity(status = DownloadStatus.DOWNLOADING)
    insertModelPackage(queuedOne.modelId)
    insertModelPackage(queuedTwo.modelId)
    insertModelPackage(active.modelId)
    dao.insert(queuedOne)
    dao.insert(active)

    val flow = dao.observeQueuedDownloads()
    assertThat(flow.first()).hasSize(1)

    dao.insert(queuedTwo)
    assertThat(flow.first().map { it.taskId }).containsAtLeast(queuedOne.taskId, queuedTwo.taskId)
  }

  @Test
  fun deleteByStatus_removesMatchingRows() = runTest {
    val completed = buildEntity(status = DownloadStatus.COMPLETED)
    val failed = buildEntity(status = DownloadStatus.FAILED)
    insertModelPackage(completed.modelId)
    insertModelPackage(failed.modelId)
    dao.insert(completed)
    dao.insert(failed)

    dao.deleteByStatus(DownloadStatus.COMPLETED)

    assertThat(dao.getAll()).hasSize(1)
    assertThat(dao.getAll().first().status).isEqualTo(DownloadStatus.FAILED)
  }

  @Test
  fun updateStatusWithError_persistsErrorMessage() = runTest {
    val taskId = UUID.randomUUID().toString()
    val entity = buildEntity(taskId = taskId, status = DownloadStatus.DOWNLOADING)
    insertModelPackage(entity.modelId)
    dao.insert(entity)

    dao.updateStatusWithError(taskId, DownloadStatus.FAILED, "disk full")

    val stored = dao.getById(taskId)
    assertThat(stored?.status).isEqualTo(DownloadStatus.FAILED)
    assertThat(stored?.errorMessage).isEqualTo("disk full")
  }

  private fun buildEntity(
    taskId: String = UUID.randomUUID().toString(),
    status: DownloadStatus = DownloadStatus.DOWNLOADING,
    started: Instant = Instant.parse("2025-01-01T00:00:00Z"),
  ): DownloadTaskEntity =
    DownloadTaskEntity(
      taskId = taskId,
      modelId = "model-$taskId",
      progress = 0f,
      status = status,
      bytesDownloaded = 0L,
      startedAt = started,
      finishedAt = null,
      errorMessage = null,
    )

  private val defaultTimestamp: Instant = Instant.parse("2025-01-01T00:00:00Z")

  private suspend fun insertModelPackage(modelId: String) {
    val entity =
      ModelPackageEntity(
        modelId = modelId,
        displayName = "Model $modelId",
        version = "1.0",
        providerType = ProviderType.CLOUD_API,
        deliveryType = DeliveryType.CLOUD_FALLBACK,
        minAppVersion = 1,
        sizeBytes = 1024,
        capabilities = setOf("text"),
        installState = InstallState.NOT_INSTALLED,
        downloadTaskId = null,
        manifestUrl = "https://example.com/$modelId",
        checksumSha256 = null,
        signature = null,
        createdAt = defaultTimestamp,
        updatedAt = defaultTimestamp,
      )
    modelPackageWriteDao.insert(entity)
  }
}
