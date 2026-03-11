package es.joshluq.kmsafe.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * A helper function that wraps an [onClick] lambda with a debounce mechanism
 * to prevent multiple rapid successive clicks.
 *
 * @param debounceTime The time in milliseconds to ignore subsequent clicks. Default is 500ms.
 * @param onClick The original click lambda to execute.
 * @return A wrapped lambda that includes the debounce logic.
 */
@Composable
fun safeClick(
    debounceTime: Long = 500L,
    onClick: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    return {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > debounceTime) {
            lastClickTime = currentTime
            onClick()
        }
    }
}

/**
 * A custom [Modifier] that provides a "safe" clickable behavior by preventing
 * rapid successive clicks (debouncing).
 *
 * @param debounceTime The time in milliseconds to ignore subsequent clicks. Default is 500ms.
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch press events.
 * @param indication [androidx.compose.foundation.Indication] to show for this clickable.
 * @param enabled Controls the enabled state of the clickable.
 * @param onClick The original click lambda to execute.
 */
fun Modifier.safeClickable(
    debounceTime: Long = 500L,
    interactionSource: MutableInteractionSource? = null,
    indication: androidx.compose.foundation.Indication? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val safeOnClick = safeClick(debounceTime, onClick)
    this.clickable(
        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        indication = indication,
        enabled = enabled,
        onClick = safeOnClick
    )
}
