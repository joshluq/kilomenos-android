package es.joshluq.kmsafe.feature.projection.components

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.feature.projection.R
import kotlin.math.absoluteValue
import es.joshluq.kmsafe.core.ui.R as CoreR

/**
 * A visual gauge representing the projected mileage versus the contract limit.
 *
 * @param projectedKms The total kilometers projected at the end of the contract.
 * @param limitKms The total kilometers included in the contract.
 * @param balance The current balance (limit - projected).
 * @param modifier Modifier for the container.
 */
@Composable
fun ProjectionGauge(
    projectedKms: Double,
    limitKms: Double,
    balance: Double,
    modifier: Modifier = Modifier
) {
    val percentage = (projectedKms.toFloat() / limitKms.toFloat()).coerceIn(0f, 1.2f)
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 800),
        label = "GaugeAnimation"
    )

    val isOverLimit = balance < 0
    val accentColor = if (isOverLimit) CanvasKitTheme.colors.error else CanvasKitTheme.colors.brandAccent
    val backgroundColor = CanvasKitTheme.colors.borderSubtle

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()

            // Background Arc (180 degrees)
            drawArc(
                color = backgroundColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress Arc
            val sweepAngle = (animatedPercentage.coerceAtMost(1f) * 180f)
            drawArc(
                color = accentColor,
                startAngle = 180f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Danger Zone (if over 100%)
            if (animatedPercentage > 1f) {
                val dangerSweep = ((animatedPercentage - 1f) * 180f).coerceAtMost(36f)
                drawArc(
                    color = Color.Red,
                    startAngle = 0f,
                    sweepAngle = dangerSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth + 4f, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-10).dp)
        ) {
            Text(
                text = stringResource(
                    if (isOverLimit) CoreR.string.common_km_negative_suffix else CoreR.string.common_km_positive_suffix,
                    NumberFormatter.formatDistance(balance.absoluteValue)
                ),
                style = CanvasKitTheme.typography.displayMedium,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Black
            )
            Text(
                text = stringResource(R.string.projection_card_expected_balance).uppercase(),
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary,
                letterSpacing = 1.sp
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun ProjectionGaugePreview() {
    CanvasKitTheme {
        androidx.compose.material3.Surface(color = CanvasKitTheme.colors.backgroundPrimary) {
            ProjectionGauge(
                projectedKms = 12000.0,
                limitKms = 10000.0,
                balance = -2000.0,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
