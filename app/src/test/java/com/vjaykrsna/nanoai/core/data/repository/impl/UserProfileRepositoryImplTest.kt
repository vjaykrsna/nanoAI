package com.vjaykrsna.nanoai.core.data.repository.impl

import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.feature.uiux.data.UserProfileLocalDataSource
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.jupiter.api.extension.ExtendWith

@RunWith(RobolectricTestRunner::class)
@ExtendWith(MainDispatcherExtension::class)
class UserProfileRepositoryImplTest {

    private lateinit var localDataSource: UserProfileLocalDataSource
    private lateinit var repository: UserProfileRepositoryImpl
    private val testDispatcher = MainDispatcherExtension().dispatcher
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        localDataSource = mockk(relaxed = true)
        repository = UserProfileRepositoryImpl(
            local = localDataSource,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `updateThemePreference should call local data source`() = testScope.runTest {
        // Given
        val userId = "testUser"
        val themeName = "DARK"

        // When
        repository.updateThemePreference(userId, themeName)

        // Then
        coVerify { localDataSource.updateThemePreference(userId, ThemePreference.DARK) }
    }

    @Test
    fun `updateCompactMode should call local data source`() = testScope.runTest {
        // Given
        val userId = "testUser"
        val enabled = true

        // When
        repository.updateCompactMode(userId, enabled)

        // Then
        coVerify { localDataSource.updateCompactMode(userId, enabled) }
    }

    @Test
    fun `updatePinnedTools should call local data source`() = testScope.runTest {
        // Given
        val userId = "testUser"
        val pinnedTools = listOf("tool1", "tool2")

        // When
        repository.updatePinnedTools(userId, pinnedTools)

        // Then
        coVerify { localDataSource.updatePinnedTools(userId, pinnedTools) }
    }

    @Test
    fun `updateLeftDrawerOpen should call local data source`() = testScope.runTest {
        // Given
        val userId = "testUser"
        val open = true

        // When
        repository.updateLeftDrawerOpen(userId, open)

        // Then
        coVerify { localDataSource.setLeftDrawerOpen(userId, open) }
    }

    @Test
    fun `updateRightDrawerState should call local data source`() = testScope.runTest {
        // Given
        val userId = "testUser"
        val open = true
        val panel = "testPanel"

        // When
        repository.updateRightDrawerState(userId, open, panel)

        // Then
        coVerify { localDataSource.setRightDrawerState(userId, open, panel) }
    }

    @Test
    fun `updateActiveModeRoute should call local data source`() = testScope.runTest {
        // Given
        val userId = "testUser"
        val route = "testRoute"

        // When
        repository.updateActiveModeRoute(userId, route)

        // Then
        coVerify { localDataSource.setActiveModeRoute(userId, route) }
    }
}
