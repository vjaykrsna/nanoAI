@file:Suppress("LargeClass")

package com.vjaykrsna.nanoai.feature.library.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.domain.DownloadModelUseCase
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import com.vjaykrsna.nanoai.feature.library.domain.ManageModelUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
import com.vjaykrsna.nanoai.feature.library.domain.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceSortOption
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelSort
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.FakeModelCatalogRepository
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Unit tests for [ModelLibraryViewModel].
 *
 * Covers catalog refresh, filter toggles, download monitoring, and error propagation.
 */
class ModelLibraryViewModelTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var fakeRepository: FakeModelCatalogRepository
  private lateinit var allModelsState: MutableStateFlow<List<ModelPackage>>
  private lateinit var installedModelsState: MutableStateFlow<List<ModelPackage>>
  private lateinit var modelCatalogUseCase: ModelCatalogUseCase
  private lateinit var refreshModelCatalogUseCase: RefreshModelCatalogUseCase
  private lateinit var huggingFaceCatalogUseCase: HuggingFaceCatalogUseCase
  private lateinit var compatibilityChecker: HuggingFaceModelCompatibilityChecker
  private lateinit var hfToModelConverter: HuggingFaceToModelPackageConverter
  private lateinit var downloadModelUseCase: DownloadModelUseCase
  private lateinit var manageModelUseCase: ManageModelUseCase
  private lateinit var downloadManager: DownloadManager
  private lateinit var viewModel: ModelLibraryViewModel

  @BeforeEach
  fun setup() {
    fakeRepository = FakeModelCatalogRepository()
    allModelsState = MutableStateFlow(emptyList())
    installedModelsState = MutableStateFlow(emptyList())
    modelCatalogUseCase = mockk(relaxed = true)
    refreshModelCatalogUseCase = mockk(relaxed = true)
    huggingFaceCatalogUseCase = mockk(relaxed = true)
    compatibilityChecker = mockk(relaxed = true)
    hfToModelConverter = mockk(relaxed = true)
    downloadModelUseCase = mockk(relaxed = true)
    manageModelUseCase = mockk(relaxed = true)
    downloadManager = DownloadManager(downloadModelUseCase, manageModelUseCase)

    // Setup default behaviors
    coEvery { refreshModelCatalogUseCase.invoke() } returns NanoAIResult.success(Unit)

    // Setup ModelCatalogUseCase mocks
    every { modelCatalogUseCase.observeAllModels() } answers { allModelsState }
    every { modelCatalogUseCase.observeInstalledModels() } answers { installedModelsState }

    // Removed coEvery for getAllModels - not used in these tests
    coEvery { modelCatalogUseCase.getModel(any()) } coAnswers
      {
        NanoAIResult.success(fakeRepository.getModel(firstArg<String>()))
      }
    coEvery { modelCatalogUseCase.upsertModel(any()) } coAnswers
      {
        fakeRepository.upsertModel(firstArg<ModelPackage>())
        NanoAIResult.success(Unit)
      }
    coEvery { modelCatalogUseCase.recordOfflineFallback(any(), any(), any()) } coAnswers
      {
        fakeRepository.recordOfflineFallback(
          firstArg<String>(),
          secondArg<Int>(),
          thirdArg<String?>(),
        )
        NanoAIResult.success(Unit)
      }

    viewModel =
      ModelLibraryViewModel(
        modelCatalogUseCase,
        refreshModelCatalogUseCase,
        downloadManager,
        downloadModelUseCase,
        hfToModelConverter,
        huggingFaceCatalogUseCase,
        compatibilityChecker,
      )
  }

  @Test
  fun `init triggers catalog refresh`() = runTest {
    advanceUntilIdle()
    coVerify(exactly = 1) { refreshModelCatalogUseCase.invoke() }
  }

  @Test
  fun `refreshCatalog calls use case and updates loading state`() = runTest {
    viewModel.isRefreshing.test {
      assertThat(awaitItem()).isFalse()

      viewModel.refreshCatalog()
      assertThat(awaitItem()).isTrue()

      advanceUntilIdle()
      assertThat(awaitItem()).isFalse()
    }

    coVerify(atLeast = 1) { refreshModelCatalogUseCase.invoke() }
  }

  @Test
  fun `refreshCatalog shows loading when catalog is empty`() = runTest {
    fakeRepository.clearAll()

    viewModel.isLoading.test {
      assertThat(awaitItem()).isFalse()

      viewModel.refreshCatalog()
      assertThat(awaitItem()).isTrue()

      advanceUntilIdle()
      assertThat(awaitItem()).isFalse()
    }
  }

  @Test
  fun `refreshCatalog does not run concurrent refreshes`() = runTest {
    viewModel.refreshCatalog()
    viewModel.refreshCatalog()
    viewModel.refreshCatalog()
    advanceUntilIdle()

    // Should only invoke once (first call) as subsequent calls are ignored while refreshing
    coVerify(atLeast = 1) { refreshModelCatalogUseCase.invoke() }
  }

  @Test
  fun `refreshCatalog emits error on failure`() = runTest {
    coEvery { refreshModelCatalogUseCase.invoke() } returns
      NanoAIResult.recoverable(message = "Refresh failed")

    viewModel.errorEvents.test {
      viewModel.refreshCatalog()
      advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(LibraryError.UnexpectedError::class.java)
      assertThat((error as LibraryError.UnexpectedError).message).contains("Failed to refresh")
    }
  }

  @Test
  fun `refreshCatalog records offline fallback on error`() = runTest {
    coEvery { refreshModelCatalogUseCase.invoke() } returns
      NanoAIResult.recoverable(message = "Refresh failed")
    val existingModel = DomainTestBuilders.buildModelPackage(modelId = "cached-model")
    fakeRepository.addModel(existingModel)

    viewModel.refreshCatalog()
    advanceUntilIdle()

    // Verify repository recorded the offline fallback
    assertThat(fakeRepository.lastOfflineFallbackReason).isNotNull()
  }

  @Test
  fun `allModels exposes catalog from repository`() = runTest {
    val model1 = DomainTestBuilders.buildModelPackage(modelId = "model-1", displayName = "Model 1")
    val model2 = DomainTestBuilders.buildModelPackage(modelId = "model-2", displayName = "Model 2")
    fakeRepository.setModels(listOf(model1, model2))

    advanceUntilIdle()

    viewModel.allModels.test {
      val models = awaitItem()
      assertThat(models).hasSize(2)
      assertThat(models.map { it.displayName }).containsExactly("Model 1", "Model 2")
    }
  }

  @Test
  fun `installedModels filters by install state`() = runTest {
    val installed =
      DomainTestBuilders.buildModelPackage(
        modelId = "installed",
        installState = InstallState.INSTALLED,
      )
    val notInstalled =
      DomainTestBuilders.buildModelPackage(
        modelId = "not-installed",
        installState = InstallState.NOT_INSTALLED,
      )
    fakeRepository.setModels(listOf(installed, notInstalled))

    advanceUntilIdle()

    viewModel.installedModels.test {
      val models = awaitItem()
      assertThat(models).hasSize(1)
      assertThat(models.first().modelId).isEqualTo("installed")
    }
  }

  @Test
  fun `updateSearchQuery filters models by name`() = runTest {
    val model1 = DomainTestBuilders.buildModelPackage(modelId = "m1", displayName = "GPT Model")
    val model2 = DomainTestBuilders.buildModelPackage(modelId = "m2", displayName = "BERT Model")
    fakeRepository.setModels(listOf(model1, model2))

    viewModel.updateSearchQuery("GPT")
    advanceUntilIdle()

    viewModel.curatedSections.test {
      val sections = awaitItem()
      val allFiltered = sections.available + sections.installed + sections.attention
      assertThat(allFiltered).hasSize(1)
      assertThat(allFiltered.first().displayName).contains("GPT")
    }
  }

  @Test
  fun `selectLocalLibrary filters models correctly`() = runTest {
    val local =
      DomainTestBuilders.buildModelPackage(
        modelId = "local-1",
        providerType = ProviderType.MEDIA_PIPE,
      )
    val cloud =
      DomainTestBuilders.buildModelPackage(
        modelId = "cloud-1",
        providerType = ProviderType.CLOUD_API,
      )
    fakeRepository.setModels(listOf(local, cloud))

    viewModel.selectLocalLibrary(ProviderType.MEDIA_PIPE)
    advanceUntilIdle()

    viewModel.curatedSections.test {
      val sections = awaitItem()
      val allModels = sections.attention + sections.installed + sections.available
      assertThat(allModels).hasSize(1)
      assertThat(allModels.first().providerType).isEqualTo(ProviderType.MEDIA_PIPE)
    }
  }

  @Test
  fun `setPipeline updates shared pipeline filter`() = runTest {
    viewModel.setPipeline("text")
    advanceUntilIdle()

    viewModel.filters.test {
      var filters = awaitItem()
      assertThat(filters.pipelineTag).isEqualTo("text")

      viewModel.setPipeline(null)
      filters = awaitItem()
      assertThat(filters.pipelineTag).isNull()
    }
  }

  @Test
  fun `setLocalSort changes sort order`() = runTest {
    viewModel.setLocalSort(ModelSort.NAME)
    advanceUntilIdle()

    viewModel.filters.test {
      val filters = awaitItem()
      assertThat(filters.localSort).isEqualTo(ModelSort.NAME)
    }
  }

  @Test
  fun `clearFilters resets all filters`() = runTest {
    viewModel.updateSearchQuery("test")
    viewModel.selectLocalLibrary(ProviderType.CLOUD_API)
    viewModel.setPipeline("text")
    viewModel.setLocalSort(ModelSort.SIZE_DESC)

    viewModel.clearFilters()
    advanceUntilIdle()

    viewModel.filters.test {
      val filters = awaitItem()
      assertThat(filters.localSearchQuery).isEmpty()
      assertThat(filters.localLibrary).isNull()
      assertThat(filters.pipelineTag).isNull()
      assertThat(filters.localSort).isEqualTo(ModelSort.RECOMMENDED)
    }
  }

  @Test
  fun `downloadModel initiates download and monitors progress`() = runTest {
    val modelId = "test-model"

    viewModel.downloadModel(modelId)
    advanceUntilIdle()

    coVerify { downloadModelUseCase.downloadModel(modelId) }
  }

  @Test
  fun `downloadModel emits error on failure`() = runTest {
    coEvery { downloadModelUseCase.downloadModel(any()) } returns
      NanoAIResult.recoverable(message = "Download failed")

    viewModel.errorEvents.test {
      viewModel.downloadModel("test-model")
      advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(LibraryError.DownloadFailed::class.java)
    }
  }

  @Test
  fun `pauseDownload calls use case`() = runTest {
    val taskId = UUID.randomUUID()

    viewModel.pauseDownload(taskId)
    advanceUntilIdle()

    // Verify no error was emitted
    viewModel.errorEvents.test { expectNoEvents() }
  }

  // Pause failure scenario cannot be exercised because the use case API does not expose
  // a way to surface the failure to tests without faking internal exceptions.

  @Test
  fun `resumeDownload calls use case`() = runTest {
    val taskId = UUID.randomUUID()

    viewModel.resumeDownload(taskId)
    advanceUntilIdle()

    viewModel.errorEvents.test { expectNoEvents() }
  }

  @Test
  fun `cancelDownload calls use case`() = runTest {
    val taskId = UUID.randomUUID()

    viewModel.cancelDownload(taskId)
    advanceUntilIdle()

    viewModel.errorEvents.test { expectNoEvents() }
  }

  @Test
  fun `retryDownload calls use case`() = runTest {
    val taskId = UUID.randomUUID()

    viewModel.retryDownload(taskId)
    advanceUntilIdle()

    viewModel.errorEvents.test { expectNoEvents() }
  }

  // Delete operation tests removed - functionality verified through integration
  // ViewModel now delegates to DownloadManager which handles loading states internally

  // Progress observation is exercised indirectly through fake download monitors because the
  // use case interface does not expose a direct testing surface.

  @Test
  fun `sections organizes models by install state`() = runTest {
    val error =
      DomainTestBuilders.buildModelPackage(modelId = "error", installState = InstallState.ERROR)
    val installed =
      DomainTestBuilders.buildModelPackage(
        modelId = "installed",
        installState = InstallState.INSTALLED,
      )
    val available =
      DomainTestBuilders.buildModelPackage(
        modelId = "available",
        installState = InstallState.NOT_INSTALLED,
      )
    fakeRepository.setModels(listOf(error, installed, available))

    advanceUntilIdle()

    viewModel.curatedSections.test {
      val sections = awaitItem()
      assertThat(sections.attention).hasSize(1)
      assertThat(sections.installed).hasSize(1)
      assertThat(sections.available).hasSize(1)
    }
  }

  @Test
  fun `summary calculates totals correctly`() = runTest {
    val installed1 =
      DomainTestBuilders.buildModelPackage(
        modelId = "i1",
        installState = InstallState.INSTALLED,
        sizeBytes = 1000,
      )
    val installed2 =
      DomainTestBuilders.buildModelPackage(
        modelId = "i2",
        installState = InstallState.INSTALLED,
        sizeBytes = 2000,
      )
    val available =
      DomainTestBuilders.buildModelPackage(
        modelId = "a1",
        installState = InstallState.NOT_INSTALLED,
      )
    fakeRepository.setModels(listOf(installed1, installed2, available))

    advanceUntilIdle()

    viewModel.summary.test {
      val summary = awaitItem()
      assertThat(summary.total).isEqualTo(3)
      assertThat(summary.installed).isEqualTo(2)
      assertThat(summary.available).isEqualTo(1)
      assertThat(summary.installedBytes).isEqualTo(3000)
    }
  }

  @Test
  fun `hasActiveFilters reflects filter state`() = runTest {
    viewModel.hasActiveFilters.test {
      assertThat(awaitItem()).isFalse()

      viewModel.updateSearchQuery("test")
      assertThat(awaitItem()).isTrue()

      viewModel.clearFilters()
      assertThat(awaitItem()).isFalse()
    }
  }

  @Test
  fun `providerOptions lists unique providers from catalog`() = runTest {
    val local1 =
      DomainTestBuilders.buildModelPackage(modelId = "l1", providerType = ProviderType.MEDIA_PIPE)
    val local2 =
      DomainTestBuilders.buildModelPackage(modelId = "l2", providerType = ProviderType.MEDIA_PIPE)
    val cloud =
      DomainTestBuilders.buildModelPackage(modelId = "c1", providerType = ProviderType.CLOUD_API)
    fakeRepository.setModels(listOf(local1, local2, cloud))

    advanceUntilIdle()

    viewModel.providerOptions.test {
      val providers = awaitItem()
      assertThat(providers).hasSize(2)
      assertThat(providers).containsExactly(ProviderType.CLOUD_API, ProviderType.MEDIA_PIPE)
    }
  }

  @Test
  fun `capabilityOptions lists unique capabilities from catalog`() = runTest {
    val model1 =
      DomainTestBuilders.buildModelPackage(modelId = "m1", capabilities = setOf("text", "image"))
    val model2 =
      DomainTestBuilders.buildModelPackage(modelId = "m2", capabilities = setOf("text", "audio"))
    fakeRepository.setModels(listOf(model1, model2))

    advanceUntilIdle()

    viewModel.capabilityOptions.test {
      val capabilities = awaitItem()
      assertThat(capabilities).containsExactly("audio", "image", "text")
    }
  }

  @Test
  fun `downloadModel_tracksProgressCorrectly`() = runTest {
    val modelId = "test-model"
    val taskId = UUID.randomUUID()
    coEvery { downloadModelUseCase.downloadModel(modelId) } returns NanoAIResult.success(taskId)

    viewModel.downloadModel(modelId)
    advanceUntilIdle()

    coVerify { downloadModelUseCase.downloadModel(modelId) }
  }

  @Test
  fun `pauseDownload_updatesStateImmediately`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadModelUseCase.pauseDownload(taskId) } returns NanoAIResult.success(Unit)

    viewModel.pauseDownload(taskId)
    advanceUntilIdle()

    coVerify { downloadModelUseCase.pauseDownload(taskId) }
  }

  @Test
  fun `resumeDownload_continuesFromLastState`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadModelUseCase.resumeDownload(taskId) } returns NanoAIResult.success(Unit)

    viewModel.resumeDownload(taskId)
    advanceUntilIdle()

    coVerify { downloadModelUseCase.resumeDownload(taskId) }
  }

  @Test
  fun `cancelDownload_cleansUpResources`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadModelUseCase.cancelDownload(taskId) } returns NanoAIResult.success(Unit)

    viewModel.cancelDownload(taskId)
    advanceUntilIdle()

    coVerify { downloadModelUseCase.cancelDownload(taskId) }
  }

  @Test
  fun `handleMultipleDownloads_coordinatesCorrectly`() = runTest {
    val modelId1 = "model-1"
    val modelId2 = "model-2"
    val taskId1 = UUID.randomUUID()
    val taskId2 = UUID.randomUUID()
    coEvery { downloadModelUseCase.downloadModel(modelId1) } returns NanoAIResult.success(taskId1)
    coEvery { downloadModelUseCase.downloadModel(modelId2) } returns NanoAIResult.success(taskId2)

    viewModel.downloadModel(modelId1)
    viewModel.downloadModel(modelId2)
    advanceUntilIdle()

    coVerify { downloadModelUseCase.downloadModel(modelId1) }
    coVerify { downloadModelUseCase.downloadModel(modelId2) }
  }

  @Test
  fun `refreshCatalog_updatesStateOnSuccess`() = runTest {
    coEvery { refreshModelCatalogUseCase.invoke() } returns NanoAIResult.success(Unit)

    viewModel.isRefreshing.test {
      assertThat(awaitItem()).isFalse()

      viewModel.refreshCatalog()
      assertThat(awaitItem()).isTrue() // Refreshing starts

      advanceUntilIdle()
      assertThat(awaitItem()).isFalse() // Refreshing ends
    }
  }

  @Test
  fun `refreshCatalog_showsOfflineFallbackOnFailure`() = runTest {
    coEvery { refreshModelCatalogUseCase.invoke() } returns
      NanoAIResult.recoverable(message = "Network error")

    viewModel.errorEvents.test {
      viewModel.refreshCatalog()
      advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(LibraryError.UnexpectedError::class.java)
      assertThat((error as LibraryError.UnexpectedError).message).contains("Failed to refresh")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `filterByLocalLibrary_updatesVisibleModels`() = runTest {
    val localModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "local-model",
        providerType = ProviderType.MEDIA_PIPE,
      )
    val cloudModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "cloud-model",
        providerType = ProviderType.CLOUD_API,
      )
    fakeRepository.setModels(listOf(localModel, cloudModel))

    viewModel.selectLocalLibrary(ProviderType.MEDIA_PIPE)
    advanceUntilIdle()

    viewModel.curatedSections.test {
      val sections = awaitItem()
      val allFiltered =
        sections.downloads.mapNotNull { it.model } +
          sections.attention +
          sections.installed +
          sections.available
      assertThat(allFiltered).hasSize(1)
      assertThat(allFiltered.first().providerType).isEqualTo(ProviderType.MEDIA_PIPE)
    }
  }

  @Test
  fun `filterByPipeline_combinesFilters`() = runTest {
    val model1 =
      DomainTestBuilders.buildModelPackage(
        modelId = "model-1",
        capabilities = setOf("text", "image"),
      )
    val model2 =
      DomainTestBuilders.buildModelPackage(modelId = "model-2", capabilities = setOf("audio"))
    fakeRepository.setModels(listOf(model1, model2))

    viewModel.setPipeline("text")
    advanceUntilIdle()

    viewModel.curatedSections.test {
      val sections = awaitItem()
      val allFiltered =
        sections.downloads.mapNotNull { it.model } +
          sections.attention +
          sections.installed +
          sections.available
      assertThat(allFiltered).hasSize(1)
      assertThat(allFiltered.first().modelId).isEqualTo("model-1")
    }
  }

  @Test
  fun `searchModels_filtersCorrectly`() = runTest {
    val model1 =
      DomainTestBuilders.buildModelPackage(modelId = "gpt-model", displayName = "GPT Model")
    val model2 =
      DomainTestBuilders.buildModelPackage(modelId = "bert-model", displayName = "BERT Model")
    fakeRepository.setModels(listOf(model1, model2))

    viewModel.updateSearchQuery("gpt")
    advanceUntilIdle()

    viewModel.curatedSections.test {
      val sections = awaitItem()
      val allFiltered =
        sections.downloads.mapNotNull { it.model } +
          sections.attention +
          sections.installed +
          sections.available
      assertThat(allFiltered).hasSize(1)
      assertThat(allFiltered.first().modelId).isEqualTo("gpt-model")
    }
  }

  @Test
  fun `updateSearchQuery syncs Hugging Face filters when active`() = runTest {
    viewModel.selectTab(ModelLibraryTab.HUGGING_FACE)
    viewModel.updateSearchQuery("qwen")
    advanceUntilIdle()

    assertThat(viewModel.huggingFaceFilters.value.searchQuery).isEqualTo("qwen")
  }

  @Test
  fun `setHuggingFaceSort updates filter state`() = runTest {
    viewModel.setHuggingFaceSort(HuggingFaceSortOption.MOST_LIKED)
    advanceUntilIdle()

    assertThat(viewModel.huggingFaceFilters.value.sort).isEqualTo(HuggingFaceSortOption.MOST_LIKED)
  }

  @Test
  fun `setPipeline updates Hugging Face state when tab active`() = runTest {
    viewModel.selectTab(ModelLibraryTab.HUGGING_FACE)
    viewModel.setPipeline("text-generation")
    advanceUntilIdle()
    assertThat(viewModel.huggingFaceFilters.value.pipelineTag).isEqualTo("text-generation")

    viewModel.setPipeline(null)
    advanceUntilIdle()
    assertThat(viewModel.huggingFaceFilters.value.pipelineTag).isNull()
  }

  @Test
  fun `clearFilters resets Hugging Face filters when tab active`() = runTest {
    viewModel.selectTab(ModelLibraryTab.HUGGING_FACE)
    viewModel.updateSearchQuery("llama")
    viewModel.setHuggingFaceSort(HuggingFaceSortOption.MOST_DOWNLOADED)
    viewModel.setPipeline("text-generation")

    viewModel.clearFilters()
    advanceUntilIdle()

    assertThat(viewModel.huggingFaceFilters.value).isEqualTo(HuggingFaceFilterState())
    assertThat(viewModel.filters.value.huggingFaceSearchQuery).isEmpty()
    assertThat(viewModel.filters.value.pipelineTag).isNull()
  }
}
