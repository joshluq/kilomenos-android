package es.joshluq.kmsafe.ui.renting.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.inputs.CanvasKitTextField
import es.joshluq.canvaskit.components.inputs.CanvasKitTextFieldVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.ui.util.safeClickable

/**
 * Shared TextField for Renting related screens.
 */
@Composable
fun RentingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    errorMessage: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val spacing = CanvasKitTheme.spacing
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        CanvasKitTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            errorText = errorMessage,
            isError = errorMessage != null,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            enabled = enabled,
            variant = CanvasKitTextFieldVariant.Outlined,
            label = label
        )
    }
}

/**
 * Shared Display/Clickable field for Renting related screens.
 */
@Composable
fun RentingDisplayField(
    label: String,
    value: String,
    placeholder: String? = null,
    suffix: String? = null,
    onClick: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = CanvasKitTheme.colors
    val spacing = CanvasKitTheme.spacing
    val typography = CanvasKitTheme.typography
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Row {
            Spacer(modifier = Modifier.width(spacing.lg))
            Text(
                text = label,
                style = typography.labelSmall,
                color = if (errorMessage != null) colors.error else colors.textPrimary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 1.dp,
                    color = if (errorMessage != null) colors.error else colors.borderSubtle,
                    shape = CanvasKitTheme.shapes.pill
                )
                .clip(CanvasKitTheme.shapes.pill)
                .then(
                    if (onClick != null) {
                        Modifier.safeClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        color = colors.textSecondary,
                        style = typography.bodyMedium
                    )
                } else {
                    Text(
                        text = value,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (suffix != null) {
                        Text(
                            text = suffix,
                            style = typography.labelLarge,
                            color = colors.brandAccent
                        )
                    }
                    if (trailingIcon != null) {
                        if (suffix != null) Spacer(modifier = Modifier.width(CanvasKitTheme.spacing.xs))
                        trailingIcon()
                    }
                }
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = typography.labelSmall,
                color = colors.error,
                modifier = Modifier.padding(start = spacing.lg, top = 2.dp)
            )
        }
    }
}
