package com.vjaykrsna.nanoai.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceSortOption
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.model.toQuery
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val HUGGING_FACE_SEARCH_DEBOUNCE_MS = 350L

class HuggingFaceLibraryViewModel
@Inject
constructor(
  private val huggingFaceCatalogUseCase: HuggingFaceCatalogUseCase,
  private val compatibilityChecker: HuggingFaceModelCompatibilityChecker,
) : ViewModel() {

  private val _models = MutableStateFlow<List<HuggingFaceModelSummary>>(emptyList())
  val models: StateFlow<List<HuggingFaceModelSummary>> = _models.asStateFlow()

  private val _filters = MutableStateFlow(HuggingFaceFilterState())
  val filters: StateFlow<HuggingFaceFilterState> = _filters.asStateFlow()

  val pipelineOptions: StateFlow<List<String>> =
    models
      .map { models ->
        models
          .mapNotNull { it.pipelineTag?.takeIf(String::isNotBlank) }
          .distinctBy { it.lowercase(Locale.US) }
          .sortedBy { it.lowercase(Locale.US) }
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val libraryOptions: StateFlow<List<String>> =
    models
      .map { models ->
        models
          .mapNotNull { it.libraryName?.takeIf(String::isNotBlank) }
          .distinctBy { it.lowercase(Locale.US) }
          .sortedBy { it.lowercase(Locale.US) }
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val downloadableModelIds: StateFlow<Set<String>> =
    models
      .map { models ->
        models
          .filter { compatibilityChecker.checkCompatibility(it) != null }
          .map { it.modelId }
          .toSet()
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorEvents = MutableSharedFlow<LibraryError>()
  val errorEvents = _errorEvents.asSharedFlow()

  private val _downloadRequests = MutableSharedFlow<HuggingFaceModelSummary>()
  val downloadRequests = _downloadRequests.asSharedFlow()

  private var initialized = false
  private var lastFilters: HuggingFaceFilterState? = null

  init {
    observeFilterChanges()
    // Trigger initial load
    fetchModels(HuggingFaceFilterState(), force = true)
  }

  private fun observeFilterChanges() {
    viewModelScope.launch {
      filters.debounce(HUGGING_FACE_SEARCH_DEBOUNCE_MS).collect { filterState ->
        fetchModels(filterState)
      }
    }
  }

  private fun fetchModels(filters: HuggingFaceFilterState, force: Boolean = false) {
    if (!force && !shouldFetch(filters)) return

    initialized = true
    lastFilters = filters

    viewModelScope.launch {
      _isLoading.value = true
      val query = filters.toQuery()
      val result = huggingFaceCatalogUseCase.listModels(query)
      when (result) {
        is NanoAIResult.Success -> {
          _models.value =
            result.value.map { model -> model.copy(tags = applyTagVisibilityRules(model.tags)) }
        }
        is NanoAIResult.RecoverableError -> {
          _errorEvents.emit(LibraryError.HuggingFaceLoadFailed(result.message))
        }
        is NanoAIResult.FatalError -> {
          _errorEvents.emit(LibraryError.HuggingFaceLoadFailed(result.message))
        }
      }
      _isLoading.value = false
    }
  }

  private fun shouldFetch(filters: HuggingFaceFilterState): Boolean {
    if (!initialized) return true
    return lastFilters != filters
  }

  fun updateSearchQuery(query: String) {
    _filters.update { it.copy(searchQuery = query) }
  }

  fun setPipeline(pipelineTag: String?) {
    _filters.update { it.copy(pipelineTag = pipelineTag) }
  }

  fun setSort(sort: HuggingFaceSortOption) {
    _filters.update { it.copy(sort = sort) }
  }

  fun setLibrary(library: String?) {
    _filters.update { it.copy(library = library) }
  }

  fun clearFilters() {
    _filters.value = HuggingFaceFilterState()
  }

  fun requestDownload(model: HuggingFaceModelSummary) {
    viewModelScope.launch { _downloadRequests.emit(model) }
  }

  private fun applyTagVisibilityRules(rawTags: Collection<String>): List<String> {
    if (rawTags.isEmpty()) return emptyList()
    val normalized = rawTags.map { it.trim() }.filter { it.isNotEmpty() }
    val hasMultimodal = normalized.any { it.equals("multimodal", ignoreCase = true) }
    return normalized.filterNot { hasMultimodal && it.equals("text-generation", ignoreCase = true) }
  }

  private fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
  }
}
