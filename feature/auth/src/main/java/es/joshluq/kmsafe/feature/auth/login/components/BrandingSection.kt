package es.joshluq.kmsafe.feature.auth.login.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.components.BrandingLogo

/**
 * A reusable branding header for the onboarding and authentication screens.
 * Projects the "Expert Co-Pilot" persona with immersive visuals.
 */
@Composable
fun BrandingSection(
    modifier: Modifier = Modifier,
    subtitle: String,
    dark: Boolean = isSystemInDarkTheme(),
    topPadding: Boolean = false
) {
    val theme = CanvasKitTheme
    val textColor = if (dark) theme.colors.textPrimary else theme.colors.backgroundPrimary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (dark) theme.colors.backgroundPrimary else theme.colors.brandPrimary)
            .padding(top = 64.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(if (topPadding) 64.dp else 0.dp))

        BrandingLogo(
            logoSize = 80.dp,
            textColor = textColor,
            textStyle = theme.typography.displayMedium,
            animationDuration = 0 // Instant in this section to avoid distracting from subtitle
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = theme.typography.bodyLarge,
            color = theme.colors.backgroundPrimary.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
