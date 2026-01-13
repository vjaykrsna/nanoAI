package com.vjaykrsna.nanoai.core.data.uiux

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandCategory
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandPaletteState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowHeightClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowSizeClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowWidthClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload
import com.vjaykrsna.nanoai.core.domain.repository.NavigationRepository
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class NavigationRepositoryImplTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var repository: NavigationRepositoryImpl

  @BeforeEach
  fun setUp() {
    repository = createRepository()
  }

  private fun createRepository(): NavigationRepositoryImpl {
    return NavigationRepositoryImpl(ioDispatcher = mainDispatcherExtension.dispatcher)
  }

  @Test
  fun `showCommandPalette should expose populated state`() = runTest {
    repository.showCommandPalette(PaletteSource.KEYBOARD_SHORTCUT)
    advanceUntilIdle()

    val state = repository.commandPaletteState.first()
    assertThat(state).isInstanceOf(CommandPaletteState::class.java)
    assertThat(state.surfaceTarget).isEqualTo(CommandCategory.MODES)
  }

  @Test
  fun `hideCommandPalette should reset state`() = runTest {
    repository.showCommandPalette(PaletteSource.KEYBOARD_SHORTCUT)
    advanceUntilIdle()

    repository.hideCommandPalette()
    advanceUntilIdle()

    val state = repository.commandPaletteState.first()
    assertThat(state).isEqualTo(CommandPaletteState.Empty)
  }

  @Test
  fun `recordUndoPayload should publish to flow`() = runTest {
    val payload = UndoPayload(actionId = "restore_defaults")
    repository.recordUndoPayload(payload)
    advanceUntilIdle()

    val emitted = repository.undoPayload.first()
    assertThat(emitted).isEqualTo(payload)
  }

  @Test
  fun `updateWindowSizeClass should push new value`() = runTest {
    val compactWidthMediumHeight =
      ShellWindowSizeClass(
        widthSizeClass = ShellWindowWidthClass.COMPACT,
        heightSizeClass = ShellWindowHeightClass.MEDIUM,
      )
    repository.updateWindowSizeClass(compactWidthMediumHeight)
    advanceUntilIdle()

    assertThat(repository.windowSizeClass.first()).isEqualTo(compactWidthMediumHeight)
  }

  @Test
  fun `openMode does not crash when switching modes`() = runTest {
    repository.openMode(ModeId.CHAT)
    advanceUntilIdle()

    assertThat(repository).isInstanceOf(NavigationRepository::class.java)
  }
}
