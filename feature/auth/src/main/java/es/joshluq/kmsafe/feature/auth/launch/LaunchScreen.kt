package es.joshluq.kmsafe.feature.auth.launch

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.components.BrandingLogo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun LaunchRoute(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: LaunchViewModel = hiltViewModel()
) {
    LaunchScreen(
        effects = viewModel.effects,
        onNavigateToLogin = onNavigateToLogin,
        onNavigateToDashboard = onNavigateToDashboard
    )
}

@Composable
fun LaunchScreen(
    effects: Flow<LaunchEffect>,
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val backgroundColor = CanvasKitTheme.colors.backgroundPrimary
    val contentColor = CanvasKitTheme.colors.brandPrimary

    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                LaunchEffect.NavigateToDashboard -> onNavigateToDashboard()
                LaunchEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            BrandingLogo(
                logoSize = 100.dp,
                textColor = contentColor,
                textStyle = CanvasKitTheme.typography.displayMedium,
                animationDuration = 1200
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun LaunchScreenPreview() {
    CanvasKitTheme {
        LaunchScreen(
            effects = flowOf(),
            onNavigateToLogin = {},
            onNavigateToDashboard = {}
        )
    }
}
