package com.manjee.linkops.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * LinkOps Light Color Scheme
 */
private val LightColorScheme = lightColorScheme(
    primary = LinkOpsColors.Primary,
    onPrimary = LinkOpsColors.OnPrimary,
    primaryContainer = LinkOpsColors.PrimaryLight,
    onPrimaryContainer = LinkOpsColors.PrimaryDark,

    secondary = LinkOpsColors.Secondary,
    onSecondary = LinkOpsColors.OnSecondary,
    secondaryContainer = LinkOpsColors.SecondaryLight,
    onSecondaryContainer = LinkOpsColors.SecondaryDark,

    background = LinkOpsColors.Background,
    onBackground = LinkOpsColors.OnBackground,

    surface = LinkOpsColors.Surface,
    onSurface = LinkOpsColors.OnSurface,
    surfaceVariant = LinkOpsColors.SurfaceVariant,
    onSurfaceVariant = LinkOpsColors.OnSurfaceVariant,

    error = LinkOpsColors.Error,
    onError = LinkOpsColors.OnPrimary,
    errorContainer = LinkOpsColors.ErrorLight,
    onErrorContainer = LinkOpsColors.Error,

    outline = LinkOpsColors.Divider
)

/**
 * LinkOps Dark Color Scheme
 */
private val DarkColorScheme = darkColorScheme(
    primary = LinkOpsColors.Primary,
    onPrimary = LinkOpsColors.OnPrimary,
    primaryContainer = LinkOpsColors.PrimaryDark,
    onPrimaryContainer = LinkOpsColors.PrimaryLight,

    secondary = LinkOpsColors.SecondaryLight,
    onSecondary = LinkOpsColors.SecondaryDark,
    secondaryContainer = LinkOpsColors.SecondaryDark,
    onSecondaryContainer = LinkOpsColors.SecondaryLight,

    background = LinkOpsColors.BackgroundDark,
    onBackground = LinkOpsColors.OnBackgroundDark,

    surface = LinkOpsColors.SurfaceDark,
    onSurface = LinkOpsColors.OnSurfaceDark,
    surfaceVariant = LinkOpsColors.SurfaceVariantDark,
    onSurfaceVariant = LinkOpsColors.OnSurfaceVariant,

    error = LinkOpsColors.Error,
    onError = LinkOpsColors.OnPrimary,
    errorContainer = LinkOpsColors.ErrorLight,
    onErrorContainer = LinkOpsColors.Error,

    outline = LinkOpsColors.DividerDark
)

/**
 * LinkOps Theme
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param content Content to be themed
 */
@Composable
fun LinkOpsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LinkOpsTypography,
        content = content
    )
}
