package com.vjaykrsna.nanoai.core.data.preferences

import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import kotlinx.datetime.Instant

/**
 * Data class representing UI/UX preferences.
 *
 * Stored using DataStore for reactive updates. These preferences overlay the database-backed
 * UserProfile for quick access to frequently-accessed UI settings.
 *
 * @property themePreference User's theme preference (light/dark/system)
 * @property visualDensity Visual density preference for UI spacing
 * @property pinnedToolIds Ordered list of pinned tool IDs (max 10)
 */
data class UiPreferences(
  val themePreference: ThemePreference,
  val visualDensity: VisualDensity,
  val pinnedToolIds: List<String>,
  val commandPaletteRecents: List<String>,
  val connectivityBannerLastDismissed: Instant?,
  val highContrastEnabled: Boolean = false,
) {
  constructor() :
    this(
      themePreference = ThemePreference.SYSTEM,
      visualDensity = VisualDensity.DEFAULT,
      pinnedToolIds = emptyList(),
      commandPaletteRecents = emptyList(),
      connectivityBannerLastDismissed = null,
      highContrastEnabled = false,
    )
}

/**
 * Convert DataStore UiPreferences to domain UiPreferencesSnapshot.
 *
 * This mapper bridges the persistence layer (DataStore) to the domain layer, allowing preferences
 * to be merged with database-backed UserProfile records.
 */
fun UiPreferences.toDomainSnapshot(): UiPreferencesSnapshot =
  UiPreferencesSnapshot(
    themePreference = themePreference,
    visualDensity = visualDensity,
    pinnedTools = pinnedToolIds,
    commandPaletteRecents = commandPaletteRecents,
    connectivityBannerLastDismissed = connectivityBannerLastDismissed,
    highContrastEnabled = highContrastEnabled,
  )

/**
 * Convert domain UiPreferencesSnapshot to DataStore UiPreferences.
 *
 * This reverse mapper allows domain preferences to be persisted to DataStore.
 */
fun UiPreferencesSnapshot.toDataStorePreferences(): UiPreferences =
  UiPreferences(
    themePreference = themePreference,
    visualDensity = visualDensity,
    pinnedToolIds = pinnedTools,
    commandPaletteRecents = commandPaletteRecents,
    connectivityBannerLastDismissed = connectivityBannerLastDismissed,
    highContrastEnabled = highContrastEnabled,
  )
