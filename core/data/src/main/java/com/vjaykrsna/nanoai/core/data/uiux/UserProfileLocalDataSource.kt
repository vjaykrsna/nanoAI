package com.vjaykrsna.nanoai.core.data.uiux

import com.vjaykrsna.nanoai.core.data.db.daos.LayoutSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UIStateSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UserProfileDao
import com.vjaykrsna.nanoai.core.data.db.entities.toDomain
import com.vjaykrsna.nanoai.core.data.db.entities.toEntity
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import com.vjaykrsna.nanoai.core.data.preferences.toDomainSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.DataStoreUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import java.time.Instant as JavaInstant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant

/**
 * Local data source for user profile operations.
 *
 * Coordinates access to Room database (via DAOs) and DataStore (via UiPreferencesStore) to provide
 * a unified interface for local user profile data. Handles cache hydration and preference overlay
 * merging.
 */
@Singleton
class UserProfileLocalDataSource
@Inject
constructor(
  private val userProfileDao: UserProfileDao,
  private val layoutSnapshotDao: LayoutSnapshotDao,
  private val uiStateSnapshotDao: UIStateSnapshotDao,
  private val uiPreferencesStore: UiPreferencesStore,
) {
  /**
   * Observe user profile with preferences overlay.
   *
   * Combines database profile with DataStore preferences, with preferences taking precedence for
   * quick-access UI settings. Returns null if no profile exists.
   *
   * @param userId The unique user identifier
   * @return Flow of UserProfile or null if not found
   */
  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    combine(
      userProfileDao.observeById(userId),
      layoutSnapshotDao.observeByUserId(userId),
      uiPreferencesStore.uiPreferences,
    ) { profileEntity, layoutEntities, preferences ->
      profileEntity?.let { entity ->
        val layouts = layoutEntities.map { it.toDomain() }
        val baseProfile = entity.toDomain(layouts)

        // Apply DataStore preferences overlay
        baseProfile.withPreferences(preferences.toDomainSnapshot())
      }
    }

  /**
   * Get user profile (one-shot query).
   *
   * @param userId The unique user identifier
   * @return UserProfile or null if not found
   */
  suspend fun getUserProfile(userId: String): UserProfile? {
    val entity = userProfileDao.getById(userId) ?: return null
    val layouts = layoutSnapshotDao.getAllByUserId(userId).map { it.toDomain() }
    return entity.toDomain(layouts)
  }

  /**
   * Insert or update user profile in database.
   *
   * @param profile The user profile to save
   */
  suspend fun saveUserProfile(profile: UserProfile) {
    // Save profile entity
    userProfileDao.insert(profile.toEntity())

    // Save layout snapshots
    val layoutEntities =
      profile.savedLayouts.mapIndexed { index, layout ->
        layout.toEntity(userId = profile.id, position = index)
      }
    layoutSnapshotDao.insertAll(layoutEntities)
  }

  /**
   * Update theme preference in both DataStore and database.
   *
   * @param userId The unique user identifier
   * @param themePreference The new theme preference
   */
  suspend fun updateThemePreference(userId: String, themePreference: ThemePreference) {
    // Update DataStore for immediate UI effect
    uiPreferencesStore.setThemePreference(themePreference)

    // Update database for persistence
    userProfileDao.updateThemePreference(userId, themePreference)
  }

  /**
   * Update visual density in both DataStore and database.
   *
   * @param userId The unique user identifier
   * @param visualDensity The new visual density
   */
  suspend fun updateVisualDensity(userId: String, visualDensity: VisualDensity) {
    // Update DataStore for immediate UI effect
    uiPreferencesStore.setVisualDensity(visualDensity)

    // Update database for persistence
    userProfileDao.updateVisualDensity(userId, visualDensity)
  }

  /**
   * Update pinned tools in both DataStore and database.
   *
   * @param userId The unique user identifier
   * @param pinnedTools List of tool IDs (max 10)
   */
  suspend fun updatePinnedTools(userId: String, pinnedTools: List<String>) {
    // Update DataStore for immediate UI effect
    uiPreferencesStore.setPinnedToolIds(pinnedTools)

    // Update database for persistence
    userProfileDao.updatePinnedTools(userId, pinnedTools)
  }

  /**
   * Update compact mode in database.
   *
   * @param userId The unique user identifier
   * @param compactMode True to enable compact mode
   */
  suspend fun updateCompactMode(userId: String, compactMode: Boolean) {
    val targetDensity = if (compactMode) VisualDensity.COMPACT else VisualDensity.DEFAULT

    // Update DataStore for immediate UI feedback
    uiPreferencesStore.setVisualDensity(targetDensity)

    // Keep persistent profile record aligned with preference snapshot
    userProfileDao.updateVisualDensity(userId, targetDensity)
    userProfileDao.updateCompactMode(userId, compactMode)

    // Reflect compact mode toggles across saved layouts to preserve invariants
    val layoutEntities = layoutSnapshotDao.getAllByUserId(userId)
    if (layoutEntities.isNotEmpty()) {
      val updatedEntities =
        layoutEntities.map { entity ->
          val domain = entity.toDomain()
          val trimmedPinnedTools =
            if (compactMode && domain.pinnedTools.size > VisualDensity.COMPACT_PINNED_TOOL_CAP) {
              domain.pinnedTools.take(VisualDensity.COMPACT_PINNED_TOOL_CAP)
            } else {
              domain.pinnedTools
            }
          val adjusted = domain.copy(pinnedTools = trimmedPinnedTools).withCompactMode(compactMode)
          adjusted.toEntity(userId = entity.userId, position = entity.position)
        }
      layoutSnapshotDao.insertAll(updatedEntities)
    }
  }

  /**
   * Update last opened screen in database.
   *
   * @param userId The unique user identifier
   * @param screenType The screen type that was last opened
   */
  suspend fun updateLastOpenedScreen(userId: String, screenType: ScreenType) {
    userProfileDao.updateLastOpenedScreen(userId, screenType)
  }

  /**
   * Save or update a layout snapshot.
   *
   * @param userId The unique user identifier
   * @param layout The layout snapshot to save
   * @param position The position in the saved layouts list
   */
  suspend fun saveLayoutSnapshot(userId: String, layout: LayoutSnapshot, position: Int) {
    layoutSnapshotDao.insert(layout.toEntity(userId, position))
  }

  /**
   * Delete a layout snapshot.
   *
   * @param layoutId The unique layout identifier
   */
  suspend fun deleteLayoutSnapshot(layoutId: String) {
    layoutSnapshotDao.deleteById(layoutId)
  }

  /**
   * Observe UI state snapshot for session restoration.
   *
   * @param userId The unique user identifier
   * @return Flow of UIStateSnapshot or null if not found
   */
  fun observeUIStateSnapshot(userId: String): Flow<UIStateSnapshot?> =
    uiStateSnapshotDao.observeByUserId(userId).map { entity -> entity?.toDomain() }

  /**
   * Get UI state snapshot (one-shot query).
   *
   * @param userId The unique user identifier
   * @return UIStateSnapshot or null if not found
   */
  suspend fun getUIStateSnapshot(userId: String): UIStateSnapshot? =
    uiStateSnapshotDao.getByUserId(userId)?.toDomain()

  /**
   * Save or update UI state snapshot.
   *
   * @param snapshot The UI state snapshot to save
   */
  suspend fun saveUIStateSnapshot(snapshot: UIStateSnapshot) {
    uiStateSnapshotDao.insert(snapshot.toEntity())
  }

  /**
   * Update sidebar collapsed state.
   *
   * @param userId The unique user identifier
   * @param collapsed True if sidebar is collapsed
   */
  suspend fun updateSidebarCollapsed(userId: String, collapsed: Boolean) {
    uiStateSnapshotDao.updateSidebarCollapsed(userId, collapsed)
  }

  /**
   * Add a recent action to the UI state.
   *
   * @param userId The unique user identifier
   * @param actionId The action ID to add
   */
  suspend fun addRecentAction(userId: String, actionId: String) {
    uiStateSnapshotDao.addRecentAction(userId, actionId)
  }

  /**
   * Hydrate cache from DataStore on app startup.
   *
   * Returns current DataStore preferences for merging with remote/database data.
   *
   * @return Current UI preferences from DataStore
   */
  suspend fun getCachedPreferences() = uiPreferencesStore.uiPreferences.first()

  /** Observe UiPreferences as domain snapshots for repository consumers. */
  fun observePreferences(): Flow<DataStoreUiPreferences> =
    uiPreferencesStore.uiPreferences.map { it.toDomainSnapshot() }

  /** Record command palette usage for recents tracking. */
  suspend fun recordCommandPaletteRecent(commandId: String) {
    uiPreferencesStore.recordCommandPaletteRecent(commandId)
  }

  /** Replace command palette recent entries. */
  suspend fun setCommandPaletteRecents(commandIds: List<String>) {
    uiPreferencesStore.setCommandPaletteRecents(commandIds)
  }

  /** Persist connectivity banner dismissal timestamp. */
  suspend fun setConnectivityBannerDismissed(dismissedAt: JavaInstant?) {
    val kotlinInstant: Instant? = dismissedAt?.toKotlinInstant()
    uiPreferencesStore.setConnectivityBannerDismissed(kotlinInstant)
  }
}
