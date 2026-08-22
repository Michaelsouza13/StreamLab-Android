package com.streamlab.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.darkColorScheme
import androidx.compose.runtime.Composable

val TvDarkColorScheme = darkColorScheme(
    primary = PurpleAccent,
    onPrimary = TextPrimary,
    secondary = PurpleAccentVariant,
    onSecondary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextPrimary,
    error = ErrorRed,
    onError = TextPrimary,
    border = SurfaceVariantDark
)

val TvShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun StreamlabTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        shapes = TvShapes,
        content = content
    )
}
