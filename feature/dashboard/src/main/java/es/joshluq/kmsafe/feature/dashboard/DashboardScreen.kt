package es.joshluq.kmsafe.feature.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.joshluq.canvaskit.components.navigation.CanvasKitBottomBar
import es.joshluq.canvaskit.components.navigation.CanvasKitBottomBarItem
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.text.asString
import es.joshluq.kmsafe.core.ui.R as CoreR
import es.joshluq.kmsafe.core.ui.components.AppExecutiveHudOverlay
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.domain.model.AppOverlayState

/**
 * Route Composable connecting the ViewModel to the DashboardScreen.
 */
@Composable
fun DashboardRoute(
    navigationContent: @Composable (selectedTab: DashboardTab, onSelectTab: (DashboardTab) -> Unit) -> Unit
) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state = viewModel.state.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state.value,
        onEvent = viewModel::sendEvent,
        navigationContent = navigationContent
    )
}

/**
 * DashboardScreen acts as a container for the main application navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: State,
    onEvent: (Event) -> Unit,
    navigationContent: @Composable (selectedTab: DashboardTab, onSelectTab: (DashboardTab) -> Unit) -> Unit
) {
    BackHandler(enabled = state.selectedTab != DashboardTab.OVERVIEW && !state.isNavigationBlocked) {
        onEvent(Event.OnTabSelected(DashboardTab.OVERVIEW))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                DashboardNavigationBar(state, onEvent)
            },
            containerColor = CanvasKitTheme.colors.backgroundPrimary,
            contentWindowInsets = WindowInsets.navigationBars
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                navigationContent(state.selectedTab) { tab ->
                    onEvent(Event.OnTabSelected(tab))
                }
            }
        }

        val activeOverlay = if (state.hudOverlayState !is AppOverlayState.None) {
            state.hudOverlayState
        } else if (state.isSwitchingVehicle) {
            AppOverlayState.VehicleSwitching(state.switchingVehicleName)
        } else {
            AppOverlayState.None
        }

        // Executive HUD Overlay (Elevated at Dashboard root level to block Scaffold & NavigationBar)
        AppExecutiveHudOverlay(state = activeOverlay)
    }
}

@Composable
private fun DashboardNavigationBar(
    state: State,
    onEvent: (Event) -> Unit
) {
    val selectedTab = state.selectedTab
    val home = TextProvider.Resource(CoreR.string.dashboard_item_home)
    val history = TextProvider.Resource(CoreR.string.dashboard_item_history)
    val expenses = TextProvider.Resource(CoreR.string.dashboard_item_expenses)
    val projection = TextProvider.Resource(CoreR.string.dashboard_item_projection)
    val profile = TextProvider.Resource(CoreR.string.dashboard_item_profile)

    CanvasKitBottomBar {
        CanvasKitBottomBarItem(
            selected = selectedTab == DashboardTab.OVERVIEW,
            onClick = safeClick { onEvent(Event.OnTabSelected(DashboardTab.OVERVIEW)) },
            icon = { tint -> Icon(Icons.Default.Home, contentDescription = home.asString(), tint = tint) },
            label = { tint -> Text(home.asString(), color = tint, style = CanvasKitTheme.typography.labelSmall) }
        )
        CanvasKitBottomBarItem(
            selected = selectedTab == DashboardTab.HISTORY,
            onClick = safeClick { onEvent(Event.OnTabSelected(DashboardTab.HISTORY)) },
            icon = { tint ->
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = history.asString(),
                    tint = tint
                )
            },
            label = { tint -> Text(history.asString(), color = tint, style = CanvasKitTheme.typography.labelSmall) },
        )

        if (state.hasRentingContract) {
            CanvasKitBottomBarItem(
                selected = selectedTab == DashboardTab.PROJECTION,
                onClick = safeClick { onEvent(Event.OnTabSelected(DashboardTab.PROJECTION)) },
                icon = { tint ->
                    Icon(
                        Icons.Default.Timeline,
                        contentDescription = projection.asString(),
                        tint = tint
                    )
                },
                label = { tint ->
                    Text(
                        projection.asString(),
                        color = tint,
                        style = CanvasKitTheme.typography.labelSmall
                    )
                }
            )

            CanvasKitBottomBarItem(
                selected = selectedTab == DashboardTab.EXPENSES,
                onClick = safeClick { onEvent(Event.OnTabSelected(DashboardTab.EXPENSES)) },
                icon = { tint ->
                    Icon(
                        Icons.Default.LocalGasStation,
                        contentDescription = expenses.asString(),
                        tint = tint
                    )
                },
                label = { tint ->
                    Text(
                        expenses.asString(),
                        color = tint,
                        style = CanvasKitTheme.typography.labelSmall
                    )
                }
            )
        }
        CanvasKitBottomBarItem(
            selected = selectedTab == DashboardTab.PROFILE,
            onClick = safeClick { onEvent(Event.OnTabSelected(DashboardTab.PROFILE)) },
            icon = { tint -> Icon(Icons.Default.Person, contentDescription = profile.asString(), tint = tint) },
            label = { tint -> Text(profile.asString(), color = tint, style = CanvasKitTheme.typography.labelSmall) },
        )
    }
}
