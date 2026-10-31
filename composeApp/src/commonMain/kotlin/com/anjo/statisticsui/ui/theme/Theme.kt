package com.anjo.statisticsui.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

val ACCENTS =
    mapOf(
        "red" to Color(0xFFF44336),
        "deep-orange" to Color(0xFFFF5722),
        "orange" to Color(0xFFFF9800),
        "amber" to Color(0xFFFFC107),
        "yellow" to Color(0xFFFFEB3B),
        "lime" to Color(0xFFCDDC39),
        "light-green" to Color(0xFF8BC34A),
        "green" to Color(0xFF4CAF50),
        "teal" to Color(0xFF009688),
        "cyan" to Color(0xFF00BCD4),
        "light-blue" to Color(0xFF03A9F4),
        "blue" to Color(0xFF2196F3),
        "indigo" to Color(0xFF3F51B5),
        "deep-purple" to Color(0xFF673AB7),
        "purple" to Color(0xFF9C27B0),
        "pink" to Color(0xFFE91E63),
        "brown" to Color(0xFF795548),
        "blue-grey" to Color(0xFF607D8B),
    )

const val DEFAULT_ACCENT = "teal"

fun accentScheme(
    accent: String?,
    dark: Boolean,
): ColorScheme {
    val (hue, chroma) = (ACCENTS[accent] ?: ACCENTS.getValue(DEFAULT_ACCENT)).hueAndChroma()
    val p = Palette(hue, min(chroma, 56.0))
    val s = Palette(hue, 10.0)
    val t = Palette(hue + 60, 20.0)
    val e = Palette(36.0, 74.0)
    val n = Palette(hue, 2.5)
    val nv = Palette(hue, 4.0)

    fun Palette.tone(
        light: Int,
        darkTone: Int,
    ) = this[if (dark) darkTone else light]

    return lightColorScheme(
        primary = p.tone(40, 80),
        onPrimary = p.tone(100, 20),
        primaryContainer = p.tone(90, 30),
        onPrimaryContainer = p.tone(10, 90),
        inversePrimary = p.tone(80, 40),
        secondary = s.tone(40, 80),
        onSecondary = s.tone(100, 20),
        secondaryContainer = s.tone(90, 30),
        onSecondaryContainer = s.tone(10, 90),
        tertiary = t.tone(40, 80),
        onTertiary = t.tone(100, 20),
        tertiaryContainer = t.tone(90, 30),
        onTertiaryContainer = t.tone(10, 90),
        error = e.tone(40, 80),
        onError = e.tone(100, 20),
        errorContainer = e.tone(90, 30),
        onErrorContainer = e.tone(10, 90),
        background = n.tone(98, 6),
        onBackground = n.tone(10, 90),
        surface = n.tone(98, 6),
        onSurface = n.tone(10, 90),
        surfaceVariant = nv.tone(90, 30),
        onSurfaceVariant = nv.tone(30, 80),
        surfaceTint = p.tone(40, 80),
        inverseSurface = n.tone(20, 90),
        inverseOnSurface = n.tone(95, 20),
        outline = nv.tone(50, 60),
        outlineVariant = nv.tone(80, 30),
        scrim = Color.Black,
        surfaceDim = n.tone(87, 6),
        surfaceBright = n.tone(98, 24),
        surfaceContainerLowest = n.tone(100, 4),
        surfaceContainerLow = n.tone(96, 10),
        surfaceContainer = n.tone(94, 12),
        surfaceContainerHigh = n.tone(92, 17),
        surfaceContainerHighest = n.tone(90, 22),
    )
}

private class Palette(
    val hue: Double,
    val chroma: Double,
) {
    operator fun get(tone: Int): Color {
        val l = tone.toDouble()
        var fits = chroma
        if (lchToSrgb(l, chroma, hue) == null) {
            var low = 0.0
            var high = chroma
            repeat(20) {
                val mid = (low + high) / 2
                if (lchToSrgb(l, mid, hue) == null) high = mid else low = mid
            }
            fits = low
        }
        val (r, g, b) = lchToSrgb(l, fits, hue)!!
        return Color(r, g, b)
    }
}

private const val EPSILON = 216.0 / 24389
private const val KAPPA = 24389.0 / 27
private const val WHITE_X = 0.95047
private const val WHITE_Z = 1.08883

private fun lchToSrgb(
    l: Double,
    c: Double,
    h: Double,
): List<Float>? {
    fun inverse(t: Double) = if (t.pow(3) > EPSILON) t.pow(3) else (116 * t - 16) / KAPPA
    val fy = (l + 16) / 116
    val x = inverse(fy + c * cos(h * PI / 180) / 500) * WHITE_X
    val y = inverse(fy)
    val z = inverse(fy - c * sin(h * PI / 180) / 200) * WHITE_Z
    val linear =
        listOf(
            3.2406 * x - 1.5372 * y - 0.4986 * z,
            -0.9689 * x + 1.8758 * y + 0.0415 * z,
            0.0557 * x - 0.2040 * y + 1.0570 * z,
        )
    if (linear.any { it < -1e-4 || it > 1 + 1e-4 }) return null
    return linear.map {
        val v = it.coerceIn(0.0, 1.0)
        (if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055).toFloat()
    }
}

private fun Color.hueAndChroma(): Pair<Double, Double> {
    fun linear(v: Float) = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

    fun f(t: Double) = if (t > EPSILON) t.pow(1.0 / 3) else (KAPPA * t + 16) / 116
    val r = linear(red)
    val g = linear(green)
    val b = linear(blue)
    val fx = f((0.4124 * r + 0.3576 * g + 0.1805 * b) / WHITE_X)
    val fy = f(0.2126 * r + 0.7152 * g + 0.0722 * b)
    val fz = f((0.0193 * r + 0.1192 * g + 0.9505 * b) / WHITE_Z)
    val a = 500 * (fx - fy)
    val bb = 200 * (fy - fz)
    return (atan2(bb, a) * 180 / PI).mod(360.0) to hypot(a, bb)
}

private val ColorScheme.isLight get() = surface.luminance() > 0.5f

val ColorScheme.success get() = if (isLight) Color(0xFF1B6C2F) else Color(0xFF9AD6A3)

val ColorScheme.warning get() = if (isLight) Color(0xFF7A5900) else Color(0xFFF2C14D)
