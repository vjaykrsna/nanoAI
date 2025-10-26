package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.domain.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun openMode_updatesActiveMode() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)

      // Set up navigation use case to actually call repository
      coEvery { navigationOperationsUseCase.openMode(any()) } coAnswers
        {
          fakeRepos.navigationRepository.openMode(firstArg())
        }

      val viewModel =
        NavigationViewModel(fakeRepos.navigationRepository, navigationOperationsUseCase)

      viewModel.openMode(ModeId.CHAT)
      advanceUntilIdle()

      val navState = viewModel.navigationState.first { state -> state.activeMode == ModeId.CHAT }
      assertThat(navState.activeMode).isEqualTo(ModeId.CHAT)
      assertThat(navState.leftDrawerState.isOpen).isFalse()
    }

  @Test
  fun toggleRightDrawer_setsPanelAndOpensDrawer() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)

      // Set up navigation use case to actually call repository
      coEvery { navigationOperationsUseCase.toggleRightDrawer(any()) } coAnswers
        {
          fakeRepos.navigationRepository.toggleRightDrawer(firstArg())
        }

      val viewModel =
        NavigationViewModel(fakeRepos.navigationRepository, navigationOperationsUseCase)

      viewModel.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
      advanceUntilIdle()

      val navState =
        viewModel.navigationState.first { state ->
          state.activeRightPanel == RightPanel.MODEL_SELECTOR
        }
      assertThat(navState.rightDrawerState.isOpen).isTrue()
      assertThat(navState.activeRightPanel).isEqualTo(RightPanel.MODEL_SELECTOR)
    }
}
