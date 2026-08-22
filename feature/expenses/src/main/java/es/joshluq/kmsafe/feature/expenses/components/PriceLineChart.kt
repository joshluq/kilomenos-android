package es.joshluq.kmsafe.feature.expenses.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLocale
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.domain.model.PricePoint

/**
 * A line chart that visualizes the evolution of fuel prices over time.
 * Includes horizontal reference lines for Min, Max, and Average prices.
 */
@Composable
fun PriceLineChart(
    history: List<PricePoint>,
    minPrice: Double,
    maxPrice: Double,
    avgPrice: Double,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    val locale = LocalLocale.current.platformLocale
    val textMeasurer = rememberTextMeasurer()
    val accentColor = CanvasKitTheme.colors.brandAccent
    val gridColor = CanvasKitTheme.colors.textSecondary.copy(alpha = 0.2f)
    val labelStyle = CanvasKitTheme.typography.labelSmall.copy(fontSize = 10.sp, color = CanvasKitTheme.colors.textSecondary)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val width = size.width
        val height = size.height
        val padding = 24.dp.toPx()

        // Normalize data
        val startTime = history.minOf { it.timestamp }
        val endTime = history.maxOf { it.timestamp }
        val timeRange = (endTime - startTime).coerceAtLeast(1L)
        
        // Add a bit of vertical buffer (10%)
        val priceRange = (maxPrice - minPrice).coerceAtLeast(0.01)
        val yMin = minPrice - (priceRange * 0.1)
        val yMax = maxPrice + (priceRange * 0.1)
        val normalizedPriceRange = yMax - yMin

        fun normalizeX(time: Long): Float = 
            padding + ((time - startTime).toFloat() / timeRange) * (width - 2 * padding)

        fun normalizeY(price: Double): Float = 
            height - padding - (((price - yMin).toFloat() / normalizedPriceRange.toFloat()) * (height - 2 * padding))

        // 1. Draw Grid / Reference Lines
        val referencePrices = listOf(minPrice, avgPrice, maxPrice)
        referencePrices.forEach { price ->
            val y = normalizeY(price)
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1.dp.toPx()
            )
            
            // Labels for Min/Max
            if (price == minPrice || price == maxPrice) {
                val label = String.format(locale, "%.3f", price)
                val textLayout = textMeasurer.measure(label, style = labelStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    style = labelStyle,
                    topLeft = Offset(width - textLayout.size.width, y - textLayout.size.height)
                )
            }
        }

        // 2. Draw Area Fill (Gradient)
        if (history.size > 1) {
            val fillPath = Path().apply {
                moveTo(normalizeX(history.first().timestamp), height - padding)
                history.forEach { point ->
                    lineTo(normalizeX(point.timestamp), normalizeY(point.unitPrice))
                }
                lineTo(normalizeX(history.last().timestamp), height - padding)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(accentColor.copy(alpha = 0.3f), Color.Transparent),
                    startY = normalizeY(maxPrice),
                    endY = height - padding
                )
            )
        }

        // 3. Draw Price Line
        if (history.size > 1) {
            val linePath = Path().apply {
                val firstPoint = history.first()
                moveTo(normalizeX(firstPoint.timestamp), normalizeY(firstPoint.unitPrice))
                history.drop(1).forEach { point ->
                    lineTo(normalizeX(point.timestamp), normalizeY(point.unitPrice))
                }
            }
            drawPath(
                path = linePath,
                color = accentColor,
                style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }

        // 4. Draw Points (Dots)
        history.forEach { point ->
            drawCircle(
                color = accentColor,
                radius = 4.dp.toPx(),
                center = Offset(normalizeX(point.timestamp), normalizeY(point.unitPrice))
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(normalizeX(point.timestamp), normalizeY(point.unitPrice))
            )
        }
    }
}
