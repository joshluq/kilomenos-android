package es.joshluq.kmsafe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import es.joshluq.kmsafe.ui.common.cropper.CropImageScreen
import es.joshluq.kmsafe.ui.dashboard.DashboardRoute
import es.joshluq.kmsafe.ui.datamanagement.DataManagementRoute
import es.joshluq.kmsafe.ui.launch.LaunchRoute
import es.joshluq.kmsafe.ui.login.LoginRoute
import es.joshluq.kmsafe.ui.onboarding.OnboardingRoute
import es.joshluq.kmsafe.ui.premium.PremiumPaywallRoute
import es.joshluq.kmsafe.ui.profile.preferences.PreferencesRoute
import es.joshluq.kmsafe.ui.history.detail.RecordDetailRoute
import es.joshluq.kmsafe.ui.profile.vehicles.VehicleListRoute
import es.joshluq.kmsafe.ui.signup.SignupRoute

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onLaunchBilling: () -> Unit = {},
    onShowPrivacyOptions: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Launch
    ) {
        composable<Destination.Launch> {
            LaunchRoute(
                onNavigateToLogin = {
                    navController.navigate(Destination.Login) {
                        popUpTo(Destination.Launch) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Launch) { inclusive = true }
                    }
                }
            )
        }

        composable<Destination.Login> {
            LoginRoute(
                onNavigateToDashboard = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Login) { inclusive = true }
                    }
                },
                onNavigateToPremiumPaywall = {
                    // Navigate to Dashboard as base and clear auth stack
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Login) { inclusive = true }
                    }
                    // Then show paywall on top
                    navController.navigate(Destination.PremiumPaywall)
                },
                onNavigateToSignup = {
                    navController.navigate(Destination.Signup)
                }
            )
        }

        composable<Destination.Signup> {
            SignupRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDashboard = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Login) { inclusive = true }
                    }
                },
                onNavigateToPremiumPaywall = {
                    // Navigate to Dashboard as base and clear auth stack
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Login) { inclusive = true }
                    }
                    // Then show paywall on top
                    navController.navigate(Destination.PremiumPaywall)
                }
            )
        }

        composable<Destination.PremiumPaywall> {
            PremiumPaywallRoute(
                onNavigateToDashboard = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Login) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLaunchBilling = onLaunchBilling
            )
        }

        composable<Destination.Dashboard> {
            DashboardRoute(
                onNavigateToOnboarding = { vehicleId, isEdit ->
                    navController.navigate(Destination.RentingDetails(vehicleId, isEdit))
                },
                onNavigateToVehicles = {
                    navController.navigate(Destination.VehicleList)
                },
                onNavigateToDataManagement = {
                    navController.navigate(Destination.DataManagement)
                },
                onNavigateToLogin = {
                    navController.navigate(Destination.Login) {
                        popUpTo(Destination.Dashboard) { inclusive = true }
                    }
                },
                onNavigateToPremiumPaywall = {
                    navController.navigate(Destination.PremiumPaywall)
                },
                onNavigateToPreferences = {
                    navController.navigate(Destination.Preferences)
                },
                onNavigateToRecordDetail = { recordId ->
                    navController.navigate(Destination.RecordDetail(recordId))
                }
            )
        }

        composable<Destination.VehicleList> {
            VehicleListRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToVehicleDetails = { vehicleId ->
                    navController.navigate(Destination.RentingDetails(vehicleId, isEdit = false))
                },
                onNavigateToAddVehicle = {
                    navController.navigate(Destination.RentingDetails(null, isEdit = false))
                }
            )
        }

        composable<Destination.DataManagement> {
            DataManagementRoute(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Destination.Preferences> {
            PreferencesRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onShowPrivacyOptions = onShowPrivacyOptions
            )
        }

        composable<Destination.RentingDetails> { backStackEntry ->
            OnboardingRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCropper = { uri ->
                    navController.navigate(Destination.ImageCropper(uri))
                },
                backStackEntry = backStackEntry
            )
        }

        composable<Destination.RecordDetail> {
            RecordDetailRoute(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Destination.ImageCropper> { backStackEntry ->
            val destination: Destination.ImageCropper = backStackEntry.toRoute()
            CropImageScreen(
                uri = destination.uri,
                onCropSuccess = { croppedUri ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("cropped_uri", croppedUri)
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
    }
}
