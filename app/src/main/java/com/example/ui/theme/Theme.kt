package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LiftLogDarkColorScheme = darkColorScheme(
    primary = EmberOrange,
    onPrimary = Color.White,
    primaryContainer = EmberContainerDark,
    onPrimaryContainer = EmberGlow,
    secondary = CyanPulse,
    onSecondary = Color(0xFF042229),
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanPulse,
    tertiary = EmeraldPr,
    onTertiary = Color(0xFF032117),
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = EmeraldPr,
    background = ObsidianBg,
    onBackground = InkWhite,
    surface = SlateSurface,
    onSurface = InkWhite,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = InkMutedDark,
    outline = SlateBorder,
    outlineVariant = Color(0xFF222A3A),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFCA5A5)
)

private val LiftLogLightColorScheme = lightColorScheme(
    primary = EmberOrangeDark,
    onPrimary = Color.White,
    primaryContainer = EmberContainerLight,
    onPrimaryContainer = Color(0xFF5D1500),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = Color(0xFF0E4A5C),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = AlabasterBg,
    onBackground = InkCharcoal,
    surface = PureWhiteSurface,
    onSurface = InkCharcoal,
    surfaceVariant = SoftSlateSurface,
    onSurfaceVariant = InkMutedLight,
    outline = LightBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val LiftLogShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LiftLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LiftLogDarkColorScheme else LiftLogLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LiftLogTypography,
        shapes = LiftLogShapes,
        content = content
    )
}
