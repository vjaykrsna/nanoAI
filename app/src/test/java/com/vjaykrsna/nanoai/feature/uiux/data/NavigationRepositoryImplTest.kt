package com.vjaykrsna.nanoai.feature.uiux.data

import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class NavigationRepositoryImplTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var repository: NavigationRepositoryImpl

    private fun createTestSnapshot(
        isLeftDrawerOpen: Boolean = false,
        isRightDrawerOpen: Boolean = false,
        activeRightPanel: String? = null
    ) = UIStateSnapshot(
        userId = "testUser",
        expandedPanels = emptyList(),
        recentActions = emptyList(),
        isSidebarCollapsed = false,
        isLeftDrawerOpen = isLeftDrawerOpen,
        isRightDrawerOpen = isRightDrawerOpen,
        activeRightPanel = activeRightPanel
    )

    @BeforeEach
    fun setUp() {
        userProfileRepository = mockk(relaxed = true)
    }

    private fun createRepository(initialState: UIStateSnapshot): NavigationRepositoryImpl {
        coEvery { userProfileRepository.observeUIStateSnapshot(any()) } returns flowOf(initialState)
        return NavigationRepositoryImpl(
            userProfileRepository = userProfileRepository,
            ioDispatcher = mainDispatcherExtension.dispatcher
        )
    }

    @Test
    fun `openMode should update the active route and close drawers`() = runTest {
        // Given
        repository = createRepository(createTestSnapshot())

        // When
        repository.openMode(ModeId.CHAT)
        advanceUntilIdle()

        // Then
        coVerify { userProfileRepository.updateActiveModeRoute(any(), "chat") }
        coVerify { userProfileRepository.updateLeftDrawerOpen(any(), false) }
        coVerify { userProfileRepository.updateCommandPaletteVisibility(any(), false) }
    }

    @Test
    fun `toggleLeftDrawer should open the drawer when it is closed`() = runTest {
        // Given
        repository = createRepository(createTestSnapshot(isLeftDrawerOpen = false))

        // When
        repository.toggleLeftDrawer()
        advanceUntilIdle()


        // Then
        coVerify { userProfileRepository.updateLeftDrawerOpen(any(), true) }
    }

    @Test
    fun `toggleLeftDrawer should close the drawer when it is open`() = runTest {
        // Given
        repository = createRepository(createTestSnapshot(isLeftDrawerOpen = true))

        // When
        repository.toggleLeftDrawer()
        advanceUntilIdle()

        // Then
        coVerify { userProfileRepository.updateLeftDrawerOpen(any(), false) }
    }

    @Test
    fun `toggleRightDrawer should open the drawer with the correct panel`() = runTest {
        // Given
        repository = createRepository(createTestSnapshot(isRightDrawerOpen = false))

        // When
        repository.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
        advanceUntilIdle()

        // Then
        coVerify { userProfileRepository.updateRightDrawerState(any(), true, "model_selector") }
    }

    @Test
    fun `toggleRightDrawer should close the drawer if it is already open with the same panel`() = runTest {
        // Given
        repository = createRepository(createTestSnapshot(isRightDrawerOpen = true, activeRightPanel = "model_selector"))

        // When
        repository.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
        advanceUntilIdle()

        // Then
        coVerify { userProfileRepository.updateRightDrawerState(any(), false, null) }
    }
}
