package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.domain.DownloadModelUseCase
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.feature.library.domain.ModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.domain.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.DownloadManager
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryViewModel
import com.vjaykrsna.nanoai.testing.FakeModelCatalogRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Before

abstract class BaseModelLibraryScreenTest {

  val composeTestRule: ComposeContentTestRule = createComposeRule()

  protected lateinit var catalogRepository: FakeModelCatalogRepository
  protected lateinit var modelCatalogUseCase: ModelCatalogUseCase
  protected lateinit var refreshUseCase: RefreshModelCatalogUseCase
  protected lateinit var huggingFaceCatalogUseCase: HuggingFaceCatalogUseCase
  protected lateinit var viewModel: ModelLibraryViewModel
  protected lateinit var downloadManager: DownloadManager
  protected lateinit var downloadModelUseCase: DownloadModelUseCase
  protected lateinit var hfToModelConverter: HuggingFaceToModelPackageConverter
  protected lateinit var compatibilityChecker: HuggingFaceModelCompatibilityChecker

  @Before
  fun setUpBase() {
    catalogRepository = FakeModelCatalogRepository()
    modelCatalogUseCase = mockk(relaxed = true)
    refreshUseCase = mockk(relaxed = true)
    huggingFaceCatalogUseCase = mockk(relaxed = true)
    downloadManager = mockk(relaxed = true)
    downloadModelUseCase = mockk(relaxed = true)
    hfToModelConverter = mockk(relaxed = true)
    compatibilityChecker = mockk(relaxed = true)

    coEvery { refreshUseCase.invoke() } returns NanoAIResult.success(Unit)
    coEvery { huggingFaceCatalogUseCase.listModels(any()) } returns
      NanoAIResult.success(emptyList())
    every { hfToModelConverter.convertIfCompatible(any()) } returns null
    every { modelCatalogUseCase.observeAllModels() } returns catalogRepository.observeAllModels()
    every { modelCatalogUseCase.observeInstalledModels() } returns
      catalogRepository.observeInstalledModels()

    viewModel =
      ModelLibraryViewModel(
        modelCatalogUseCase = modelCatalogUseCase,
        refreshModelCatalogUseCase = refreshUseCase,
        downloadManager = downloadManager,
        downloadModelUseCase = downloadModelUseCase,
        hfToModelConverter = hfToModelConverter,
        huggingFaceCatalogUseCase = huggingFaceCatalogUseCase,
        compatibilityChecker = compatibilityChecker,
      )
  }

  protected fun renderModelLibraryScreen() {
    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }
    composeTestRule.waitForIdle()
  }
}
