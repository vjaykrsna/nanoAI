@file:Suppress("LongMethod")

package com.vjaykrsna.nanoai.feature.library.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryTab
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryViewModel
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryUiEvent
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.LOADING_INDICATOR_TAG
import kotlinx.coroutines.flow.collectLatest

/**
 * Model library screen for browsing, downloading, and managing AI models.
 *
 * Features:
 * - Tabbed interface for local vs remote models
 * - Search and filtering capabilities
 * - Download progress tracking
 * - Model management (delete, retry downloads)
 * - Pull-to-refresh functionality
 *
 * @param modifier Modifier to apply to the screen
 * @param viewModel ModelLibraryViewModel for managing library state and operations
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ModelLibraryScreen(
  modifier: Modifier = Modifier,
  viewModel: ModelLibraryViewModel = hiltViewModel(),
) {
  val filters by viewModel.filters.collectAsState()
  val summary by viewModel.summary.collectAsState()
  val localSections by viewModel.localSections.collectAsState()
  val curatedSections by viewModel.curatedSections.collectAsState()
  val huggingFaceModels by viewModel.huggingFaceModels.collectAsState()
  val huggingFaceDownloadableModelIds by viewModel.huggingFaceDownloadableModelIds.collectAsState()
  val pipelineOptions by viewModel.pipelineOptions.collectAsState()
  val huggingFaceLibraryOptions by viewModel.huggingFaceLibraryOptions.collectAsState()
  val providerOptions by viewModel.providerOptions.collectAsState()
  val capabilityOptions by viewModel.capabilityOptions.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val isHuggingFaceLoading by viewModel.isHuggingFaceLoading.collectAsState()

  val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }

  val documentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
      uri?.let(viewModel::importLocalModel)
    }

  LaunchedEffect(Unit) {
    viewModel.errorEvents.collectLatest { error ->
      val message =
        when (error) {
          is LibraryError.DownloadFailed -> "Download failed for ${error.modelId}: ${error.message}"
          is LibraryError.DeleteFailed -> "Delete failed for ${error.modelId}: ${error.message}"
          is LibraryError.PauseFailed -> "Pause failed: ${error.message}"
          is LibraryError.ResumeFailed -> "Resume failed: ${error.message}"
          is LibraryError.CancelFailed -> "Cancel failed: ${error.message}"
          is LibraryError.RetryFailed -> "Retry failed: ${error.message}"
          is LibraryError.UnexpectedError -> "Error: ${error.message}"
          is LibraryError.HuggingFaceLoadFailed -> "Hugging Face error: ${error.message}"
        }
      snackbarHostState.showSnackbar(message)
    }
  }

  LaunchedEffect(documentLauncher) {
    viewModel.uiEvents.collectLatest { event ->
      when (event) {
        LibraryUiEvent.RequestLocalModelImport ->
          documentLauncher.launch(
            arrayOf("application/octet-stream", "application/x-tflite", "*/*")
          )
      }
    }
  }

  val pullRefreshState =
    rememberPullRefreshState(refreshing = isRefreshing, onRefresh = viewModel::refreshCatalog)

  Box(
    modifier =
      modifier.fillMaxSize().pullRefresh(pullRefreshState).semantics {
        contentDescription = "Model library screen with enhanced management controls"
      }
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      LibraryHeader(summary = summary)

      ModelLibraryToolbar(
        tab = filters.tab,
        searchQuery = filters.currentSearchQuery(),
        pipelineOptions = pipelineOptions,
        selectedPipeline = filters.pipelineTag,
        localSort = filters.localSort,
        huggingFaceSort = filters.huggingFaceSort,
        localLibraryOptions = providerOptions,
        selectedLocalLibrary = filters.localLibrary,
        huggingFaceLibraryOptions = huggingFaceLibraryOptions,
        selectedHuggingFaceLibrary = filters.huggingFaceLibrary,
        capabilityOptions = capabilityOptions,
        selectedCapabilities = filters.selectedCapabilities,
        activeFilterCount = filters.activeFilterCount,
        onSearchChange = viewModel::updateSearchQuery,
        onPipelineSelect = viewModel::setPipeline,
        onSelectLocalSort = viewModel::setLocalSort,
        onSelectHuggingFaceSort = viewModel::setHuggingFaceSort,
        onSelectLocalLibrary = viewModel::selectLocalLibrary,
        onSelectHuggingFaceLibrary = viewModel::setHuggingFaceLibrary,
        onToggleCapability = viewModel::toggleCapability,
      )

      ModelLibraryTabs(selectedTab = filters.tab, onTabSelect = viewModel::selectTab)

      when (filters.tab) {
        ModelLibraryTab.LOCAL -> {
          if (isLoading) {
            Box(
              modifier = Modifier.fillMaxWidth().weight(1f),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(
                modifier =
                  Modifier.testTag(LOADING_INDICATOR_TAG).semantics {
                    contentDescription = "Loading models"
                  }
              )
            }
          } else {
            ModelLibraryContent(
              modifier = Modifier.weight(1f),
              sections = localSections,
              selectedTab = ModelLibraryTab.LOCAL,
              onDownload = { model -> viewModel.downloadModel(model.modelId) },
              onDelete = { model -> viewModel.deleteModel(model.modelId) },
              onPause = viewModel::pauseDownload,
              onResume = viewModel::resumeDownload,
              onCancel = viewModel::cancelDownload,
              onRetry = viewModel::retryDownload,
              onImportLocalModel = viewModel::requestLocalModelImport,
            )
          }
        }
        ModelLibraryTab.CURATED -> {
          if (isLoading) {
            Box(
              modifier = Modifier.fillMaxWidth().weight(1f),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(
                modifier =
                  Modifier.testTag(LOADING_INDICATOR_TAG).semantics {
                    contentDescription = "Loading curated models"
                  }
              )
            }
          } else {
            ModelLibraryContent(
              modifier = Modifier.weight(1f),
              sections = curatedSections,
              selectedTab = ModelLibraryTab.CURATED,
              onDownload = { model -> viewModel.downloadModel(model.modelId) },
              onDelete = { model -> viewModel.deleteModel(model.modelId) },
              onPause = viewModel::pauseDownload,
              onResume = viewModel::resumeDownload,
              onCancel = viewModel::cancelDownload,
              onRetry = viewModel::retryDownload,
            )
          }
        }
        ModelLibraryTab.HUGGING_FACE -> {
          HuggingFaceLibraryContent(
            modifier = Modifier.weight(1f),
            models = huggingFaceModels,
            isLoading = isHuggingFaceLoading,
            onDownloadModel = viewModel::downloadHuggingFaceModel,
            downloadableModelIds = huggingFaceDownloadableModelIds,
          )
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier =
        Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).semantics {
          contentDescription = "Model library notifications and messages"
        },
    )

    PullRefreshIndicator(
      refreshing = isRefreshing,
      state = pullRefreshState,
      modifier = Modifier.align(Alignment.TopCenter),
    )
  }
}
