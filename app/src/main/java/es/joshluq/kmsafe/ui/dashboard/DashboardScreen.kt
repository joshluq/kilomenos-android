package es.joshluq.kmsafe.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import es.joshluq.canvaskit.components.navigation.CanvasKitBottomBar
import es.joshluq.canvaskit.components.navigation.CanvasKitBottomBarItem
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.ui.navigation.DashboardNavigation
import es.joshluq.kmsafe.ui.navigation.Destination
import es.joshluq.kmsafe.ui.util.safeClick
import kotlinx.coroutines.flow.Flow

/**
 * Route Composable connecting the ViewModel to the DashboardScreen.
 */
@Composable
fun DashboardRoute(
    navController: NavHostController,
    onNavigateToOnboarding: (String?, Boolean) -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToDataManagement: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToRecordDetail: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToPremiumPaywall: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToWelcomeDiscovery: () -> Unit,
    onNavigateToVehicleDetail: (String) -> Unit
) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state = viewModel.state.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state.value,
        effects = viewModel.effects,
        onEvent = viewModel::sendEvent,
        appNavController = navController,
        onNavigateToOnboarding = onNavigateToOnboarding,
        onNavigateToVehicles = onNavigateToVehicles,
        onNavigateToDataManagement = onNavigateToDataManagement,
        onNavigateToPreferences = onNavigateToPreferences,
        onNavigateToRecordDetail = onNavigateToRecordDetail,
        onNavigateToLogin = onNavigateToLogin,
        onNavigateToPremiumPaywall = onNavigateToPremiumPaywall,
        onNavigateToPermissions = onNavigateToPermissions,
        onNavigateToWelcomeDiscovery = onNavigateToWelcomeDiscovery,
        onNavigateToVehicleDetail = onNavigateToVehicleDetail
    )
}

/**
 * DashboardScreen acts as a container for the main application navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: State,
    effects: Flow<Effect>? = null,
    onEvent: (Event) -> Unit,
    appNavController: NavHostController,
    onNavigateToOnboarding: (String?, Boolean) -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToDataManagement: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToRecordDetail: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToPremiumPaywall: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToWelcomeDiscovery: () -> Unit,
    onNavigateToVehicleDetail: (String) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination

    LaunchedEffect(currentDestination) {
        currentDestination?.let { dest ->
            val tab = when {
                dest.hasRoute<Destination.Overview>() -> DashboardTab.OVERVIEW
                dest.hasRoute<Destination.History>() -> DashboardTab.HISTORY
                dest.hasRoute<Destination.ProjectionAnalysis>() -> DashboardTab.PROJECTION
                dest.hasRoute<Destination.Profile>() -> DashboardTab.PROFILE
                else -> null
            }
            tab?.let { onEvent(Event.OnTabSynced(it)) }
        }
    }

    effects?.let {
        navController.HandleEffects(effects)
    }

    BackHandler(enabled = state.selectedTab != DashboardTab.OVERVIEW) {
        onEvent(Event.OnTabSelected(DashboardTab.OVERVIEW))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasKitTheme.colors.backgroundSecondary),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(modifier = Modifier.padding(bottom = 104.dp)) {
            DashboardNavigation(
                navController = navController,
                appNavController = appNavController,
                onNavigateToOnboarding = onNavigateToOnboarding,
                onNavigateToVehicles = onNavigateToVehicles,
                onNavigateToDataManagement = onNavigateToDataManagement,
                onNavigateToPreferences = onNavigateToPreferences,
                onNavigateToRecordDetail = onNavigateToRecordDetail,
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToPremiumPaywall = onNavigateToPremiumPaywall,
                onNavigateToPermissions = onNavigateToPermissions,
                onNavigateToWelcomeDiscovery = onNavigateToWelcomeDiscovery,
                onNavigateToVehicleDetail = onNavigateToVehicleDetail
            )
        }

        Spacer(
            modifier = Modifier
                .height(CanvasKitTheme.spacing.lg)
                .fillMaxWidth()
                .background(CanvasKitTheme.colors.backgroundPrimary)
        )
        DashboardNavigationBar(state, onEvent)
    }
}

@Composable
private fun NavHostController.HandleEffects(
    effects: Flow<Effect>
) {
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is Effect.NavigateToTab -> {
                    val route = when (effect.tab) {
                        DashboardTab.OVERVIEW -> Destination.Overview
                        DashboardTab.HISTORY -> Destination.History
                        DashboardTab.PROJECTION -> Destination.ProjectionAnalysis
                        DashboardTab.PROFILE -> Destination.Profile
                    }
                    navigate(route) {
                        popUpTo(graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardNavigationBar(
    state: State,
    onEvent: (Event) -> Unit
) {
    val selectedTab = state.selectedTab
    val home = TextProvider.Resource(R.string.dashboard_item_home)
    val history = TextProvider.Resource(R.string.dashboard_item_history)
    val projection = TextProvider.Resource(R.string.dashboard_item_projection)
    val profile = TextProvider.Resource(R.string.dashboard_item_profile)

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
        }
        CanvasKitBottomBarItem(
            selected = selectedTab == DashboardTab.PROFILE,
            onClick = safeClick { onEvent(Event.OnTabSelected(DashboardTab.PROFILE)) },
            icon = { tint -> Icon(Icons.Default.Person, contentDescription = profile.asString(), tint = tint) },
            label = { tint -> Text(profile.asString(), color = tint, style = CanvasKitTheme.typography.labelSmall) },
        )
    }
}
