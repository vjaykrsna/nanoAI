package com.vjaykrsna.nanoai.feature.uiux.data

import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ThemeRepositoryImplTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var repository: ThemeRepositoryImpl

    @BeforeEach
    fun setUp() {
        userProfileRepository = mockk(relaxed = true)
    }

    private fun createRepository(initialState: UiPreferencesSnapshot): ThemeRepositoryImpl {
        coEvery { userProfileRepository.observePreferences() } returns flowOf(initialState)
        return ThemeRepositoryImpl(
            userProfileRepository = userProfileRepository,
            ioDispatcher = mainDispatcherExtension.dispatcher
        )
    }

    @Test
    fun `updateThemePreference should call user profile repository`() = runTest {
        // Given
        repository = createRepository(UiPreferencesSnapshot())
        val theme = ThemePreference.DARK

        // When
        repository.updateThemePreference(theme)
        advanceUntilIdle()

        // Then
        coVerify { userProfileRepository.updateThemePreference(any(), theme.name) }
    }

    @Test
    fun `updateVisualDensity should call user profile repository`() = runTest {
        // Given
        repository = createRepository(UiPreferencesSnapshot())
        val density = VisualDensity.COMPACT

        // When
        repository.updateVisualDensity(density)
        advanceUntilIdle()

        // Then
        coVerify { userProfileRepository.updateVisualDensity(any(), density.name) }
    }

    @Test
    fun `uiPreferenceSnapshot should reflect the latest preferences`() = runTest {
        // Given
        val preferences = UiPreferencesSnapshot(themePreference = ThemePreference.DARK)
        repository = createRepository(preferences)

        // When
        advanceUntilIdle()
        val snapshot = repository.uiPreferenceSnapshot.first()

        // Then
        assertThat(snapshot.theme).isEqualTo(ThemePreference.DARK)
    }
}
