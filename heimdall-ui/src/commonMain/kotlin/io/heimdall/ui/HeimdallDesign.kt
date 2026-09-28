package io.heimdall.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme

data class HeimdallPalette(
    val background: Color,
    val onBackground: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val code: Color,
    val onCode: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val error: Color,
    val border: Color,
)

object HeimdallDesign {
    val background = Color(0xFF0F1018)
    val onBackground = Color(0xFFF3F6FC)
    val primary = Color(0xFFE9A63A)
    val onPrimary = Color(0xFF211300)
    val primaryContainer = Color(0xFF5D3B12)
    val onPrimaryContainer = Color(0xFFFFE4AE)
    val surface = Color(0xFF182331)
    val onSurface = Color(0xFFF3F6FC)
    val surfaceVariant = Color(0xFF202D3D)
    val onSurfaceVariant = Color(0xFFB9C7DF)
    val code = Color(0xFF171A24)
    val onCode = Color(0xFFE1E8F5)
    val success = Color(0xFF9FE6B5)
    val onSuccess = Color(0xFF082015)
    val warning = Color(0xFFFFD38A)
    val error = Color(0xFFFFB4B4)
    val border = Color(0xFF34465B)
    val corner = 8.dp
    val popupCorner = 18.dp
    val bodySize = 13.sp
    val labelSize = 11.sp
    val codeFont = FontFamily.Monospace

    @androidx.compose.runtime.Composable
    fun palette(): HeimdallPalette = if (isSystemInDarkTheme()) {
        HeimdallPalette(background, onBackground, primary, onPrimary, primaryContainer, onPrimaryContainer, surface, onSurface, surfaceVariant, onSurfaceVariant, code, onCode, success, onSuccess, warning, error, border)
    } else {
        HeimdallPalette(
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF10212E),
            primary = Color(0xFF9A5A00),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE0A3),
            onPrimaryContainer = Color(0xFF321A00),
            surface = Color(0xFFEAF1F5),
            onSurface = Color(0xFF10212E),
            surfaceVariant = Color(0xFFD7E5ED),
            onSurfaceVariant = Color(0xFF405466),
            code = Color(0xFFE8EEF2),
            onCode = Color(0xFF1C2B36),
            success = Color(0xFF176B3A),
            onSuccess = Color.White,
            warning = Color(0xFF8A5200),
            error = Color(0xFF9B1C1C),
            border = Color(0xFFB6C6D1),
        )
    }
}
