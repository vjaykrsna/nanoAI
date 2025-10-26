package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.data.repository.ConnectivityRepository
import com.vjaykrsna.nanoai.feature.uiux.domain.ConnectivityOperationsUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** ViewModel responsible for connectivity status and banner management. */
class ConnectivityViewModel
@Inject
constructor(
  private val connectivityRepository: ConnectivityRepository,
  private val updateConnectivityUseCase: ConnectivityOperationsUseCase,
  @Suppress("UnusedPrivateProperty")
  @MainImmediateDispatcher
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

  /** Connectivity banner state for displaying offline/online status and actions. */
  val connectivityBannerState: StateFlow<ConnectivityBannerState> =
    connectivityRepository.connectivityBannerState.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = ConnectivityBannerState(status = ConnectivityStatus.ONLINE),
    )

  /** Updates connectivity status and handles online/offline transitions. */
  fun updateConnectivity(status: ConnectivityStatus) {
    updateConnectivityUseCase.updateConnectivity(status)
  }
}
