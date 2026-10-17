package vn.ghichunhanh.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Màu phụ không có sẵn trong Material: nền phím, màu nút "=", nút xóa… */
@Immutable
data class ExtraColors(
    val accentSoft: Color,
    val keyBg: Color,
    val opBg: Color,
    val opFg: Color,
    val eqBg: Color,
    val eqFg: Color,
    val clearBg: Color,
    val clearFg: Color,
    val sunday: Color,
)

private val LightExtra = ExtraColors(
    accentSoft = Color(0xFFE0F2F5),
    keyBg = Color(0xFFEEF3F4),
    opBg = Color(0xFFD2ECF1),
    opFg = Color(0xFF0A4D5A),
    eqBg = Color(0xFFF2A93B),
    eqFg = Color(0xFF2A1B00),
    clearBg = Color(0xFFF7E1D8),
    clearFg = Color(0xFF7E2A10),
    sunday = Color(0xFFB3261E),
)

private val DarkExtra = ExtraColors(
    accentSoft = Color(0xFF12303A),
    keyBg = Color(0xFF212B2E),
    opBg = Color(0xFF143A45),
    opFg = Color(0xFFA5E4EF),
    eqBg = Color(0xFFF2A93B),
    eqFg = Color(0xFF2A1B00),
    clearBg = Color(0xFF3B2620),
    clearFg = Color(0xFFF4BBA6),
    sunday = Color(0xFFF2B8B5),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0B7285),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F5),
    onPrimaryContainer = Color(0xFF06333C),
    background = Color(0xFFF1F5F6),
    onBackground = Color(0xFF14201F),
    surface = Color.White,
    onSurface = Color(0xFF14201F),
    onSurfaceVariant = Color(0xFF52625F),
    outline = Color(0xFFB5C5C8),
    outlineVariant = Color(0xFFDDE6E8),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF4FC3D9),
    onPrimary = Color(0xFF04161A),
    primaryContainer = Color(0xFF12303A),
    onPrimaryContainer = Color(0xFFB8EAF3),
    background = Color(0xFF0E1314),
    onBackground = Color(0xFFE5EEF0),
    surface = Color(0xFF172022),
    onSurface = Color(0xFFE5EEF0),
    onSurfaceVariant = Color(0xFF9AAAAE),
    outline = Color(0xFF43555A),
    outlineVariant = Color(0xFF283437),
)

val LocalExtraColors = staticCompositionLocalOf { LightExtra }

/** [dark] = null nghĩa là theo cài đặt sáng/tối của điện thoại. */
@Composable
fun AppTheme(dark: Boolean? = null, content: @Composable () -> Unit) {
    val useDark = dark ?: isSystemInDarkTheme()
    CompositionLocalProvider(LocalExtraColors provides if (useDark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (useDark) DarkScheme else LightScheme,
            content = content,
        )
    }
}
