package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BlueCorp,
    onPrimary = SurfaceWhite,
    primaryContainer = BlueSoft,
    onPrimaryContainer = NavyDeep,
    secondary = GoldPrimary,
    onSecondary = NavyDeep,
    secondaryContainer = PendingAmberBg,
    onSecondaryContainer = PendingAmberText,
    tertiary = WhatsAppGreen,
    onTertiary = SurfaceWhite,
    background = BgCanvas,
    onBackground = InkText,
    surface = SurfaceWhite,
    onSurface = InkText,
    surfaceVariant = CardSubtle,
    onSurfaceVariant = MutedText,
    outline = LineBorder,
    outlineVariant = BlueBorder,
    error = InvalidRedText,
    onError = SurfaceWhite,
    errorContainer = InvalidRedBg,
    onErrorContainer = InvalidRedText
)

private val ColorTokensDarkAmberBg = androidx.compose.ui.graphics.Color(0xFF3E3010)
private val ColorTokensDarkRedBg = androidx.compose.ui.graphics.Color(0xFF471B20)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBluePrimary,
    onPrimary = NavyDeep,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkInkText,
    secondary = GoldPrimary,
    onSecondary = NavyDeep,
    secondaryContainer = ColorTokensDarkAmberBg,
    onSecondaryContainer = GoldPrimary,
    tertiary = WhatsAppGreen,
    onTertiary = NavyDeep,
    background = DarkBgCanvas,
    onBackground = DarkInkText,
    surface = DarkSurface,
    onSurface = DarkInkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedText,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = InvalidRedBg,
    onError = InvalidRedText,
    errorContainer = ColorTokensDarkRedBg,
    onErrorContainer = InvalidRedBg
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun CapacitamaqTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
