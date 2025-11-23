package com.vjaykrsna.nanoai.feature.library.presentation

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.DownloadModelUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
class HuggingFaceDownloadCoordinatorTest {

  private lateinit var coordinator: HuggingFaceDownloadCoordinator
  private lateinit var converter: HuggingFaceToModelPackageConverter
  private lateinit var modelCatalogUseCase: ModelCatalogUseCase
  private lateinit var downloadModelUseCase: DownloadModelUseCase
  private lateinit var emitError: suspend (LibraryError) -> Unit

  private val testSummary =
    HuggingFaceModelSummary(
      modelId = "test/model",
      displayName = "Test Model",
      author = "test",
      pipelineTag = "text-generation",
      libraryName = "tflite",
      tags = emptyList(),
      likes = 0,
      downloads = 0,
      isPrivate = false,
      trendingScore = 0,
      createdAt = null,
      lastModified = null,
    )

  private val testModelPackage =
    ModelPackage(
      modelId = "test/model",
      displayName = "Test Model",
      version = "1.0",
      providerType = ProviderType.MEDIA_PIPE,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 1000L,
      capabilities = emptySet(),
      installState = InstallState.NOT_INSTALLED,
      manifestUrl = "url",
      createdAt = Instant.fromEpochMilliseconds(0),
      updatedAt = Instant.fromEpochMilliseconds(0),
    )

  @BeforeEach
  fun setup() {
    converter = mockk()
    modelCatalogUseCase = mockk()
    downloadModelUseCase = mockk()
    emitError = mockk(relaxed = true)

    coordinator =
      HuggingFaceDownloadCoordinator(
        converter,
        modelCatalogUseCase,
        downloadModelUseCase,
        emitError,
      )
  }

  @Test
  fun `process emits error if model conversion fails`() = runTest {
    coEvery { converter.convertIfCompatible(testSummary) } returns null

    coordinator.process(testSummary)

    val errorSlot = slot<LibraryError>()
    coVerify { emitError(capture(errorSlot)) }

    assertTrue(errorSlot.captured is LibraryError.DownloadFailed)
    assertEquals(
      "Model is not compatible with local runtimes",
      (errorSlot.captured as LibraryError.DownloadFailed).message,
    )
  }

  @Test
  fun `process emits error if model already exists in catalog`() = runTest {
    coEvery { converter.convertIfCompatible(testSummary) } returns testModelPackage
    coEvery { modelCatalogUseCase.getModel(testModelPackage.modelId) } returns
      NanoAIResult.success(testModelPackage)

    coordinator.process(testSummary)

    val errorSlot = slot<LibraryError>()
    coVerify { emitError(capture(errorSlot)) }

    assertTrue(errorSlot.captured is LibraryError.DownloadFailed)
    assertEquals(
      "Model already exists in catalog",
      (errorSlot.captured as LibraryError.DownloadFailed).message,
    )
  }

  @Test
  fun `process emits error if adding to catalog fails`() = runTest {
    coEvery { converter.convertIfCompatible(testSummary) } returns testModelPackage
    coEvery { modelCatalogUseCase.getModel(testModelPackage.modelId) } returns
      NanoAIResult.recoverable("Not found")
    coEvery { modelCatalogUseCase.upsertModel(testModelPackage) } returns
      NanoAIResult.recoverable("DB Error")

    coordinator.process(testSummary)

    val errorSlot = slot<LibraryError>()
    coVerify { emitError(capture(errorSlot)) }

    assertTrue(errorSlot.captured is LibraryError.DownloadFailed)
    assertTrue(
      (errorSlot.captured as LibraryError.DownloadFailed)
        .message
        .contains("Failed to add model to catalog")
    )
  }

  @Test
  fun `process emits error if start download fails`() = runTest {
    coEvery { converter.convertIfCompatible(testSummary) } returns testModelPackage
    coEvery { modelCatalogUseCase.getModel(testModelPackage.modelId) } returns
      NanoAIResult.recoverable("Not found")
    coEvery { modelCatalogUseCase.upsertModel(testModelPackage) } returns NanoAIResult.success(Unit)
    coEvery { downloadModelUseCase.downloadModel(testModelPackage.modelId) } returns
      NanoAIResult.recoverable("Network Error")

    coordinator.process(testSummary)

    val errorSlot = slot<LibraryError>()
    coVerify { emitError(capture(errorSlot)) }

    assertTrue(errorSlot.captured is LibraryError.DownloadFailed)
    assertEquals("Network Error", (errorSlot.captured as LibraryError.DownloadFailed).message)
  }

  @Test
  fun `process succeeds when all steps succeed`() = runTest {
    coEvery { converter.convertIfCompatible(testSummary) } returns testModelPackage
    coEvery { modelCatalogUseCase.getModel(testModelPackage.modelId) } returns
      NanoAIResult.success(null)
    coEvery { modelCatalogUseCase.upsertModel(testModelPackage) } returns NanoAIResult.success(Unit)
    coEvery { downloadModelUseCase.downloadModel(testModelPackage.modelId) } returns
      NanoAIResult.success(UUID.randomUUID())

    coordinator.process(testSummary)

    coVerify(exactly = 0) { emitError(any()) }
    coVerify { downloadModelUseCase.downloadModel(testModelPackage.modelId) }
  }
}
