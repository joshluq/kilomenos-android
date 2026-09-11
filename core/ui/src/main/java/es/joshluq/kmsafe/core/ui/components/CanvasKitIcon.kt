package es.joshluq.kmsafe.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.icons.CanvasKitIcons

/**
 * Standard Design System Icon component that wraps [ImageVector] adhering to
 * CanvasKit design tokens and European Accessibility Act (EAA) guidelines.
 *
 * @param imageVector The vector asset to display, typically from [CanvasKitIcons].
 * @param contentDescription Mandatory accessibility description for screen readers.
 * @param modifier Modifier applied to the icon.
 * @param tint Color to apply to the icon strokes and fills. Defaults to [CanvasKitTheme.colors.textPrimary].
 * @param size Target dimension for the icon. Defaults to 24.dp.
 */
@Composable
fun CanvasKitIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = CanvasKitTheme.colors.textPrimary,
    size: Dp = 24.dp
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CanvasKitIconPreview() {
    CanvasKitTheme {
        Surface(color = CanvasKitTheme.colors.backgroundPrimary) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                CanvasKitIcon(
                    imageVector = CanvasKitIcons.Navigation.HorizonRunway,
                    contentDescription = "Runway Horizon",
                    tint = CanvasKitTheme.colors.brandAccent
                )
            }
        }
    }
}
