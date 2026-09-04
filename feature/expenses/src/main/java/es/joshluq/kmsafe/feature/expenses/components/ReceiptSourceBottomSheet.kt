package es.joshluq.kmsafe.feature.expenses.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonSize
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.feature.expenses.R

/**
 * BottomSheet presented when tapping the AI Camera Floating Action Button.
 * Prompts the user to either snap a photo directly or pick an image from the gallery.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptSourceBottomSheet(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CanvasKitTheme.colors.backgroundPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = CanvasKitTheme.spacing.md)
                .padding(bottom = CanvasKitTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(CanvasKitTheme.spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.expenses_scan_source_title),
                style = CanvasKitTheme.typography.headingMedium,
                fontWeight = FontWeight.Bold,
                color = CanvasKitTheme.colors.textPrimary
            )

            Text(
                text = stringResource(R.string.expenses_scan_source_desc),
                style = CanvasKitTheme.typography.bodyMedium,
                color = CanvasKitTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(CanvasKitTheme.spacing.xs))

            CanvasKitButton(
                text = stringResource(R.string.expenses_scan_source_camera),
                variant = CanvasKitButtonVariant.Primary,
                size = CanvasKitButtonSize.Medium,
                icon = Icons.Default.CameraAlt,
                onClick = safeClick {
                    onDismiss()
                    onCameraClick()
                },
                modifier = Modifier.fillMaxWidth()
            )

            CanvasKitButton(
                text = stringResource(R.string.expenses_scan_source_gallery),
                variant = CanvasKitButtonVariant.Secondary,
                size = CanvasKitButtonSize.Medium,
                icon = Icons.Default.Description,
                onClick = safeClick {
                    onDismiss()
                    onGalleryClick()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
