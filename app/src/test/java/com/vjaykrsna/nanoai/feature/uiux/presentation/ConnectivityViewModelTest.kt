package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.domain.ConnectivityOperationsUseCase
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun updateConnectivity_updatesBannerState() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)

      // Set up connectivity operations use case to actually call repository
      every { connectivityOperationsUseCase.updateConnectivity(any()) } answers
        {
          runBlocking { fakeRepos.connectivityRepository.updateConnectivity(firstArg()) }
        }

      val viewModel =
        ConnectivityViewModel(
          fakeRepos.connectivityRepository,
          connectivityOperationsUseCase,
          dispatcher,
        )

      viewModel.updateConnectivity(ConnectivityStatus.ONLINE)
      advanceUntilIdle()

      val bannerState = viewModel.connectivityBannerState.first()
      assertThat(bannerState.status).isEqualTo(ConnectivityStatus.ONLINE)
    }
}
