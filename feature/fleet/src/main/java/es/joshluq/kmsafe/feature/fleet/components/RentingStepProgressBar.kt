package es.joshluq.kmsafe.feature.fleet.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme

@Composable
fun RentingStepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val progress = currentStep.toFloat() / totalSteps.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "RentingStepProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(CanvasKitTheme.colors.borderSubtle.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(4.dp)
                .clip(CircleShape)
                .background(CanvasKitTheme.colors.brandAccent)
        )
    }
}
