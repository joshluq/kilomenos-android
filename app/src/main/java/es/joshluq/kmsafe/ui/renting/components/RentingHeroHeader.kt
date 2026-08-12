package es.joshluq.kmsafe.ui.renting.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import es.joshluq.canvaskit.components.feedback.CanvasKitSkeleton
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme

/**
 * Prominent header showing vehicle photo and name.
 * Used in Detail and Edit screens.
 */
@Composable
fun RentingHeroHeader(
    vehicleName: String,
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(CanvasKitTheme.colors.backgroundSecondary),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { CanvasKitSkeleton(modifier = Modifier.fillMaxSize()) }
                )
            } else {
                // Placeholder initial or icon
                Text(
                    text = vehicleName.take(1).uppercase(),
                    style = CanvasKitTheme.typography.displayLarge,
                    color = CanvasKitTheme.colors.brandAccent,
                    fontWeight = FontWeight.Black
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = vehicleName,
            style = CanvasKitTheme.typography.headingLarge,
            color = CanvasKitTheme.colors.textPrimary,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
