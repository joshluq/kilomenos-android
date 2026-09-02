package es.joshluq.kmsafe.feature.fleet.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme

/**
 * Common layout for Setup Wizard steps.
 * Provides consistent title, description, scrollable area, and action footer.
 */
@Composable
fun RentingStepLayout(
    title: String,
    description: String,
    primaryActionLabel: String,
    onPrimaryActionClick: () -> Unit,
    primaryActionEnabled: Boolean = true,
    primaryActionLoading: Boolean = false,
    secondaryAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CanvasKitTheme.spacing.screenHorizontal)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = CanvasKitTheme.typography.headingLarge,
                color = CanvasKitTheme.colors.textPrimary,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = CanvasKitTheme.typography.bodyLarge,
                color = CanvasKitTheme.colors.textSecondary,
                textAlign = TextAlign.Justify
            )
            Spacer(modifier = Modifier.height(32.dp))

            content()

            Spacer(modifier = Modifier.height(120.dp)) // Reserve space for footer
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(CanvasKitTheme.spacing.screenHorizontal)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CanvasKitButton(
                text = primaryActionLabel,
                onClick = onPrimaryActionClick,
                enabled = primaryActionEnabled,
                loading = primaryActionLoading,
                modifier = Modifier.fillMaxWidth()
            )

            if (secondaryAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                secondaryAction()
            }
        }
    }
}
