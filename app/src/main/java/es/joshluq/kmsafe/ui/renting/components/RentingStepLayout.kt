package es.joshluq.kmsafe.ui.renting.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme

/**
 * Base layout for a single step in the setup wizard.
 * Provides consistent spacing for title, description, content, and primary action.
 */
@Composable
fun RentingStepLayout(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    primaryActionLabel: String,
    onPrimaryActionClick: () -> Unit,
    primaryActionEnabled: Boolean = true,
    primaryActionLoading: Boolean = false,
    secondaryAction: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = title,
            style = CanvasKitTheme.typography.headingLarge,
            color = CanvasKitTheme.colors.textPrimary,
            fontWeight = FontWeight.Black
        )

        if (description != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = CanvasKitTheme.typography.bodyLarge,
                color = CanvasKitTheme.colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Box(modifier = Modifier.weight(1f)) {
            Column {
                content()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CanvasKitButton(
            onClick = onPrimaryActionClick,
            enabled = primaryActionEnabled,
            loading = primaryActionLoading,
            modifier = Modifier.fillMaxWidth()
        ) { contentColor ->
            Text(text = primaryActionLabel, color = contentColor)
        }

        if (secondaryAction != null) {
            Spacer(modifier = Modifier.height(12.dp))
            secondaryAction()
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
