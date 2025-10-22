package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.domain.DownloadModelUseCase
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.feature.library.domain.ModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.domain.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.DownloadManager
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryViewModel
import com.vjaykrsna.nanoai.testing.FakeHuggingFaceCatalogRepository
import com.vjaykrsna.nanoai.testing.FakeModelCatalogRepository
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule

abstract class BaseModelLibraryScreenTest {

  @get:Rule val composeTestRule: ComposeContentTestRule = createComposeRule()
  @get:Rule val testEnvironmentRule = TestEnvironmentRule()

  protected lateinit var catalogRepository: FakeModelCatalogRepository
  protected lateinit var modelCatalogUseCase: ModelCatalogUseCase
  protected lateinit var refreshUseCase: RefreshModelCatalogUseCase
  protected lateinit var huggingFaceCatalogRepository: FakeHuggingFaceCatalogRepository
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
    huggingFaceCatalogRepository = FakeHuggingFaceCatalogRepository()
    downloadManager = mockk(relaxed = true)
    downloadModelUseCase = mockk(relaxed = true)
    hfToModelConverter = mockk(relaxed = true)
    compatibilityChecker = mockk(relaxed = true)

    coEvery { refreshUseCase.invoke() } returns NanoAIResult.success(Unit)

    viewModel =
      ModelLibraryViewModel(
        modelCatalogRepository = catalogRepository,
        modelCatalogUseCase = modelCatalogUseCase,
        refreshModelCatalogUseCase = refreshUseCase,
        downloadManager = downloadManager,
        downloadModelUseCase = downloadModelUseCase,
        hfToModelConverter = hfToModelConverter,
        huggingFaceCatalogRepository = huggingFaceCatalogRepository,
        compatibilityChecker = compatibilityChecker,
      )
  }

  protected fun renderModelLibraryScreen() {
    composeTestRule.setContent { ModelLibraryScreen(viewModel = viewModel) }
    composeTestRule.waitForIdle()
  }
}
