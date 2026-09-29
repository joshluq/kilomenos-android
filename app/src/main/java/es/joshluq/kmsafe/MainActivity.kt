package es.joshluq.kmsafe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.core.analytics.AnalyticsTracker
import es.joshluq.kmsafe.core.monetization.util.ConsentManager
import es.joshluq.kmsafe.core.navigation.Destination
import es.joshluq.kmsafe.core.navigation.NavigationResultStore
import es.joshluq.kmsafe.infrastructure.remote.billing.BillingManager
import es.joshluq.kmsafe.ui.navigation.AppNavigation
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var consentManager: ConsentManager

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var resultStore: NavigationResultStore

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var logger: LoggerKit

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        runCatching { enableEdgeToEdge() }
            .onFailure { error ->
                logger.e("MainActivity", "Failed to enable edge-to-edge layout: ${error.message}", error)
            }

        mainViewModel.handleIntent(intent)

        setContent {
            CanvasKitTheme {
                AppNavigation(
                    initialDestination = Destination.Launch,
                    resultStore = resultStore,
                    analyticsTracker = analyticsTracker,
                    onLaunchBilling = { billingManager.launchBillingFlow(this) },
                    onShowPrivacyOptions = {
                        consentManager.showPrivacyOptionsForm(this) { canRequestAds ->
                            if (canRequestAds) {
                                consentManager.initializeAds(this)
                            }
                        }
                    }
                )
            }
        }

        consentManager.gatherConsent(this) { canRequestAds ->
            if (canRequestAds) {
                consentManager.initializeAds(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.onAppResumed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainViewModel.handleIntent(intent)
    }
}
