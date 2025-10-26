package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.domain.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent
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
class ShellViewModelNavigationTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun openMode_closesDrawersAndHidesPalette() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)

      // Mock sub-ViewModels
      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = mockk<ConnectivityViewModel>(relaxed = true)
      val progressViewModel = mockk<ProgressViewModel>(relaxed = true)
      val themeViewModel = mockk<ThemeViewModel>(relaxed = true)

      // Set up navigation use case to actually call repository
      coEvery { navigationOperationsUseCase.openMode(any()) } coAnswers
        {
          fakeRepos.navigationRepository.openMode(firstArg())
        }

      val viewModel =
        ShellViewModel(
          fakeRepos.navigationRepository,
          navigationViewModel,
          connectivityViewModel,
          progressViewModel,
          themeViewModel,
          dispatcher,
        )

      viewModel.onEvent(ShellUiEvent.ModeSelected(ModeId.CHAT))
      advanceUntilIdle()

      val uiState = viewModel.uiState.first { state -> state.layout.activeMode == ModeId.CHAT }
      assertThat(uiState.layout.activeMode).isEqualTo(ModeId.CHAT)
      assertThat(uiState.layout.isLeftDrawerOpen).isFalse()
      assertThat(uiState.layout.showCommandPalette).isFalse()
    }

  @Test
  fun toggleRightDrawer_setsPanelAndReflectsInState() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)

      // Mock sub-ViewModels
      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = mockk<ConnectivityViewModel>(relaxed = true)
      val progressViewModel = mockk<ProgressViewModel>(relaxed = true)
      val themeViewModel = mockk<ThemeViewModel>(relaxed = true)

      // Set up navigation use case to actually call repository
      coEvery { navigationOperationsUseCase.toggleRightDrawer(any()) } coAnswers
        {
          fakeRepos.navigationRepository.toggleRightDrawer(firstArg())
        }

      val viewModel =
        ShellViewModel(
          fakeRepos.navigationRepository,
          navigationViewModel,
          connectivityViewModel,
          progressViewModel,
          themeViewModel,
          dispatcher,
        )

      viewModel.onEvent(ShellUiEvent.ToggleRightDrawer(RightPanel.MODEL_SELECTOR))
      advanceUntilIdle()

      val uiState =
        viewModel.uiState.first { state ->
          state.layout.activeRightPanel == RightPanel.MODEL_SELECTOR
        }
      assertThat(uiState.layout.isRightDrawerOpen).isTrue()
      assertThat(uiState.layout.activeRightPanel).isEqualTo(RightPanel.MODEL_SELECTOR)
    }
}
