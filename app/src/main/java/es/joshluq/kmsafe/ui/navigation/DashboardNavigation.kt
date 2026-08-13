package es.joshluq.kmsafe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import es.joshluq.kmsafe.ui.history.HistoryRoute
import es.joshluq.kmsafe.ui.overview.OverviewRoute
import es.joshluq.kmsafe.ui.profile.ProfileRoute
import es.joshluq.kmsafe.ui.projection.ProjectionAnalysisRoute

@Composable
fun DashboardNavigation(
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
    NavHost(
        navController = navController,
        startDestination = Destination.Overview
    ) {
        composable<Destination.Overview> {
            OverviewRoute(
                onNavigateToOnboarding = onNavigateToOnboarding,
                onNavigateToProjection = {
                    navController.navigate(Destination.ProjectionAnalysis) {
                        launchSingleTop = true
                    }
                },
                onNavigateToPermissions = onNavigateToPermissions,
                onNavigateToPremiumPaywall = onNavigateToPremiumPaywall,
                onNavigateToPreferences = onNavigateToPreferences,
                onNavigateToWelcomeDiscovery = onNavigateToWelcomeDiscovery,
                onNavigateToVehicleDetail = onNavigateToVehicleDetail
            )
        }

        composable<Destination.History> {
            HistoryRoute(
                onNavigateToDetail = onNavigateToRecordDetail
            )
        }

        composable<Destination.ProjectionAnalysis> {
            ProjectionAnalysisRoute()
        }

        composable<Destination.Profile> {
            ProfileRoute(
                onNavigateToDataManagement = onNavigateToDataManagement,
                onNavigateToVehicles = onNavigateToVehicles,
                onNavigateToPreferences = onNavigateToPreferences,
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToPremiumPaywall = onNavigateToPremiumPaywall,
                onNavigateToWelcomeDiscovery = onNavigateToWelcomeDiscovery
            )
        }
    }
}
