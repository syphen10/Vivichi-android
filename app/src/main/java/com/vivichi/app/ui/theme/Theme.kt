package com.vivichi.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette lifted 1:1 from the original CSS custom properties.
val Pink = Color(0xFFFF85A2)
val PinkLight = Color(0xFFFFB3C6)
val PinkDark = Color(0xFFE8607E)
val Purple = Color(0xFFC4B0FF)
val PurpleDark = Color(0xFF9B85E8)
val Yellow = Color(0xFFFFD166)
val Green = Color(0xFFA8E6CF)
val GreenDark = Color(0xFF4DD9AC)
val Orange = Color(0xFFFF9A3C)
val Red = Color(0xFFFF6B6B)
val BgPink = Color(0xFFFFF0F7)
val TextDark = Color(0xFF3D2E4E)
val SoftText = Color(0xFF9B8BB8)
val MutedText = Color(0xFFC8BEDD)
val BorderPink = Color(0x2EFF85A2)

private val VivichiColors = lightColorScheme(
    primary = PinkDark,
    onPrimary = Color.White,
    secondary = PurpleDark,
    onSecondary = Color.White,
    background = BgPink,
    onBackground = TextDark,
    surface = Color.White,
    onSurface = TextDark,
    error = Red
)

val VivichiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val VivichiTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 19.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
)

// Season-tinted gradient for whichever seasonal outfit is currently equipped.
// Falls back to the default Purple/Pink brand gradient when no seasonal outfit is worn.
fun seasonalGradient(equippedOutfit: String): List<Color> = when (equippedOutfit) {
    "spring" -> listOf(Color(0xFF6FCF97), Color(0xFF27AE60))
    "summer" -> listOf(Yellow, Orange)
    "autumn" -> listOf(Color(0xFFFF9A62), Color(0xFFD35400))
    "winter" -> listOf(Color(0xFF85C1E9), Color(0xFF2980B9))
    "ocean" -> listOf(Color(0xFF7FD1F5), Color(0xFF3A8FD8))
    "mint" -> listOf(Color(0xFF8EE3C8), Color(0xFF3CB38A))
    "lavender" -> listOf(Color(0xFFC9B6FF), Color(0xFF8E7CF0))
    "sunset" -> listOf(Color(0xFFFFB38A), Color(0xFFF2668B))
    "candy" -> listOf(Color(0xFFFF9CCB), Color(0xFFB18CFF))
    "rainbow" -> listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4), Color(0xFFA1C4FD))
    "galaxy" -> listOf(Color(0xFF7A6CF0), Color(0xFFE06BC8))
    else -> listOf(Purple, Pink)
}

// Season-tinted app-wide background wash for whichever seasonal outfit is equipped.
// This is the actual page background (not just an accent/text color).
fun seasonalBackground(equippedOutfit: String): Color = when (equippedOutfit) {
    "spring" -> Color(0xFFEAFAF1)
    "summer" -> Color(0xFFFEF9E7)
    "autumn" -> Color(0xFFFEF0E6)
    "winter" -> Color(0xFFEBF5FB)
    "ocean" -> Color(0xFFE6F4FB)
    "mint" -> Color(0xFFE9FBF3)
    "lavender" -> Color(0xFFF3EEFF)
    "sunset" -> Color(0xFFFFF0E8)
    "candy" -> Color(0xFFFFEAF5)
    "rainbow" -> Color(0xFFFFF3F0)
    "galaxy" -> Color(0xFFEFEAFF)
    else -> BgPink
}

@Composable
fun VivichiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VivichiColors,
        typography = VivichiTypography,
        shapes = VivichiShapes,
        content = content
    )
}
