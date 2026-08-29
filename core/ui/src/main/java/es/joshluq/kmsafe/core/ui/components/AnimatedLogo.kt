package es.joshluq.kmsafe.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme

/**
 * A specialized component that draws the KiloMenos "Intelligent Path" logo
 * with a high-end drawing animation.
 */
@Composable
fun AnimatedLogo(
    modifier: Modifier = Modifier,
    logoSize: Dp = 100.dp,
    pathColor: androidx.compose.ui.graphics.Color = CanvasKitTheme.colors.textPrimary,
    accentColor: androidx.compose.ui.graphics.Color = CanvasKitTheme.colors.brandAccent,
    animationDuration: Int = 1200
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration)
        )
    }

    Canvas(modifier = modifier.size(logoSize)) {
        val strokeWidth = (logoSize.toPx() * 0.1f)

        // Define Paths (normalized to 108x108 viewport as in XML)
        val scale = size.width / 108f

        val mainBarPath = Path().apply {
            moveTo(34f * scale, 24f * scale)
            lineTo(34f * scale, 84f * scale)
        }

        val upperArmPath = Path().apply {
            moveTo(34f * scale, 54f * scale)
            lineTo(74f * scale, 24f * scale)
        }

        val lowerArmPath = Path().apply {
            moveTo(34f * scale, 54f * scale)
            lineTo(74f * scale, 84f * scale)
        }

        // Helper to draw animated segments
        fun drawAnimatedPath(path: Path, color: androidx.compose.ui.graphics.Color) {
            val pathMeasure = PathMeasure()
            pathMeasure.setPath(path, false)
            val length = pathMeasure.length
            val segmentPath = Path()
            pathMeasure.getSegment(0f, length * progress.value, segmentPath)

            drawPath(
                path = segmentPath,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Draw main structure
        drawAnimatedPath(mainBarPath, pathColor)
        drawAnimatedPath(upperArmPath, pathColor)

        // Draw the "Intelligent Path" (Accent)
        drawAnimatedPath(lowerArmPath, accentColor)
    }
}
