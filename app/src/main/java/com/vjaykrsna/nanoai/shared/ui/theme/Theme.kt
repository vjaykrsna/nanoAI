package com.vjaykrsna.nanoai.shared.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference

private val LightColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
  )

// NOTE: Pure black for OLED power savings
private val AmoledColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = AmoledBackground,
    onBackground = DarkOnBackground,
    surface = AmoledSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
  )

// NOTE: High contrast light scheme - WCAG AAA compliant for accessibility
private val HighContrastLightColorScheme =
  lightColorScheme(
    primary = HighContrastLightPrimary,
    onPrimary = HighContrastLightOnPrimary,
    primaryContainer = HighContrastLightPrimaryContainer,
    onPrimaryContainer = HighContrastLightOnPrimaryContainer,
    secondary = HighContrastLightSecondary,
    onSecondary = HighContrastLightOnSecondary,
    secondaryContainer = HighContrastLightSecondaryContainer,
    onSecondaryContainer = HighContrastLightOnSecondaryContainer,
    tertiary = HighContrastLightTertiary,
    onTertiary = HighContrastLightOnTertiary,
    tertiaryContainer = HighContrastLightTertiaryContainer,
    onTertiaryContainer = HighContrastLightOnTertiaryContainer,
    background = HighContrastLightBackground,
    onBackground = HighContrastLightOnBackground,
    surface = HighContrastLightSurface,
    onSurface = HighContrastLightOnSurface,
    surfaceVariant = HighContrastLightSurfaceVariant,
    onSurfaceVariant = HighContrastLightOnSurfaceVariant,
    outline = HighContrastLightOutline,
    outlineVariant = HighContrastLightOutlineVariant,
    error = HighContrastLightError,
    onError = HighContrastLightOnError,
    errorContainer = HighContrastLightErrorContainer,
    onErrorContainer = HighContrastLightOnErrorContainer,
    inverseSurface = HighContrastLightInverseSurface,
    inverseOnSurface = HighContrastLightInverseOnSurface,
    inversePrimary = HighContrastLightInversePrimary,
  )

// High contrast dark scheme - WCAG AAA compliant
private val HighContrastDarkColorScheme =
  darkColorScheme(
    primary = HighContrastDarkPrimary,
    onPrimary = HighContrastDarkOnPrimary,
    primaryContainer = HighContrastDarkPrimaryContainer,
    onPrimaryContainer = HighContrastDarkOnPrimaryContainer,
    secondary = HighContrastDarkSecondary,
    onSecondary = HighContrastDarkOnSecondary,
    secondaryContainer = HighContrastDarkSecondaryContainer,
    onSecondaryContainer = HighContrastDarkOnSecondaryContainer,
    tertiary = HighContrastDarkTertiary,
    onTertiary = HighContrastDarkOnTertiary,
    tertiaryContainer = HighContrastDarkTertiaryContainer,
    onTertiaryContainer = HighContrastDarkOnTertiaryContainer,
    background = HighContrastDarkBackground,
    onBackground = HighContrastDarkOnBackground,
    surface = HighContrastDarkSurface,
    onSurface = HighContrastDarkOnSurface,
    surfaceVariant = HighContrastDarkSurfaceVariant,
    onSurfaceVariant = HighContrastDarkOnSurfaceVariant,
    outline = HighContrastDarkOutline,
    outlineVariant = HighContrastDarkOutlineVariant,
    error = HighContrastDarkError,
    onError = HighContrastDarkOnError,
    errorContainer = HighContrastDarkErrorContainer,
    onErrorContainer = HighContrastDarkOnErrorContainer,
    inverseSurface = HighContrastDarkInverseSurface,
    inverseOnSurface = HighContrastDarkInverseOnSurface,
    inversePrimary = HighContrastDarkInversePrimary,
  )

@Composable
fun NanoAITheme(
  themePreference: ThemePreference = ThemePreference.SYSTEM,
  highContrastEnabled: Boolean = false,
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val systemDarkTheme = isSystemInDarkTheme()
  val darkTheme = resolveDarkTheme(themePreference, systemDarkTheme)
  val isAmoled = themePreference == ThemePreference.AMOLED
  val isHighContrast = highContrastEnabled
  val colorScheme =
    rememberNanoAIColorScheme(
      darkTheme = darkTheme,
      isAmoled = isAmoled,
      dynamicColor = dynamicColor,
      isHighContrast = isHighContrast,
    )

  ApplySystemBars(colorScheme = colorScheme, darkTheme = darkTheme)

  MaterialTheme(colorScheme = colorScheme, typography = NanoAITypography, content = content)
}

private fun resolveDarkTheme(themePreference: ThemePreference, systemDarkTheme: Boolean): Boolean {
  return when (themePreference) {
    ThemePreference.SYSTEM -> systemDarkTheme
    ThemePreference.DARK,
    ThemePreference.AMOLED -> true
    ThemePreference.LIGHT -> false
  }
}

@Composable
private fun rememberNanoAIColorScheme(
  darkTheme: Boolean,
  isAmoled: Boolean,
  dynamicColor: Boolean,
  isHighContrast: Boolean,
): ColorScheme {
  return when {
    isHighContrast && darkTheme -> HighContrastDarkColorScheme
    isHighContrast && !darkTheme -> HighContrastLightColorScheme
    isAmoled -> AmoledColorScheme
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }
}

@Composable
private fun ApplySystemBars(colorScheme: ColorScheme, darkTheme: Boolean) {
  val view = LocalView.current
  if (view.isInEditMode) {
    return
  }

  val activity = view.context as? ComponentActivity ?: return
  val surfaceColor = colorScheme.surface.toArgb()
  val onSurfaceColor = colorScheme.onSurface.toArgb()
  val statusBarStyle =
    if (darkTheme) {
      SystemBarStyle.dark(surfaceColor)
    } else {
      SystemBarStyle.light(surfaceColor, onSurfaceColor)
    }
  val navigationBarStyle =
    if (darkTheme) {
      SystemBarStyle.dark(surfaceColor)
    } else {
      SystemBarStyle.light(surfaceColor, onSurfaceColor)
    }

  SideEffect {
    activity.enableEdgeToEdge(
      statusBarStyle = statusBarStyle,
      navigationBarStyle = navigationBarStyle,
    )
  }
}
