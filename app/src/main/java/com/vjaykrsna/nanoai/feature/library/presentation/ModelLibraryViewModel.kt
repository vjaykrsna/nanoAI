package com.vjaykrsna.nanoai.feature.library.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.*
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.domain.DownloadModelUseCase
import com.vjaykrsna.nanoai.feature.library.domain.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import com.vjaykrsna.nanoai.feature.library.domain.ModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
import com.vjaykrsna.nanoai.feature.library.domain.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceSortOption
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryDownloadItem
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryUiEvent
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySections
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySummary
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibraryTabSections
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelSort
import com.vjaykrsna.nanoai.feature.library.presentation.util.filterBy
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Download priority constants (lower number = higher priority)
private const val DOWNLOAD_PRIORITY_DOWNLOADING = 0
private const val DOWNLOAD_PRIORITY_PAUSED = 1
private const val DOWNLOAD_PRIORITY_QUEUED = 2
private const val DOWNLOAD_PRIORITY_FAILED = 3
private const val DOWNLOAD_PRIORITY_COMPLETED = 4
private const val DOWNLOAD_PRIORITY_CANCELLED = 5

@Suppress(
  "LargeClass"
) // ViewModel handles complex state management for entire model library feature
@HiltViewModel
class ModelLibraryViewModel
@Inject
constructor(
  private val modelCatalogUseCase: ModelCatalogUseCase,
  private val refreshModelCatalogUseCase: RefreshModelCatalogUseCase,
  private val downloadManager: DownloadManager,
  private val downloadModelUseCase: DownloadModelUseCase,
  private val hfToModelConverter: HuggingFaceToModelPackageConverter,
  private val huggingFaceCatalogUseCase: HuggingFaceCatalogUseCase,
  private val compatibilityChecker: HuggingFaceModelCompatibilityChecker,
) : ViewModel() {

  // Create HuggingFace ViewModel manually since Hilt doesn't allow injecting ViewModels
  private val huggingFaceLibraryViewModel =
    HuggingFaceLibraryViewModel(huggingFaceCatalogUseCase, compatibilityChecker)

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  // Combine refresh and download loading states
  val isLoading: StateFlow<Boolean> =
    combine(isRefreshing, downloadManager.isLoading) { refreshing, downloading ->
        refreshing || downloading
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  private val _errorEvents = MutableSharedFlow<LibraryError>()
  val errorEvents = _errorEvents.asSharedFlow()

  private val _uiEvents = MutableSharedFlow<LibraryUiEvent>()
  val uiEvents = _uiEvents.asSharedFlow()

  private val _filters = MutableStateFlow(LibraryFilterState())
  val filters: StateFlow<LibraryFilterState> = _filters.asStateFlow()

  val allModels: StateFlow<List<ModelPackage>> =
    modelCatalogUseCase
      .observeAllModels()
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val installedModels: StateFlow<List<ModelPackage>> =
    modelCatalogUseCase
      .observeInstalledModels()
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val providerOptions: StateFlow<List<ProviderType>> =
    allModels
      .map { models -> models.map(ModelPackage::providerType).distinct().sortedBy { it.name } }
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val capabilityOptions: StateFlow<List<String>> =
    allModels
      .map { models ->
        models
          .flatMap { it.capabilities }
          .map { it.trim() }
          .filter { it.isNotBlank() }
          .map { it.lowercase(Locale.US) }
          .distinct()
          .sorted()
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  // Delegate HuggingFace functionality to specialized ViewModel
  val huggingFaceModels = huggingFaceLibraryViewModel.models
  val huggingFaceFilters = huggingFaceLibraryViewModel.filters
  val huggingFacePipelineOptions = huggingFaceLibraryViewModel.pipelineOptions
  val huggingFaceLibraryOptions = huggingFaceLibraryViewModel.libraryOptions
  val isHuggingFaceLoading = huggingFaceLibraryViewModel.isLoading
  val huggingFaceDownloadableModelIds = huggingFaceLibraryViewModel.downloadableModelIds

  // Pipeline options from HuggingFace (used across tabs)
  val pipelineOptions = huggingFaceLibraryViewModel.pipelineOptions

  private val downloadTasks: StateFlow<List<DownloadTask>> =
    combine(
        downloadManager.observeDownloadTasks(),
        huggingFaceLibraryViewModel.errorEvents,
        _errorEvents,
      ) { tasks, hfErrors, localErrors ->
        // Merge error flows if needed
        tasks
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val tabSections: StateFlow<ModelLibraryTabSections> =
    combine(allModels, filters, downloadTasks) { models, filterState, downloads ->
        val filtered = models.filterBy(filterState)

        val prioritizedDownloads =
          downloads
            .filter { it.status.isActiveDownload() }
            .sortedWith(
              compareBy<DownloadTask> { downloadPriority(it.status) }
                .thenByDescending { it.progress }
                .thenBy { it.modelId }
            )
        val downloadItems =
          prioritizedDownloads.map { task ->
            val associatedModel = models.firstOrNull { it.modelId == task.modelId }
            LibraryDownloadItem(task = task, model = associatedModel)
          }

        val activeIds = prioritizedDownloads.map { it.modelId }.toSet()

        val localModels =
          filtered.filter { model ->
            model.installState == InstallState.INSTALLED || model.installState == InstallState.ERROR
          }

        val curatedAvailable =
          filtered
            .filter { it.installState == InstallState.NOT_INSTALLED }
            .filterNot { it.modelId in activeIds }
        val curatedInstalled = filtered.filter { it.installState == InstallState.INSTALLED }
        val curatedAttention = filtered.filter { it.installState == InstallState.ERROR }

        ModelLibraryTabSections(
          local =
            ModelLibrarySections(
              downloads = downloadItems,
              attention = localModels.filter { it.installState == InstallState.ERROR },
              installed = localModels.filter { it.installState == InstallState.INSTALLED },
              available = emptyList(),
            ),
          curated =
            ModelLibrarySections(
              downloads = downloadItems,
              attention = curatedAttention,
              installed = curatedInstalled,
              available = curatedAvailable,
            ),
        )
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, ModelLibraryTabSections())

  val localSections: StateFlow<ModelLibrarySections> =
    tabSections
      .map { it.local }
      .stateIn(viewModelScope, SharingStarted.Eagerly, ModelLibrarySections())

  val curatedSections: StateFlow<ModelLibrarySections> =
    tabSections
      .map { it.curated }
      .stateIn(viewModelScope, SharingStarted.Eagerly, ModelLibrarySections())

  val summary: StateFlow<ModelLibrarySummary> =
    combine(allModels, installedModels) { all, installed ->
        val attentionCount = all.count { it.installState == InstallState.ERROR }
        val availableCount = all.count { it.installState == InstallState.NOT_INSTALLED }
        ModelLibrarySummary(
          total = all.size,
          installed = installed.size,
          attention = attentionCount,
          available = availableCount,
          installedBytes = installed.sumOf(ModelPackage::sizeBytes),
        )
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, ModelLibrarySummary())

  val hasActiveFilters: StateFlow<Boolean> =
    filters.map { it.hasActiveFilters }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

  init {
    refreshCatalog()

    // Forward errors from child ViewModels
    viewModelScope.launch {
      huggingFaceLibraryViewModel.errorEvents.collect { error -> _errorEvents.emit(error) }
    }

    viewModelScope.launch {
      downloadManager.errorEvents.collect { error -> _errorEvents.emit(error) }
    }

    // Handle Hugging Face download requests
    viewModelScope.launch {
      huggingFaceLibraryViewModel.downloadRequests.collect { hfModel ->
        handleHuggingFaceDownload(hfModel)
      }
    }
  }

  fun refreshCatalog() {
    if (_isRefreshing.value) return

    _isRefreshing.value = true

    viewModelScope.launch {
      try {
        refreshModelCatalogUseCase().onFailure { error ->
          handleRefreshFailure(error.cause ?: Exception(error.message))
        }
      } finally {
        _isRefreshing.value = false
      }
    }
  }

  // Filter and search functions
  fun updateSearchQuery(query: String) {
    _filters.update { state ->
      when (state.tab) {
        ModelLibraryTab.HUGGING_FACE -> {
          huggingFaceLibraryViewModel.updateSearchQuery(query)
          state.copy(huggingFaceSearchQuery = query)
        }
        else -> state.copy(localSearchQuery = query)
      }
    }
  }

  fun setPipeline(pipelineTag: String?) {
    _filters.update { it.copy(pipelineTag = pipelineTag) }
    huggingFaceLibraryViewModel.setPipeline(pipelineTag)
  }

  fun setLocalSort(sort: ModelSort) {
    _filters.update { it.copy(localSort = sort) }
  }

  fun setHuggingFaceSort(sort: HuggingFaceSortOption) {
    _filters.update { it.copy(huggingFaceSort = sort) }
    huggingFaceLibraryViewModel.setSort(sort)
  }

  fun selectLocalLibrary(providerType: ProviderType?) {
    _filters.update { it.copy(localLibrary = providerType) }
  }

  fun toggleCapability(capability: String) {
    _filters.update { state ->
      val current = state.selectedCapabilities
      val updated =
        if (current.contains(capability)) {
          current - capability
        } else {
          current + capability
        }
      state.copy(selectedCapabilities = updated)
    }
  }

  fun clearSelectedCapabilities() {
    _filters.update { it.copy(selectedCapabilities = emptySet()) }
  }

  fun setHuggingFaceLibrary(library: String?) {
    _filters.update { it.copy(huggingFaceLibrary = library) }
    huggingFaceLibraryViewModel.setLibrary(library)
  }

  fun clearFilters() {
    _filters.update { state ->
      when (state.tab) {
        ModelLibraryTab.HUGGING_FACE -> {
          huggingFaceLibraryViewModel.clearFilters()
          state.copy(
            huggingFaceSearchQuery = "",
            pipelineTag = null,
            huggingFaceSort = HuggingFaceSortOption.TRENDING,
            huggingFaceLibrary = null,
          )
        }
        else ->
          state.copy(
            localSearchQuery = "",
            pipelineTag = null,
            localSort = ModelSort.RECOMMENDED,
            localLibrary = null,
            selectedCapabilities = emptySet(),
          )
      }
    }
  }

  fun selectTab(tab: ModelLibraryTab) {
    _filters.update { state ->
      if (state.tab == tab) {
        state
      } else {
        val newState = state.copy(tab = tab)
        // When switching to Hugging Face tab, sync the search query
        if (tab == ModelLibraryTab.HUGGING_FACE) {
          huggingFaceLibraryViewModel.updateSearchQuery(newState.huggingFaceSearchQuery)
        }
        newState
      }
    }
  }

  // UI event functions
  fun requestLocalModelImport() {
    viewModelScope.launch { _uiEvents.emit(LibraryUiEvent.RequestLocalModelImport) }
  }

  @Suppress("UnusedParameter")
  fun importLocalModel(uri: Uri) {
    viewModelScope.launch {
      _errorEvents.emit(
        LibraryError.UnexpectedError(
          "Manual import isn't available yet. Check curated or Hugging Face tabs for downloads."
        )
      )
    }
  }

  // Download functions using UseCases
  fun downloadModel(modelId: String) {
    viewModelScope.launch {
      downloadModelUseCase.downloadModel(modelId).onFailure { error ->
        _errorEvents.emit(
          LibraryError.DownloadFailed(
            modelId = modelId,
            message = error.message ?: "Failed to start download",
          )
        )
      }
    }
  }

  fun downloadHuggingFaceModel(
    hfModel: com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelSummary
  ) {
    huggingFaceLibraryViewModel.requestDownload(hfModel)
  }

  fun pauseDownload(taskId: java.util.UUID) {
    viewModelScope.launch { downloadModelUseCase.pauseDownload(taskId) }
  }

  fun resumeDownload(taskId: java.util.UUID) {
    viewModelScope.launch { downloadModelUseCase.resumeDownload(taskId) }
  }

  fun cancelDownload(taskId: java.util.UUID) {
    viewModelScope.launch { downloadModelUseCase.cancelDownload(taskId) }
  }

  fun retryDownload(taskId: java.util.UUID) {
    viewModelScope.launch { downloadModelUseCase.retryFailedDownload(taskId) }
  }

  fun deleteModel(modelId: String) {
    downloadManager.deleteModel(modelId)
  }

  fun observeDownloadProgress(taskId: java.util.UUID): StateFlow<Float> =
    downloadModelUseCase
      .getDownloadProgress(taskId)
      .stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

  private suspend fun handleRefreshFailure(error: Throwable) {
    if (error is CancellationException) throw error

    val rawMessage = error.message?.takeIf { it.isNotBlank() }
    val userMessage = buildString {
      append("Failed to refresh model catalog")
      rawMessage?.let { append(": ").append(it) }
    }

    _errorEvents.emit(LibraryError.UnexpectedError(userMessage))

    val cachedCount =
      modelCatalogUseCase.getAllModels().fold(onSuccess = { it.size }, onFailure = { 0 })
    modelCatalogUseCase.recordOfflineFallback(
      reason = error::class.simpleName ?: "UnknownError",
      cachedCount = cachedCount,
      message = rawMessage,
    )
  }

  private fun downloadPriority(status: DownloadStatus): Int =
    when (status) {
      DownloadStatus.DOWNLOADING -> DOWNLOAD_PRIORITY_DOWNLOADING
      DownloadStatus.PAUSED -> DOWNLOAD_PRIORITY_PAUSED
      DownloadStatus.QUEUED -> DOWNLOAD_PRIORITY_QUEUED
      DownloadStatus.FAILED -> DOWNLOAD_PRIORITY_FAILED
      DownloadStatus.COMPLETED -> DOWNLOAD_PRIORITY_COMPLETED
      DownloadStatus.CANCELLED -> DOWNLOAD_PRIORITY_CANCELLED
    }

  @Suppress("ReturnCount")
  private suspend fun handleHuggingFaceDownload(
    hfModel: com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelSummary
  ) {
    try {
      // Convert HF model to ModelPackage
      val modelPackage = hfToModelConverter.convertIfCompatible(hfModel)
      if (modelPackage == null) {
        _errorEvents.emit(
          LibraryError.DownloadFailed(
            modelId = hfModel.modelId,
            message = "Model is not compatible with local runtimes",
          )
        )
        return
      }

      // Check if already exists
      val existingModel =
        modelCatalogUseCase
          .getModel(modelPackage.modelId)
          .fold(onSuccess = { it }, onFailure = { null })
      if (existingModel != null) {
        _errorEvents.emit(
          LibraryError.DownloadFailed(
            modelId = modelPackage.modelId,
            message = "Model already exists in catalog",
          )
        )
        return
      }

      // Add to catalog first
      modelCatalogUseCase.upsertModel(modelPackage).onFailure { error ->
        _errorEvents.emit(
          LibraryError.DownloadFailed(
            modelId = modelPackage.modelId,
            message = "Failed to add model to catalog: ${error.message}",
          )
        )
        return
      }

      // Start download
      val result = downloadModelUseCase.downloadModel(modelPackage.modelId)
      result.onFailure { error ->
        _errorEvents.emit(
          LibraryError.DownloadFailed(
            modelId = modelPackage.modelId,
            message = error.message ?: "Failed to start download",
          )
        )
      }
    } catch (e: Exception) {
      _errorEvents.emit(
        LibraryError.DownloadFailed(
          modelId = hfModel.modelId,
          message = "Unexpected error: ${e.message}",
        )
      )
    }
  }

  private fun DownloadStatus.isActiveDownload(): Boolean =
    this == DownloadStatus.DOWNLOADING ||
      this == DownloadStatus.PAUSED ||
      this == DownloadStatus.QUEUED ||
      this == DownloadStatus.FAILED
}
