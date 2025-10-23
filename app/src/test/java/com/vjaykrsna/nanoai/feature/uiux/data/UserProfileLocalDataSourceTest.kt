package com.vjaykrsna.nanoai.feature.uiux.data

import com.vjaykrsna.nanoai.core.data.db.daos.LayoutSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UIStateSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UserProfileDao
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class UserProfileLocalDataSourceTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private lateinit var userProfileDao: UserProfileDao
    private lateinit var layoutSnapshotDao: LayoutSnapshotDao
    private lateinit var uiStateSnapshotDao: UIStateSnapshotDao
    private lateinit var uiPreferencesStore: UiPreferencesStore
    private lateinit var dataSource: UserProfileLocalDataSource

    @BeforeEach
    fun setUp() {
        userProfileDao = mockk(relaxed = true)
        layoutSnapshotDao = mockk(relaxed = true)
        uiStateSnapshotDao = mockk(relaxed = true)
        uiPreferencesStore = mockk(relaxed = true)
        dataSource = UserProfileLocalDataSource(
            userProfileDao = userProfileDao,
            layoutSnapshotDao = layoutSnapshotDao,
            uiStateSnapshotDao = uiStateSnapshotDao,
            uiPreferencesStore = uiPreferencesStore
        )
    }

    @Test
    fun `saveUserProfile should insert both the profile and layout snapshots`() = runTest {
        // Given
        val profile = UserProfile.fromPreferences("testUser", UiPreferencesSnapshot())

        // When
        dataSource.saveUserProfile(profile)

        // Then
        coVerify { userProfileDao.insert(any()) }
        coVerify { layoutSnapshotDao.insertAll(any()) }
    }

    @Test
    fun `updateThemePreference should update both the store and the dao`() = runTest {
        // Given
        val userId = "testUser"
        val theme = ThemePreference.DARK

        // When
        dataSource.updateThemePreference(userId, theme)

        // Then
        coVerify { uiPreferencesStore.setThemePreference(theme) }
        coVerify { userProfileDao.updateThemePreference(userId, theme) }
    }

    @Test
    fun `updateVisualDensity should update both the store and the dao`() = runTest {
        // Given
        val userId = "testUser"
        val density = VisualDensity.COMPACT

        // When
        dataSource.updateVisualDensity(userId, density)

        // Then
        coVerify { uiPreferencesStore.setVisualDensity(density) }
        coVerify { userProfileDao.updateVisualDensity(userId, density) }
    }

    @Test
    fun `updatePinnedTools should update both the store and the dao`() = runTest {
        // Given
        val userId = "testUser"
        val pinnedTools = listOf("tool1", "tool2")

        // When
        dataSource.updatePinnedTools(userId, pinnedTools)

        // Then
        coVerify { uiPreferencesStore.setPinnedToolIds(pinnedTools) }
        coVerify { userProfileDao.updatePinnedTools(userId, pinnedTools) }
    }

    @Test
    fun `setLeftDrawerOpen should ensure snapshot and update the dao`() = runTest {
        // Given
        val userId = "testUser"
        val open = true
        coEvery { uiStateSnapshotDao.getByUserId(userId) } returns null

        // When
        dataSource.setLeftDrawerOpen(userId, open)

        // Then
        coVerify { uiStateSnapshotDao.insert(any()) }
        coVerify { uiStateSnapshotDao.updateLeftDrawerOpen(userId, open) }
    }
}
