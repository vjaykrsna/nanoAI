package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Use case for model catalog operations. */
@Singleton
class ModelCatalogUseCase
@Inject
constructor(private val modelCatalogRepository: ModelCatalogRepository) {
  /** Observe all models in the catalog. */
  fun observeAllModels(): Flow<List<ModelPackage>> = modelCatalogRepository.observeAllModels()

  /** Observe installed models in the catalog. */
  fun observeInstalledModels(): Flow<List<ModelPackage>> =
    modelCatalogRepository.observeInstalledModels()

  /** Get all models in the catalog. */
  suspend fun getAllModels(): NanoAIResult<List<ModelPackage>> {
    return try {
      val models = modelCatalogRepository.getAllModels()
      NanoAIResult.success(models)
    } catch (e: Exception) {
      NanoAIResult.recoverable(message = "Failed to get all models", cause = e)
    }
  }

  /** Get a specific model by ID. */
  suspend fun getModel(modelId: String): NanoAIResult<ModelPackage?> {
    return try {
      val model = modelCatalogRepository.getModel(modelId)
      NanoAIResult.success(model)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to get model $modelId",
        cause = e,
        context = mapOf("modelId" to modelId),
      )
    }
  }

  /** Insert or update a model in the catalog. */
  suspend fun upsertModel(model: ModelPackage): NanoAIResult<Unit> {
    return try {
      modelCatalogRepository.upsertModel(model)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to upsert model ${model.modelId}",
        cause = e,
        context = mapOf("modelId" to model.modelId),
      )
    }
  }

  /** Record an offline fallback scenario. */
  suspend fun recordOfflineFallback(
    reason: String,
    cachedCount: Int,
    message: String? = null,
  ): NanoAIResult<Unit> {
    return try {
      modelCatalogRepository.recordOfflineFallback(reason, cachedCount, message)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to record offline fallback",
        cause = e,
        context = mapOf("reason" to reason, "cachedCount" to cachedCount.toString()),
      )
    }
  }
}
