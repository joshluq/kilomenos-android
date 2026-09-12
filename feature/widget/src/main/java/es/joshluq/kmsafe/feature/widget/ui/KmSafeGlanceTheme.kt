package es.joshluq.kmsafe.feature.widget.ui

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * Color tokens adapted for Jetpack Glance reflecting the CanvasKit Design System.
 */
object KmSafeGlanceTheme {
    val Background = ColorProvider(Color(0xFF11141A))
    val Surface = ColorProvider(Color(0xFF1A1F29))
    val SurfaceHighlight = ColorProvider(Color(0xFF242B38))
    val BorderSubtle = ColorProvider(Color(0xFF2E3747))

    val BrandAccent = ColorProvider(Color(0xFFFF7A00))
    val Success = ColorProvider(Color(0xFF10B981))
    val Error = ColorProvider(Color(0xFFEF4444))

    val TextPrimary = ColorProvider(Color(0xFFF8FAFC))
    val TextSecondary = ColorProvider(Color(0xFF94A3B8))
    val TextMuted = ColorProvider(Color(0xFF64748B))

    val ActionBackground = ColorProvider(Color(0xFFFF7A00))
    val ActionContent = ColorProvider(Color(0xFFFFFFFF))
}
