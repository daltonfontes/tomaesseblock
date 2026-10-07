package com.tomaesseblock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tomaesseblock.R

// Visual "Extrato": papel, tinta e um vermelho só para o que foi barrado.

val Mono = FontFamily(
    Font(R.font.plex_mono_regular, FontWeight.Normal),
    Font(R.font.plex_mono_semibold, FontWeight.SemiBold),
)
val Condensed = FontFamily(Font(R.font.plex_condensed_bold, FontWeight.Bold))

/** Cores que o Material não tem: bloqueio, rótulos e o tracejado entre linhas. */
data class ExtratoColors(val blocked: Color, val pencil: Color, val dash: Color)

private val LightExtrato = ExtratoColors(blocked = Color(0xFFC8102E), pencil = Color(0xFF6B6B6B), dash = Color(0xFFBDBDBD))
private val DarkExtrato = ExtratoColors(blocked = Color(0xFFFF6B7D), pencil = Color(0xFFA3A3A3), dash = Color(0xFF4A4A4A))

private val LightColors = lightColorScheme(
    primary = Color(0xFF141414),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF141414),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF141414),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDEDED),
    onSecondaryContainer = Color(0xFF141414),
    tertiary = LightExtrato.blocked,
    error = LightExtrato.blocked,
    background = Color.White,
    onBackground = Color(0xFF141414),
    surface = Color.White,
    onSurface = Color(0xFF141414),
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = LightExtrato.pencil,
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF2F2F2),
    outline = Color(0xFF141414),
    outlineVariant = LightExtrato.dash,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFEDEDED),
    onPrimary = Color(0xFF121212),
    primaryContainer = Color(0xFFEDEDED),
    onPrimaryContainer = Color(0xFF121212),
    secondary = Color(0xFFEDEDED),
    onSecondary = Color(0xFF121212),
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color(0xFFEDEDED),
    tertiary = DarkExtrato.blocked,
    error = DarkExtrato.blocked,
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = DarkExtrato.pencil,
    surfaceContainer = Color(0xFF121212),
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF1E1E1E),
    outline = Color(0xFFEDEDED),
    outlineVariant = DarkExtrato.dash,
)

private val Base = Typography()

private fun TextStyle.mono() = copy(fontFamily = Mono, letterSpacing = 0.sp)

private val ExtratoTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold),
    displayMedium = Base.displayMedium.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold),
    displaySmall = Base.displaySmall.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.mono().copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    titleSmall = Base.titleSmall.mono().copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.mono().copy(fontSize = 15.sp),
    bodyMedium = Base.bodyMedium.mono().copy(fontSize = 13.sp),
    bodySmall = Base.bodySmall.mono(),
    labelLarge = Base.labelLarge.mono().copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.04.em),
    labelMedium = Base.labelMedium.mono().copy(letterSpacing = 0.04.em),
    labelSmall = Base.labelSmall.mono().copy(letterSpacing = 0.06.em),
)

// Sem cantos arredondados: tudo é régua e caixa, como num extrato impresso.
private val Square = RoundedCornerShape(0)
private val ExtratoShapes = Shapes(
    extraSmall = Square,
    small = Square,
    medium = Square,
    large = Square,
    extraLarge = Square,
)

private val LocalExtrato = androidx.compose.runtime.staticCompositionLocalOf { LightExtrato }

object Extrato {
    val colors: ExtratoColors
        @Composable @ReadOnlyComposable
        get() = LocalExtrato.current
}

@Composable
fun TomaEsseBlockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalExtrato provides if (darkTheme) DarkExtrato else LightExtrato,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = ExtratoTypography,
            shapes = ExtratoShapes,
            content = content,
        )
    }
}
