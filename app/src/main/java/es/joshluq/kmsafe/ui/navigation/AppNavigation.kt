package es.joshluq.kmsafe.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import es.joshluq.kmsafe.core.navigation.Destination
import es.joshluq.kmsafe.core.navigation.LocalNavigationResultStore
import es.joshluq.kmsafe.core.navigation.NavigationResultStore
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
import es.joshluq.kmsafe.feature.premium.paywall.PremiumPaywallRoute
import es.joshluq.kmsafe.feature.profile.preferences.PreferencesRoute
import es.joshluq.kmsafe.ui.common.cropper.CropImageScreen
import es.joshluq.kmsafe.ui.common.permissions.AssistedTrackingPermissionsScreen
import es.joshluq.kmsafe.ui.common.permissions.AutoTrackingPermissionsScreen

/**
 * Root navigation host of the application implemented with Navigation 3 [NavDisplay].
 */
@Composable
fun AppNavigation(
    initialDestination: Destination = Destination.Launch,
    resultStore: NavigationResultStore,
    onLaunchBilling: () -> Unit = {},
    onShowPrivacyOptions: () -> Unit = {}
) {
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    val backStack = remember { mutableStateListOf<Destination>(initialDestination) }

    val onNavigate: (Destination) -> Unit = { dest ->
        backStack.add(dest)
    }

    val onBack: () -> Unit = {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        onBack()
    }

    CompositionLocalProvider(LocalNavigationResultStore provides resultStore) {
        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(200)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeOut(animationSpec = tween(200)))
            },
            popTransitionSpec = {
                (slideInHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeIn(animationSpec = tween(200)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(animationSpec = tween(200)))
            },
            entryProvider = { key ->
                when (key) {
                    Destination.Launch -> NavEntry(key) {
                        LaunchRoute(
                            onNavigateToLogin = {
                                viewModelStoreOwner?.viewModelStore?.clear()
                                backStack.clear()
                                backStack.add(Destination.Login)
                            },
                            onNavigateToDashboard = {
                                backStack.clear()
                                backStack.add(Destination.Dashboard)
                            }
                        )
                    }

                    Destination.Login -> NavEntry(key) {
                        LoginRoute(
                            onNavigateToDashboard = {
                                backStack.clear()
                                backStack.add(Destination.Dashboard)
                            },
                            onNavigateToPremiumPaywall = {
                                backStack.clear()
                                backStack.add(Destination.Dashboard)
                                backStack.add(Destination.PremiumPaywall)
                            },
                            onNavigateToSignup = {
                                onNavigate(Destination.Signup)
                            }
                        )
                    }

                    Destination.Signup -> NavEntry(key) {
                        SignupRoute(
                            onNavigateBack = onBack,
                            onNavigateToDashboard = {
                                backStack.clear()
                                backStack.add(Destination.Dashboard)
                            },
                            onNavigateToWelcomeDiscovery = {
                                backStack.clear()
                                backStack.add(Destination.Dashboard)
                                backStack.add(Destination.WelcomeDiscovery(isGuideMode = false))
                            },
                            onNavigateToPremiumPaywall = {
                                backStack.clear()
                                backStack.add(Destination.Dashboard)
                                backStack.add(Destination.PremiumPaywall)
                            }
                        )
                    }

                    is Destination.WelcomeDiscovery -> NavEntry(key) {
                        WelcomeDiscoveryScreen(
                            isGuideMode = key.isGuideMode,
                            onNavigateToRentingSetup = {
                                val hasDashboard = backStack.contains(Destination.Dashboard)
                                if (!hasDashboard) {
                                    backStack.add(0, Destination.Dashboard)
                                }
                                onNavigate(Destination.SetupWizard)
                            },
                            onFinishGuide = {
                                if (backStack.size > 1) {
                                    onBack()
                                } else {
                                    backStack.clear()
                                    backStack.add(Destination.Dashboard)
                                }
                            },
                            onSkip = {
                                if (backStack.size > 1) {
                                    onBack()
                                } else {
                                    backStack.clear()
                                    backStack.add(Destination.Dashboard)
                                }
                            }
                        )
                    }

                    Destination.PremiumPaywall -> NavEntry(key) {
                        PremiumPaywallRoute(
                            onNavigateToDashboard = {
                                backStack.clear()
                                backStack.add(Destination.Dashboard)
                            },
                            onNavigateBack = onBack,
                            onLaunchBilling = onLaunchBilling
                        )
                    }

                    Destination.Dashboard -> NavEntry(key) {
                        DashboardRoute(
                            navigationContent = { selectedTab, onSelectTab ->
                                DashboardNavigation(
                                    selectedTab = selectedTab,
                                    onSelectTab = onSelectTab,
                                    onNavigateToOnboarding = { vehicleId, isEdit ->
                                        if (isEdit && vehicleId != null) {
                                            onNavigate(Destination.EditContract(vehicleId))
                                        } else {
                                            onNavigate(Destination.SetupWizard)
                                        }
                                    },
                                    onNavigateToVehicles = {
                                        onNavigate(Destination.VehicleList)
                                    },
                                    onNavigateToLogin = {
                                        viewModelStoreOwner?.viewModelStore?.clear()
                                        backStack.clear()
                                        backStack.add(Destination.Login)
                                    },
                                    onNavigateToPremiumPaywall = {
                                        onNavigate(Destination.PremiumPaywall)
                                    },
                                    onNavigateToPreferences = {
                                        onNavigate(Destination.Preferences)
                                    },
                                    onNavigateToRecordDetail = { recordId ->
                                        onNavigate(Destination.RecordDetail(recordId))
                                    },
                                    onNavigateToStations = {
                                        onNavigate(Destination.StationManagement)
                                    },
                                    onNavigateToStationDetail = { stationId ->
                                        onNavigate(Destination.StationDetail(stationId))
                                    },
                                    onNavigateToPermissions = {
                                        onNavigate(Destination.AutoTrackingPermissions)
                                    },
                                    onNavigateToAssistedPermissions = {
                                        onNavigate(Destination.AssistedTrackingPermissions)
                                    },
                                    onNavigateToWelcomeDiscovery = { isGuideMode ->
                                        onNavigate(Destination.WelcomeDiscovery(isGuideMode = isGuideMode))
                                    },
                                    onNavigateToVehicleDetail = { vehicleId ->
                                        onNavigate(Destination.VehicleDetail(vehicleId))
                                    },
                                    onNavigateToEditContract = { vehicleId ->
                                        onNavigate(Destination.EditContract(vehicleId))
                                    }
                                )
                            }
                        )
                    }

                    Destination.VehicleList -> NavEntry(key) {
                        VehicleListRoute(
                            onNavigateBack = onBack,
                            onNavigateToVehicleDetails = { vehicleId ->
                                onNavigate(Destination.VehicleDetail(vehicleId))
                            },
                            onNavigateToAddVehicle = {
                                onNavigate(Destination.SetupWizard)
                            },
                            onNavigateToPremiumPaywall = {
                                onNavigate(Destination.PremiumPaywall)
                            }
                        )
                    }

                    Destination.Preferences -> NavEntry(key) {
                        PreferencesRoute(
                            onNavigateBack = onBack,
                            onShowPrivacyOptions = onShowPrivacyOptions,
                            onNavigateToPermissions = {
                                onNavigate(Destination.AutoTrackingPermissions)
                            }
                        )
                    }

                    Destination.AutoTrackingPermissions -> NavEntry(key) {
                        AutoTrackingPermissionsScreen(
                            onAllPermissionsGranted = {
                                resultStore.setResult("permissions_granted", true)
                                onBack()
                            },
                            onDismiss = {
                                resultStore.setResult("permissions_granted", false)
                                onBack()
                            }
                        )
                    }

                    Destination.AssistedTrackingPermissions -> NavEntry(key) {
                        AssistedTrackingPermissionsScreen(
                            onAllPermissionsGranted = {
                                resultStore.setResult("permissions_granted", true)
                                onBack()
                            },
                            onDismiss = {
                                resultStore.setResult("permissions_granted", false)
                                onBack()
                            }
                        )
                    }

                    is Destination.EditContract -> NavEntry(key) {
                        EditContractRoute(
                            vehicleId = key.vehicleId,
                            onNavigateBack = onBack,
                            onNavigateToCropper = { uri ->
                                onNavigate(Destination.ImageCropper(uri))
                            }
                        )
                    }

                    Destination.SetupWizard -> NavEntry(key) {
                        SetupWizardRoute(
                            onNavigateBack = onBack,
                            onNavigateToDashboard = {
                                if (backStack.contains(Destination.VehicleList)) {
                                    val targetIndex = backStack.lastIndexOf(Destination.VehicleList)
                                    while (backStack.lastIndex > targetIndex) {
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                } else {
                                    backStack.clear()
                                    backStack.add(Destination.Dashboard)
                                }
                            },
                            onNavigateToCropper = { uri ->
                                onNavigate(Destination.ImageCropper(uri))
                            },
                            onNavigateToPremiumPaywall = {
                                onNavigate(Destination.PremiumPaywall)
                            }
                        )
                    }

                    is Destination.VehicleDetail -> NavEntry(key) {
                        VehicleDetailRoute(
                            vehicleId = key.vehicleId,
                            onNavigateBack = onBack,
                            onNavigateToEdit = { vehicleId ->
                                onNavigate(Destination.EditContract(vehicleId))
                            }
                        )
                    }

                    is Destination.RecordDetail -> NavEntry(key) {
                        RecordDetailRoute(
                            recordId = key.recordId,
                            onNavigateBack = onBack,
                            onNavigateToPremiumPaywall = {
                                onNavigate(Destination.PremiumPaywall)
                            }
                        )
                    }

                    is Destination.ImageCropper -> NavEntry(key) {
                        CropImageScreen(
                            uri = key.uri,
                            onCropSuccess = { croppedUri ->
                                resultStore.setResult("cropped_uri", croppedUri)
                                onBack()
                            },
                            onCancel = onBack
                        )
                    }

                    is Destination.Expenses -> NavEntry(key) {
                        ExpensesRoute(
                            stationId = key.stationId,
                            autoOpenAdd = key.autoOpenAdd,
                            priceReportMode = key.priceReportMode,
                            onNavigateToUpgrade = {
                                onNavigate(Destination.PremiumPaywall)
                            },
                            onNavigateToStations = {
                                onNavigate(Destination.StationManagement)
                            },
                            onNavigateToStationDetail = { stationId ->
                                onNavigate(Destination.StationDetail(stationId))
                            }
                        )
                    }

                    Destination.StationManagement -> NavEntry(key) {
                        StationManagementRoute(
                            onNavigateBack = onBack,
                            onNavigateToDetail = { stationId ->
                                onNavigate(Destination.StationDetail(stationId))
                            }
                        )
                    }

                    is Destination.StationDetail -> NavEntry(key) {
                        StationDetailRoute(
                            stationId = key.stationId,
                            onNavigateBack = onBack
                        )
                    }

                    Destination.Overview,
                    Destination.History,
                    Destination.Profile,
                    Destination.ProjectionAnalysis -> NavEntry(key) {
                        // Handled internally by DashboardNavigation
                    }
                }
            }
        )
    }
}
