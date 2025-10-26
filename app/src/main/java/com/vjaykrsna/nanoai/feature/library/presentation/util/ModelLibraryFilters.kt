package com.vjaykrsna.nanoai.feature.library.presentation.util

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelSort
import java.util.Locale

fun List<ModelPackage>.filterBy(filters: LibraryFilterState): List<ModelPackage> {
  return this.applyTextSearch(filters.localSearchQuery.trim())
    .applyProviderFilter(filters.localLibrary)
    .applyPipelineFilter(filters.pipelineTag)
    .applyCapabilityFilter(filters.selectedCapabilities)
    .sortBy(filters.localSort)
}

private fun List<ModelPackage>.applyTextSearch(query: String): List<ModelPackage> {
  if (query.isEmpty()) return this

  val normalized = query.lowercase(Locale.US)
  return filter { model -> matchesTextSearch(model, normalized) }
}

private fun matchesTextSearch(model: ModelPackage, normalizedQuery: String): Boolean {
  return matchesBasicFields(model, normalizedQuery) ||
    matchesMetadataFields(model, normalizedQuery) ||
    matchesTechnicalFields(model, normalizedQuery)
}

private fun matchesBasicFields(model: ModelPackage, query: String): Boolean {
  return model.displayName.lowercase(Locale.US).contains(query) ||
    model.modelId.lowercase(Locale.US).contains(query) ||
    model.capabilities.any { capability -> capability.lowercase(Locale.US).contains(query) } ||
    model.summary?.lowercase(Locale.US)?.contains(query) == true ||
    model.description?.lowercase(Locale.US)?.contains(query) == true
}

private fun matchesMetadataFields(model: ModelPackage, query: String): Boolean {
  return model.author?.lowercase(Locale.US)?.contains(query) == true ||
    model.license?.lowercase(Locale.US)?.contains(query) == true
}

private fun matchesTechnicalFields(model: ModelPackage, query: String): Boolean {
  return model.architectures.any { arch -> arch.lowercase(Locale.US).contains(query) } ||
    model.languages.any { lang -> lang.lowercase(Locale.US).contains(query) } ||
    model.baseModel?.lowercase(Locale.US)?.contains(query) == true ||
    model.modelType?.lowercase(Locale.US)?.contains(query) == true
}

private fun List<ModelPackage>.applyProviderFilter(provider: ProviderType?): List<ModelPackage> {
  return provider?.let { filter { it.providerType == provider } } ?: this
}

private fun List<ModelPackage>.applyCapabilityFilter(
  selectedCapabilities: Set<String>
): List<ModelPackage> {
  return if (selectedCapabilities.isEmpty()) {
    this
  } else {
    val normalizedSelected = selectedCapabilities.map { it.lowercase(Locale.US) }.toSet()
    filter { model ->
      val modelCapabilities = model.capabilities.map { it.lowercase(Locale.US) }.toSet()
      normalizedSelected.all { selected -> modelCapabilities.contains(selected) }
    }
  }
}

private fun List<ModelPackage>.applyPipelineFilter(pipeline: String?): List<ModelPackage> {
  return pipeline?.let { pipelineTag ->
    val normalized = pipelineTag.lowercase(Locale.US)
    filter { model ->
      model.capabilities.any { capability -> capability.lowercase(Locale.US) == normalized }
    }
  } ?: this
}

private fun List<ModelPackage>.sortBy(sort: ModelSort): List<ModelPackage> =
  when (sort) {
    ModelSort.RECOMMENDED ->
      sortedWith(
        compareBy<ModelPackage> { it.installState != InstallState.INSTALLED }
          .thenBy {
            it.installState != InstallState.DOWNLOADING && it.installState != InstallState.PAUSED
          }
          .thenBy { it.displayName.lowercase(Locale.US) }
      )
    ModelSort.NAME -> sortedBy { it.displayName.lowercase(Locale.US) }
    ModelSort.SIZE_DESC -> sortedByDescending(ModelPackage::sizeBytes)
    ModelSort.UPDATED -> sortedByDescending(ModelPackage::updatedAt)
    ModelSort.NEWEST -> sortedByDescending(ModelPackage::createdAt)
    ModelSort.OLDEST -> sortedBy(ModelPackage::createdAt)
  }
