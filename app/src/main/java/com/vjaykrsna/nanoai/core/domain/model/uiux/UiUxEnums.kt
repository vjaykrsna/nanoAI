package com.vjaykrsna.nanoai.core.domain.model.uiux

/**
 * Enumerations backing the UI/UX feature domain models.
 *
 * These enums intentionally remain small and serializable so they can be persisted via Room type
 * converters and transported across layers without additional mapping.
 */
enum class ThemePreference {
  LIGHT,
  DARK,
  AMOLED, // Pure black backgrounds for OLED power savings
  SYSTEM;

  companion object {
    fun fromName(value: String?): ThemePreference {
      return value?.let { candidate ->
        values().firstOrNull { it.name.equals(candidate, ignoreCase = true) }
      } ?: SYSTEM
    }
  }
}

enum class VisualDensity {
  DEFAULT,
  COMPACT,
  EXPANDED;

  companion object {
    const val COMPACT_PINNED_TOOL_CAP = 6
  }
}

enum class ScreenType {
  HOME,
  SETTINGS,
  SIDEBAR,
  TOOLBOX,
  UNKNOWN;

  companion object {
    fun fromRoute(route: String?): ScreenType {
      return route?.let { candidate ->
        values().firstOrNull { it.name.equals(candidate, ignoreCase = true) }
      } ?: UNKNOWN
    }
  }
}
