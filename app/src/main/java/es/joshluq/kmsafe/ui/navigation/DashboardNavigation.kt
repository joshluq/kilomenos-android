package es.joshluq.kmsafe.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import es.joshluq.kmsafe.core.navigation.Destination
import es.joshluq.kmsafe.core.navigation.DestinationListSaver
import es.joshluq.kmsafe.feature.dashboard.DashboardTab
import es.joshluq.kmsafe.feature.expenses.ExpensesRoute
import es.joshluq.kmsafe.feature.history.HistoryRoute
import es.joshluq.kmsafe.feature.overview.OverviewRoute
import es.joshluq.kmsafe.feature.profile.ProfileRoute
import es.joshluq.kmsafe.feature.projection.ProjectionAnalysisRoute

@Composable
fun DashboardNavigation(
    selectedTab: DashboardTab,
    onSelectTab: (DashboardTab) -> Unit,
    onNavigateToOnboarding: (String?, Boolean) -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToRecordDetail: (String) -> Unit,
    onNavigateToStations: () -> Unit,
    onNavigateToStationDetail: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToPremiumPaywall: (String) -> Unit = {},
    onNavigateToPermissions: () -> Unit,
    onNavigateToAssistedPermissions: () -> Unit,
    onNavigateToWelcomeDiscovery: (Boolean) -> Unit,
    onNavigateToVehicleDetail: (String) -> Unit,
    onNavigateToEditContract: (String) -> Unit = {}
) {
    val destinationForTab: (DashboardTab) -> Destination = { tab ->
        when (tab) {
            DashboardTab.OVERVIEW -> Destination.Overview
            DashboardTab.HISTORY -> Destination.History
            DashboardTab.EXPENSES -> Destination.Expenses()
            DashboardTab.PROJECTION -> Destination.ProjectionAnalysis
            DashboardTab.PROFILE -> Destination.Profile
        }
    }

    val backStack = rememberSaveable(saver = DestinationListSaver) {
        mutableStateListOf(destinationForTab(selectedTab))
    }

    LaunchedEffect(selectedTab) {
        val targetDestination = destinationForTab(selectedTab)
        if (backStack.lastOrNull() != targetDestination) {
            backStack.clear()
            backStack.add(targetDestination)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (selectedTab != DashboardTab.OVERVIEW) {
                onSelectTab(DashboardTab.OVERVIEW)
            }
        },
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(150))
        },
        popTransitionSpec = {
            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(150))
        },
        entryProvider = { key ->
            when (key) {
                Destination.Overview -> NavEntry(key) {
                    OverviewRoute(
                        onNavigateToOnboarding = onNavigateToOnboarding,
                        onNavigateToProjection = { onSelectTab(DashboardTab.PROJECTION) },
                        onNavigateToPermissions = onNavigateToPermissions,
                        onNavigateToAssistedPermissions = onNavigateToAssistedPermissions,
                        onNavigateToPremiumPaywall = { onNavigateToPremiumPaywall("overview") },
                        onNavigateToPreferences = onNavigateToPreferences,
                        onNavigateToWelcomeDiscovery = { onNavigateToWelcomeDiscovery(false) },
                        onNavigateToVehicleDetail = onNavigateToVehicleDetail
                    )
                }
                Destination.History -> NavEntry(key) {
                    HistoryRoute(
                        onNavigateToDetail = onNavigateToRecordDetail
                    )
                }
                Destination.ProjectionAnalysis -> NavEntry(key) {
                    ProjectionAnalysisRoute(
                        onNavigateToUpgrade = { source -> onNavigateToPremiumPaywall(source) },
                        onNavigateToEditContract = onNavigateToEditContract
                    )
                }
                is Destination.Expenses -> NavEntry(key) {
                    ExpensesRoute(
                        onNavigateToUpgrade = { onNavigateToPremiumPaywall("expenses") },
                        onNavigateToStations = onNavigateToStations,
                        onNavigateToStationDetail = onNavigateToStationDetail
                    )
                }
                Destination.Profile -> NavEntry(key) {
                    ProfileRoute(
                        onNavigateToVehicles = onNavigateToVehicles,
                        onNavigateToPreferences = onNavigateToPreferences,
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToPremiumPaywall = { onNavigateToPremiumPaywall("profile") },
                        onNavigateToWelcomeDiscovery = { onNavigateToWelcomeDiscovery(true) }
                    )
                }
                else -> NavEntry(key) { }
            }
        }
    )
}
