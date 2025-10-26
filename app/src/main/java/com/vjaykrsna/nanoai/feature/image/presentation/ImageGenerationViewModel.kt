package com.vjaykrsna.nanoai.feature.image.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.feature.image.domain.ImageGalleryUseCase
import com.vjaykrsna.nanoai.feature.image.domain.model.GeneratedImage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * ViewModel for image generation feature.
 *
 * Manages prompt input, positive/negative prompts, image generation state, and error handling.
 */
@HiltViewModel
class ImageGenerationViewModel
@Inject
constructor(
  private val imageGalleryUseCase: ImageGalleryUseCase,
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

  private companion object {
    private const val SIMULATION_DELAY_MS = 2000L
  }

  private val _uiState = MutableStateFlow(ImageGenerationUiState())
  val uiState: StateFlow<ImageGenerationUiState> = _uiState.asStateFlow()

  private val _errorEvents = MutableSharedFlow<ImageGenerationError>()
  val errorEvents = _errorEvents.asSharedFlow()

  fun updatePrompt(prompt: String) {
    _uiState.value = _uiState.value.copy(prompt = prompt)
  }

  fun updateNegativePrompt(negativePrompt: String) {
    _uiState.value = _uiState.value.copy(negativePrompt = negativePrompt)
  }

  fun updateWidth(width: Int) {
    _uiState.value = _uiState.value.copy(width = width)
  }

  fun updateHeight(height: Int) {
    _uiState.value = _uiState.value.copy(height = height)
  }

  fun updateSteps(steps: Int) {
    _uiState.value = _uiState.value.copy(steps = steps)
  }

  fun updateGuidanceScale(scale: Float) {
    _uiState.value = _uiState.value.copy(guidanceScale = scale)
  }

  fun generateImage() {
    viewModelScope.launch(dispatcher) {
      val state = _uiState.value
      if (state.prompt.isBlank()) {
        _errorEvents.emit(ImageGenerationError.ValidationError("Prompt cannot be empty"))
        return@launch
      }

      _uiState.value = state.copy(isGenerating = true, errorMessage = null)

      // TODO: Implement actual image generation logic
      // For now, simulate generation
      kotlinx.coroutines.delay(SIMULATION_DELAY_MS)

      // TODO: Replace with actual generated image path
      val imagePath = "simulated_path_${UUID.randomUUID()}.png"

      // Save generated image with metadata
      val generatedImage =
        GeneratedImage(
          id = UUID.randomUUID(),
          prompt = state.prompt,
          negativePrompt = state.negativePrompt,
          width = state.width,
          height = state.height,
          steps = state.steps,
          guidanceScale = state.guidanceScale,
          filePath = imagePath,
          thumbnailPath = null, // TODO: Generate thumbnail
          createdAt = Clock.System.now(),
        )

      imageGalleryUseCase.saveImage(generatedImage).onFailure {
        // TODO: Handle error
      }

      _uiState.value =
        _uiState.value.copy(
          isGenerating = false,
          generatedImagePath = imagePath,
          errorMessage = "Image generation not yet implemented",
        )
    }
  }

  fun clearImage() {
    _uiState.value = _uiState.value.copy(generatedImagePath = null)
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}

/** UI state for image generation screen. */
data class ImageGenerationUiState(
  val prompt: String = "",
  val negativePrompt: String = "",
  val width: Int = 512,
  val height: Int = 512,
  val steps: Int = 50,
  val guidanceScale: Float = 7.5f,
  val isGenerating: Boolean = false,
  val generatedImagePath: String? = null,
  val errorMessage: String? = null,
)

/** Error states for image generation. */
sealed interface ImageGenerationError {
  data class ValidationError(val message: String) : ImageGenerationError

  data class GenerationError(val message: String) : ImageGenerationError
}
