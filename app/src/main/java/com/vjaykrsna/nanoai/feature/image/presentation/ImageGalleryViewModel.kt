package com.vjaykrsna.nanoai.feature.image.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.feature.image.domain.ImageGalleryUseCase
import com.vjaykrsna.nanoai.feature.image.domain.model.GeneratedImage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for image gallery screen.
 *
 * Manages gallery state and image deletion.
 */
@HiltViewModel
class ImageGalleryViewModel
@Inject
constructor(private val imageGalleryUseCase: ImageGalleryUseCase) : ViewModel() {

  private companion object {
    private const val STATE_FLOW_STOP_TIMEOUT_MS = 5000L
  }

  val images: StateFlow<List<GeneratedImage>> =
    imageGalleryUseCase
      .observeAllImages()
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STATE_FLOW_STOP_TIMEOUT_MS),
        emptyList(),
      )

  private val _events = MutableSharedFlow<ImageGalleryEvent>()
  val events = _events.asSharedFlow()

  fun deleteImage(id: UUID) {
    viewModelScope.launch {
      imageGalleryUseCase.deleteImage(id).onFailure {
        // TODO: Handle error
      }
      _events.emit(ImageGalleryEvent.ImageDeleted)
    }
  }

  fun deleteAllImages() {
    viewModelScope.launch {
      imageGalleryUseCase.deleteAllImages().onFailure {
        // TODO: Handle error
      }
      _events.emit(ImageGalleryEvent.AllImagesDeleted)
    }
  }
}

/** Events for image gallery screen. */
sealed interface ImageGalleryEvent {
  data object ImageDeleted : ImageGalleryEvent

  data object AllImagesDeleted : ImageGalleryEvent
}
