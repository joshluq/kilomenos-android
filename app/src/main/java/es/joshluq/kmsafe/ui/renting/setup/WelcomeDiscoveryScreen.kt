package es.joshluq.kmsafe.ui.renting.setup

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.ui.util.safeClick
import kotlinx.coroutines.launch

@Composable
fun WelcomeDiscoveryScreen(
    onNavigateToRentingSetup: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasKitTheme.colors.backgroundPrimary)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            CanvasKitButton(
                variant = CanvasKitButtonVariant.Ghost,
                onClick = safeClick { onSkip() }
            ) {
                Text(
                    text = stringResource(R.string.welcome_discovery_action_skip),
                    color = CanvasKitTheme.colors.textSecondary
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            DiscoverySlide(
                slide = when (page) {
                    0 -> DiscoverySlideData(
                        title = stringResource(R.string.welcome_discovery_slide1_title),
                        description = stringResource(R.string.welcome_discovery_slide1_desc),
                        visual = { BalanceMockup() }
                    )
                    1 -> DiscoverySlideData(
                        title = stringResource(R.string.welcome_discovery_slide2_title),
                        description = stringResource(R.string.welcome_discovery_slide2_desc),
                        visual = { ContractMockup() }
                    )
                    else -> DiscoverySlideData(
                        title = stringResource(R.string.welcome_discovery_slide3_title),
                        description = stringResource(R.string.welcome_discovery_slide3_desc),
                        visual = { OdometerMockup() }
                    )
                }
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Pager Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { iteration ->
                    val color = if (pagerState.currentPage == iteration) {
                        CanvasKitTheme.colors.brandAccent
                    } else {
                        CanvasKitTheme.colors.borderSubtle
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            CanvasKitButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = safeClick {
                    if (pagerState.currentPage < 2) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onNavigateToRentingSetup()
                    }
                }
            ) { contentColor ->
                Text(
                    text = if (pagerState.currentPage < 2) {
                        stringResource(R.string.welcome_discovery_action_next)
                    } else {
                        stringResource(R.string.welcome_discovery_action_start)
                    },
                    color = contentColor,
                    style = CanvasKitTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DiscoverySlide(slide: DiscoverySlideData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1.2f),
            contentAlignment = Alignment.Center
        ) {
            slide.visual()
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = slide.title,
            style = CanvasKitTheme.typography.headingLarge,
            color = CanvasKitTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = slide.description,
            style = CanvasKitTheme.typography.bodyLarge,
            color = CanvasKitTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BalanceMockup() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(24.dp, shape = RoundedCornerShape(24.dp), ambientColor = CanvasKitTheme.colors.brandAccent)
            .background(CanvasKitTheme.colors.backgroundPrimary, RoundedCornerShape(24.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "BALANCE",
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary
            )
            Text(
                text = "+ 1.250 km",
                style = CanvasKitTheme.typography.displayLarge,
                color = CanvasKitTheme.colors.brandAccent,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
                    .background(CanvasKitTheme.colors.brandAccent, CircleShape)
            )
        }
    }
}

@Composable
private fun ContractMockup() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(24.dp, shape = RoundedCornerShape(24.dp))
            .background(CanvasKitTheme.colors.backgroundPrimary, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (i == 2) 0.6f else 1f)
                        .height(48.dp)
                        .background(CanvasKitTheme.colors.backgroundSecondary, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(modifier = Modifier.size(32.dp, 8.dp).background(CanvasKitTheme.colors.borderSubtle, CircleShape))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            CanvasKitButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("SAVE CONTRACT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OdometerMockup() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(24.dp, shape = RoundedCornerShape(24.dp))
            .background(CanvasKitTheme.colors.backgroundPrimary, RoundedCornerShape(24.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.AddRoad, 
                contentDescription = null, 
                modifier = Modifier.size(72.dp),
                tint = CanvasKitTheme.colors.brandAccent
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "125.400",
                style = CanvasKitTheme.typography.displayMedium,
                color = CanvasKitTheme.colors.textPrimary,
                fontWeight = FontWeight.Black
            )
            Text(
                "ODOMETER",
                style = CanvasKitTheme.typography.labelSmall,
                color = CanvasKitTheme.colors.textSecondary
            )
        }
    }
}

private data class DiscoverySlideData(
    val title: String,
    val description: String,
    val visual: @Composable () -> Unit
)

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun WelcomeDiscoveryScreenPreview() {
    CanvasKitTheme {
        WelcomeDiscoveryScreen(
            onNavigateToRentingSetup = {},
            onSkip = {}
        )
    }
}
