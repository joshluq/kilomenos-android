package es.joshluq.kmsafe.ui.renting.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import es.joshluq.canvaskit.components.feedback.CanvasKitSkeleton
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R

/**
 * Component to select or display a vehicle photo.
 */
@Composable
fun VehiclePhotoSelector(
    modifier: Modifier = Modifier,
    imageUrl: String?,
    selectedUri: Uri?,
    isReadOnly: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(shape)
            .background(CanvasKitTheme.colors.backgroundPrimary)
            .border(1.dp, CanvasKitTheme.colors.borderSubtle, shape)
            .clickable(enabled = !isReadOnly, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val displayData = selectedUri ?: imageUrl

        if (displayData != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(displayData)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.onboarding_photo_label),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { CanvasKitSkeleton(modifier = Modifier.fillMaxSize()) },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CanvasKitTheme.colors.error.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = CanvasKitTheme.colors.textSecondary
                        )
                    }
                }
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.onboarding_photo_label),
                    tint = CanvasKitTheme.colors.textSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
