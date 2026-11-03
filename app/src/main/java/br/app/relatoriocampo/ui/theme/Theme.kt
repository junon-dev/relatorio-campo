package br.app.relatoriocampo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Paleta igual à da versão HTML: verde (marca / pioneiro regular), âmbar (auxiliar / crédito),
 * ardósia (publicador / revisita). Contraste de texto ≥ 4,5:1 e de controles ≥ 3:1.
 */
data class AppColors(
    val bg: Color, val card: Color, val field: Color, val ink: Color, val muted: Color, val line: Color, val off: Color,
    val accent: Color, val accentInk: Color, val accentSoft: Color, val accentText: Color,
    val amber: Color, val amberSoft: Color, val slate: Color, val slateSoft: Color, val danger: Color,
    val isDark: Boolean,
)

val LightColors = AppColors(
    bg = Color(0xFFF4F2EC), card = Color(0xFFFFFFFF), field = Color(0xFFF8F7F3), ink = Color(0xFF18211D),
    muted = Color(0xFF56605A), line = Color(0xFFE2DED4), off = Color(0xFF8A938D),
    accent = Color(0xFF2B6A4C), accentInk = Color(0xFFFFFFFF), accentSoft = Color(0xFFE2EFE7), accentText = Color(0xFF225A40),
    amber = Color(0xFF8A5410), amberSoft = Color(0xFFFBECD6), slate = Color(0xFF44515F), slateSoft = Color(0xFFE7EBF0),
    danger = Color(0xFFA8322E), isDark = false,
)

val DarkColors = AppColors(
    bg = Color(0xFF0F1412), card = Color(0xFF18201C), field = Color(0xFF121915), ink = Color(0xFFE7ECE9),
    muted = Color(0xFF9BA8A1), line = Color(0xFF2A342F), off = Color(0xFF647169),
    accent = Color(0xFF6CC296), accentInk = Color(0xFF0B1410), accentSoft = Color(0xFF1F3329), accentText = Color(0xFF8FD6B0),
    amber = Color(0xFFE9B46B), amberSoft = Color(0xFF3A2B17), slate = Color(0xFFB5C0CC), slateSoft = Color(0xFF27303A),
    danger = Color(0xFFEF8A85), isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

/** Cores que o usuário pode escolher (marca do app). Contraste conferido: ≥ 4,5:1 em claro e escuro. */
data class Palette(val key: String, val light: List<Color>, val dark: List<Color>) // accent, accentInk, accentSoft, accentText

val PALETTES = listOf(
    Palette("verde", listOf(Color(0xFF2B6A4C), Color(0xFFFFFFFF), Color(0xFFE2EFE7), Color(0xFF225A40)), listOf(Color(0xFF6CC296), Color(0xFF0B1410), Color(0xFF1F3329), Color(0xFF8FD6B0))),
    Palette("azul", listOf(Color(0xFF1F5FA8), Color(0xFFFFFFFF), Color(0xFFE1EBF7), Color(0xFF1A4F8C)), listOf(Color(0xFF7FB0EA), Color(0xFF0A1422), Color(0xFF1C2B3F), Color(0xFFA3C7F2))),
    Palette("roxo", listOf(Color(0xFF6A3FA0), Color(0xFFFFFFFF), Color(0xFFECE4F6), Color(0xFF5A348A)), listOf(Color(0xFFB79CE6), Color(0xFF140E1F), Color(0xFF2B2240), Color(0xFFCDB9EF))),
    Palette("vinho", listOf(Color(0xFF9B2C4A), Color(0xFFFFFFFF), Color(0xFFF6E3E8), Color(0xFF84253F)), listOf(Color(0xFFEC9AB0), Color(0xFF1F0B11), Color(0xFF3D1F28), Color(0xFFF2B8C7))),
    Palette("petroleo", listOf(Color(0xFF0F6E75), Color(0xFFFFFFFF), Color(0xFFDDEFF0), Color(0xFF0B5C62)), listOf(Color(0xFF5FC4CB), Color(0xFF06181A), Color(0xFF173436), Color(0xFF8ED8DD))),
)

fun palette(key: String): Palette = PALETTES.firstOrNull { it.key == key } ?: PALETTES.first()

/** Tamanho do texto: as mesmas 6 posições do Roteiro para Presidente. */
val FONT_SCALES = listOf(0.85f, 0.93f, 1.0f, 1.12f, 1.25f, 1.4f)

/** Escala de fonte do sistema, sem o ajuste do app (usada na barra inferior, que tem espaço fixo). */
val LocalSystemFontScale = staticCompositionLocalOf { 1f }

val appColors: AppColors
    @Composable @ReadOnlyComposable get() = LocalAppColors.current

/** Cor principal e fundo suave de cada categoria. */
fun AppColors.catColor(cat: String): Color = when (cat) { "aux" -> amber; "reg" -> accent; else -> slate }
fun AppColors.catText(cat: String): Color = when (cat) { "aux" -> amber; "reg" -> accentText; else -> slate }
fun AppColors.catSoft(cat: String): Color = when (cat) { "aux" -> amberSoft; "reg" -> accentSoft; else -> slateSoft }

@Composable
fun RelatorioTheme(dark: Boolean, accent: String = "verde", fontSize: Int = 2, content: @Composable () -> Unit) {
    val p = palette(accent).let { if (dark) it.dark else it.light }
    val c = (if (dark) DarkColors else LightColors).copy(accent = p[0], accentInk = p[1], accentSoft = p[2], accentText = p[3])
    val density = LocalDensity.current
    val scale = FONT_SCALES[fontSize.coerceIn(0, FONT_SCALES.size - 1)]
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.accentInk, primaryContainer = c.accentSoft, onPrimaryContainer = c.accentText,
            secondary = c.amber, background = c.bg, onBackground = c.ink, surface = c.card, onSurface = c.ink,
            surfaceVariant = c.field, onSurfaceVariant = c.muted, outline = c.line, outlineVariant = c.line, error = c.danger,
            inverseSurface = c.ink, inverseOnSurface = c.bg, inversePrimary = c.accentSoft,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.accentInk, primaryContainer = c.accentSoft, onPrimaryContainer = c.accentText,
            secondary = c.amber, background = c.bg, onBackground = c.ink, surface = c.card, onSurface = c.ink,
            surfaceVariant = c.field, onSurfaceVariant = c.muted, outline = c.line, outlineVariant = c.line, error = c.danger,
            inverseSurface = c.ink, inverseOnSurface = c.bg, inversePrimary = c.accentSoft,
        )
    }
    val base = Typography()
    val typography = base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 21.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = 16.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    )
    CompositionLocalProvider(
        LocalAppColors provides c,
        LocalSystemFontScale provides density.fontScale,
        LocalDensity provides Density(density.density, density.fontScale * scale),
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

val NumStyle = TextStyle(fontFeatureSettings = "tnum")
