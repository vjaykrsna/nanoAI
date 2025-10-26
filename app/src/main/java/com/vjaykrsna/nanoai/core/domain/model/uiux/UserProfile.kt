package com.vjaykrsna.nanoai.core.domain.model.uiux

import kotlinx.datetime.Instant

/** Aggregate domain model capturing persisted UI/UX preferences and layout state for a user. */
data class UserProfile(
  val id: String,
  val displayName: String?,
  val themePreference: ThemePreference,
  val visualDensity: VisualDensity,
  val lastOpenedScreen: ScreenType,
  val compactMode: Boolean,
  var pinnedTools: List<String>,
  var savedLayouts: List<LayoutSnapshot>,
) {
  init {
    require(id.isNotBlank()) { "UserProfile id cannot be blank." }
    displayName?.let {
      require(it.length <= MAX_DISPLAY_NAME) {
        "Display name must be ≤ $MAX_DISPLAY_NAME characters."
      }
    }

    pinnedTools = sanitizePinnedTools(pinnedTools)
    savedLayouts = sanitizeSavedLayouts(savedLayouts)

    if (compactMode) {
      require(visualDensity == VisualDensity.COMPACT) {
        "Compact mode requires visual density COMPACT to remain consistent with UI expectations."
      }
    }
  }

  /** Updates layouts while re-validating invariants. */
  fun withLayouts(layouts: List<LayoutSnapshot>): UserProfile =
    copy(savedLayouts = sanitizeSavedLayouts(layouts))

  /** Updates pinned tools while preserving ordering and validation guarantees. */
  fun withPinnedTools(tools: List<String>): UserProfile =
    copy(pinnedTools = sanitizePinnedTools(tools))

  /** Applies the supplied UI preferences snapshot to this profile. */
  fun withPreferences(preferences: UiPreferencesSnapshot): UserProfile {
    val normalized = preferences.normalized()
    return copy(
      themePreference = normalized.themePreference,
      visualDensity = normalized.visualDensity,
      pinnedTools = normalized.pinnedTools,
      compactMode = normalized.visualDensity == VisualDensity.COMPACT,
    )
  }

  companion object {
    const val MAX_DISPLAY_NAME = 50
    const val MAX_PINNED_TOOLS = 10

    /** Builds a domain profile directly from preference state when Room has not hydrated yet. */
    fun fromPreferences(
      id: String,
      preferences: UiPreferencesSnapshot,
      lastOpenedScreen: ScreenType = ScreenType.HOME,
      savedLayouts: List<LayoutSnapshot> = emptyList(),
      displayName: String? = null,
    ): UserProfile {
      val normalized = preferences.normalized()
      return UserProfile(
        id = id,
        displayName = displayName,
        themePreference = normalized.themePreference,
        visualDensity = normalized.visualDensity,
        lastOpenedScreen = lastOpenedScreen,
        compactMode = normalized.visualDensity == VisualDensity.COMPACT,
        pinnedTools = normalized.pinnedTools,
        savedLayouts = sanitizeSavedLayouts(savedLayouts),
      )
    }
  }
}

/** Lightweight record mirroring persisted profile state prior to domain mapping. */
data class UserProfileRecord(
  val id: String,
  val displayName: String?,
  val themePreference: ThemePreference,
  val visualDensity: VisualDensity,
  val lastOpenedScreen: ScreenType,
  val compactMode: Boolean,
  val pinnedTools: List<String>,
  val savedLayouts: List<LayoutSnapshot>,
)

fun UserProfileRecord.toDomain(): UserProfile =
  UserProfile(
    id = id,
    displayName = displayName,
    themePreference = themePreference,
    visualDensity = visualDensity,
    lastOpenedScreen = lastOpenedScreen,
    compactMode = compactMode,
    pinnedTools = pinnedTools,
    savedLayouts = savedLayouts,
  )

fun UserProfile.toRecord(): UserProfileRecord =
  UserProfileRecord(
    id = id,
    displayName = displayName,
    themePreference = themePreference,
    visualDensity = visualDensity,
    lastOpenedScreen = lastOpenedScreen,
    compactMode = compactMode,
    pinnedTools = pinnedTools,
    savedLayouts = savedLayouts,
  )

/** Snapshot of preference state emitted by DataStore before merging into the domain profile. */
data class UiPreferencesSnapshot(
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val visualDensity: VisualDensity = VisualDensity.DEFAULT,
  var pinnedTools: List<String> = emptyList(),
  var commandPaletteRecents: List<String> = emptyList(),
  val connectivityBannerLastDismissed: Instant? = null,
  val highContrastEnabled: Boolean = false,
) {
  init {
    pinnedTools = sanitizePinnedTools(pinnedTools)
    commandPaletteRecents = sanitizeCommandPaletteRecents(commandPaletteRecents)
  }
}

fun UiPreferencesSnapshot.normalized(): UiPreferencesSnapshot = copy()

fun UserProfile.toPreferencesSnapshot(): UiPreferencesSnapshot =
  UiPreferencesSnapshot(
    themePreference = themePreference,
    visualDensity = visualDensity,
    pinnedTools = pinnedTools,
    commandPaletteRecents = emptyList(),
    connectivityBannerLastDismissed = null,
    highContrastEnabled = false, // TODO: add to UserProfile if needed
  )
