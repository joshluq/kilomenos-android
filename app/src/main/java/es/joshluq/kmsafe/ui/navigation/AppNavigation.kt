package es.joshluq.kmsafe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import es.joshluq.kmsafe.core.navigation.DeepLinkConfig
import es.joshluq.kmsafe.core.navigation.Destination
import es.joshluq.kmsafe.feature.auth.launch.LaunchRoute
import es.joshluq.kmsafe.feature.auth.login.LoginRoute
import es.joshluq.kmsafe.feature.auth.signup.SignupRoute
import es.joshluq.kmsafe.feature.dashboard.DashboardRoute
import es.joshluq.kmsafe.feature.expenses.ExpensesRoute
import es.joshluq.kmsafe.feature.expenses.stations.StationManagementRoute
import es.joshluq.kmsafe.feature.expenses.stations.detail.StationDetailRoute
import es.joshluq.kmsafe.feature.fleet.detail.VehicleDetailRoute
import es.joshluq.kmsafe.feature.fleet.edit.EditContractRoute
import es.joshluq.kmsafe.feature.fleet.setup.SetupWizardRoute
import es.joshluq.kmsafe.feature.fleet.setup.WelcomeDiscoveryScreen
import es.joshluq.kmsafe.feature.fleet.vehicles.VehicleListRoute
import es.joshluq.kmsafe.feature.history.detail.RecordDetailRoute
import es.joshluq.kmsafe.feature.profile.preferences.PreferencesRoute
import es.joshluq.kmsafe.ui.common.cropper.CropImageScreen
import es.joshluq.kmsafe.ui.common.permissions.AutoTrackingPermissionsScreen
import es.joshluq.kmsafe.ui.datamanagement.DataManagementRoute
import es.joshluq.kmsafe.ui.premium.PremiumPaywallRoute

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
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Login) { inclusive = true }
                    }
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
                onNavigateToWelcomeDiscovery = {
                    navController.navigate(Destination.WelcomeDiscovery) {
                        popUpTo(Destination.Login) { inclusive = true }
                    }
                },
                onNavigateToPremiumPaywall = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Login) { inclusive = true }
                    }
                    navController.navigate(Destination.PremiumPaywall)
                }
            )
        }

        composable<Destination.WelcomeDiscovery> {
            WelcomeDiscoveryScreen(
                onNavigateToRentingSetup = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.WelcomeDiscovery) { inclusive = true }
                    }
                    navController.navigate(Destination.SetupWizard)
                },
                onSkip = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Destination.Dashboard) {
                            popUpTo(Destination.WelcomeDiscovery) { inclusive = true }
                        }
                    }
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

        composable<Destination.Dashboard> { backStackEntry ->
            DashboardRoute(
                navigationContent = { dashboardNavController ->
                    DashboardNavigation(
                        navController = dashboardNavController,
                        onNavigateToOnboarding = { vehicleId, isEdit ->
                            if (isEdit && vehicleId != null) {
                                navController.navigate(Destination.EditContract(vehicleId))
                            } else {
                                navController.navigate(Destination.SetupWizard)
                            }
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
                        },
                        onNavigateToStations = {
                            navController.navigate(Destination.StationManagement)
                        },
                        onNavigateToStationDetail = { stationId ->
                            navController.navigate(Destination.StationDetail(stationId))
                        },
                        onNavigateToPermissions = {
                            navController.navigate(Destination.AutoTrackingPermissions)
                        },
                        onNavigateToWelcomeDiscovery = {
                            navController.navigate(Destination.WelcomeDiscovery)
                        },
                        onNavigateToVehicleDetail = { vehicleId ->
                            navController.navigate(Destination.VehicleDetail(vehicleId))
                        },
                        backStackEntry = backStackEntry
                    )
                }
            )
        }

        composable<Destination.VehicleList> {
            VehicleListRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToVehicleDetails = { vehicleId ->
                    navController.navigate(Destination.VehicleDetail(vehicleId))
                },
                onNavigateToAddVehicle = {
                    navController.navigate(Destination.SetupWizard)
                },
                onNavigateToPremiumPaywall = {
                    navController.navigate(Destination.PremiumPaywall)
                }
            )
        }

        composable<Destination.DataManagement> {
            DataManagementRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPremiumPaywall = {
                    navController.navigate(Destination.PremiumPaywall)
                }
            )
        }

        composable<Destination.Preferences> { backStackEntry ->
            PreferencesRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onShowPrivacyOptions = onShowPrivacyOptions,
                onNavigateToPermissions = {
                    navController.navigate(Destination.AutoTrackingPermissions)
                },
                backStackEntry = backStackEntry
            )
        }

        composable<Destination.AutoTrackingPermissions> {
            AutoTrackingPermissionsScreen(
                onAllPermissionsGranted = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("permissions_granted", true)
                    navController.popBackStack()
                },
                onDismiss = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("permissions_granted", false)
                    navController.popBackStack()
                }
            )
        }

        composable<Destination.EditContract> { backStackEntry ->
            EditContractRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCropper = { uri ->
                    navController.navigate(Destination.ImageCropper(uri))
                },
                backStackEntry = backStackEntry
            )
        }

        composable<Destination.SetupWizard> { backStackEntry ->
            SetupWizardRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDashboard = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Dashboard) { inclusive = true }
                    }
                },
                onNavigateToCropper = { uri ->
                    navController.navigate(Destination.ImageCropper(uri))
                },
                backStackEntry = backStackEntry
            )
        }

        composable<Destination.VehicleDetail> {
            VehicleDetailRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { vehicleId ->
                    navController.navigate(Destination.EditContract(vehicleId))
                }
            )
        }

        composable<Destination.RecordDetail> {
            RecordDetailRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPremiumPaywall = {
                    navController.navigate(Destination.PremiumPaywall)
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

        composable<Destination.Expenses>(
            deepLinks = listOf(
                navDeepLink<Destination.Expenses>(basePath = "${DeepLinkConfig.BASE_URL}/expenses")
            )
        ) {
            ExpensesRoute(
                onNavigateToUpgrade = {
                    navController.navigate(Destination.PremiumPaywall)
                },
                onNavigateToStations = {
                    navController.navigate(Destination.StationManagement)
                },
                onNavigateToStationDetail = { stationId ->
                    navController.navigate(Destination.StationDetail(stationId))
                }
            )
        }

        composable<Destination.StationManagement> {
            StationManagementRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { stationId ->
                    navController.navigate(Destination.StationDetail(stationId))
                }
            )
        }

        composable<Destination.StationDetail> {
            StationDetailRoute(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
