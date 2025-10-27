package com.vjaykrsna.nanoai.feature.library.presentation.model

import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceSortDirection
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceSortField
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryTab

data class LibraryDownloadItem(val task: DownloadTask, val model: ModelPackage?)

data class LibraryFilterState(
  val tab: ModelLibraryTab = ModelLibraryTab.LOCAL,
  val localSearchQuery: String = "",
  val huggingFaceSearchQuery: String = "",
  val pipelineTag: String? = null,
  val localSort: ModelSort = ModelSort.RECOMMENDED,
  val localLibrary: ProviderType? = null,
  val selectedCapabilities: Set<String> = emptySet(),
  val huggingFaceSort: HuggingFaceSortOption = HuggingFaceSortOption.TRENDING,
  val huggingFaceLibrary: String? = null,
) {
  fun currentSearchQuery(): String =
    when (tab) {
      ModelLibraryTab.HUGGING_FACE -> huggingFaceSearchQuery
      else -> localSearchQuery
    }

  fun hasActiveFiltersFor(targetTab: ModelLibraryTab = tab): Boolean {
    if (targetTab == ModelLibraryTab.HUGGING_FACE) {
      return huggingFaceSearchQuery.isNotBlank() ||
        pipelineTag != null ||
        huggingFaceLibrary != null ||
        huggingFaceSort != HuggingFaceSortOption.TRENDING
    } else {
      return localSearchQuery.isNotBlank() ||
        pipelineTag != null ||
        localLibrary != null ||
        selectedCapabilities.isNotEmpty() ||
        localSort != ModelSort.RECOMMENDED
    }
  }

  fun activeFilterCountFor(targetTab: ModelLibraryTab = tab): Int {
    var count = 0
    if (pipelineTag != null) count++
    when (targetTab) {
      ModelLibraryTab.HUGGING_FACE -> {
        if (huggingFaceLibrary != null) count++
        if (huggingFaceSort != HuggingFaceSortOption.TRENDING) count++
      }
      else -> {
        if (localLibrary != null) count++
        if (selectedCapabilities.isNotEmpty()) count++
        if (localSort != ModelSort.RECOMMENDED) count++
      }
    }
    return count
  }

  val hasActiveFilters: Boolean
    get() = hasActiveFiltersFor(tab)

  val activeFilterCount: Int
    get() = activeFilterCountFor(tab)
}

internal fun LibraryFilterState.toHuggingFaceFilterState(): HuggingFaceFilterState =
  HuggingFaceFilterState(
    searchQuery = huggingFaceSearchQuery,
    sort = huggingFaceSort,
    pipelineTag = pipelineTag,
    library = huggingFaceLibrary,
    includePrivate = false,
  )

data class ModelLibrarySections(
  val downloads: List<LibraryDownloadItem> = emptyList(),
  val attention: List<ModelPackage> = emptyList(),
  val installed: List<ModelPackage> = emptyList(),
  val available: List<ModelPackage> = emptyList(),
)

data class ModelLibraryTabSections(
  val local: ModelLibrarySections = ModelLibrarySections(),
  val curated: ModelLibrarySections = ModelLibrarySections(),
)

data class ModelLibrarySummary(
  val total: Int = 0,
  val installed: Int = 0,
  val attention: Int = 0,
  val available: Int = 0,
  val installedBytes: Long = 0,
)

enum class ModelSort {
  RECOMMENDED,
  NAME,
  SIZE_DESC,
  UPDATED,
  // Enhanced sorting options for consistency with HuggingFace models
  NEWEST,
  OLDEST,
}

enum class HuggingFaceSortOption(
  val sortField: HuggingFaceSortField?,
  val direction: HuggingFaceSortDirection?,
) {
  TRENDING(null, null),
  MOST_DOWNLOADED(HuggingFaceSortField.DOWNLOADS, HuggingFaceSortDirection.DESCENDING),
  MOST_LIKED(HuggingFaceSortField.LIKES, HuggingFaceSortDirection.DESCENDING),
  RECENTLY_UPDATED(HuggingFaceSortField.LAST_MODIFIED, HuggingFaceSortDirection.DESCENDING),
  NEWEST(HuggingFaceSortField.CREATED, HuggingFaceSortDirection.DESCENDING);

  fun label(): String =
    when (this) {
      TRENDING -> "Trending"
      MOST_DOWNLOADED -> "Most downloaded"
      MOST_LIKED -> "Most liked"
      RECENTLY_UPDATED -> "Recently updated"
      NEWEST -> "Newest"
    }
}

data class HuggingFaceFilterState(
  val searchQuery: String = "",
  val sort: HuggingFaceSortOption = HuggingFaceSortOption.TRENDING,
  val pipelineTag: String? = null,
  val library: String? = null,
  val includePrivate: Boolean = false,
  val offset: Int = 0,
) {
  val hasActiveFilters: Boolean
    get() =
      sort != HuggingFaceSortOption.TRENDING ||
        pipelineTag != null ||
        library != null ||
        includePrivate
}
