package es.joshluq.kmsafe.feature.fleet.setup

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.feature.fleet.R
import kotlinx.coroutines.launch

@Composable
fun WelcomeDiscoveryScreen(
    onNavigateToRentingSetup: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CanvasKitTheme.colors.backgroundPrimary
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> DiscoverySlide(
                        title = stringResource(R.string.welcome_discovery_slide1_title),
                        description = stringResource(R.string.welcome_discovery_slide1_desc),
                        icon = Icons.Default.AccountBalanceWallet
                    )
                    1 -> DiscoverySlide(
                        title = stringResource(R.string.welcome_discovery_slide2_title),
                        description = stringResource(R.string.welcome_discovery_slide2_desc),
                        icon = Icons.Default.DirectionsCar
                    )
                    2 -> DiscoverySlide(
                        title = stringResource(R.string.welcome_discovery_slide3_title),
                        description = stringResource(R.string.welcome_discovery_slide3_desc),
                        icon = Icons.Default.Speed
                    )
                }
            }

            // Top Skip Action
            CanvasKitButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(CanvasKitTheme.spacing.md),
                variant = CanvasKitButtonVariant.Ghost,
                onClick = onSkip,

            ) { contentColor ->
                Text(
                    text = stringResource(R.string.welcome_discovery_action_skip),
                    style = CanvasKitTheme.typography.labelLarge,
                    color = contentColor
                )
            }

            // Bottom Navigation & Actions
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(CanvasKitTheme.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        CanvasKitTheme.colors.brandAccent
                                    } else {
                                        CanvasKitTheme.colors.textSecondary.copy(alpha = 0.3f)
                                    }
                                )
                        )
                    }
                }

                if (pagerState.currentPage < 2) {
                    CanvasKitButton(
                        text = stringResource(R.string.welcome_discovery_action_next),
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    CanvasKitButton(
                        text = stringResource(R.string.welcome_discovery_action_start),
                        onClick = onNavigateToRentingSetup,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverySlide(
    title: String,
    description: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(CanvasKitTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CanvasKitTheme.colors.brandAccent,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = title,
            style = CanvasKitTheme.typography.headingLarge,
            color = CanvasKitTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = CanvasKitTheme.typography.bodyLarge,
            color = CanvasKitTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun WelcomeDiscoveryScreenPreview() {
    CanvasKitTheme {
        WelcomeDiscoveryScreen(onNavigateToRentingSetup = {}, onSkip = {})
    }
}
