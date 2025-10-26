package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity

private const val MIN_FONT_SCALE = 0.85f
private const val MAX_FONT_SCALE = 1.4f

/** Lightweight snapshot of persisted UI preferences relevant to the shell. */
data class UiPreferenceSnapshot(
  val theme: ThemePreference = ThemePreference.SYSTEM,
  val density: VisualDensity = VisualDensity.DEFAULT,
  val fontScale: Float = 1f,
  val dismissedTooltips: Set<String> = emptySet(),
  val highContrastEnabled: Boolean = false,
)

/** True when a tooltip with the provided [id] has been dismissed previously. */
fun UiPreferenceSnapshot.isTooltipDismissed(id: String): Boolean = dismissedTooltips.contains(id)

/** Returns a copy with an additional dismissed tooltip tracked. */
fun UiPreferenceSnapshot.withTooltipDismissed(id: String): UiPreferenceSnapshot =
  copy(dismissedTooltips = dismissedTooltips + id)

/** Normalized font scale applied to typography (min 0.85f, max 1.4f). */
val UiPreferenceSnapshot.normalizedFontScale: Float
  get() = fontScale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
