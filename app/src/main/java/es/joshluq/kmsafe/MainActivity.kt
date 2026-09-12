package es.joshluq.kmsafe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.core.monetization.util.ConsentManager
import es.joshluq.kmsafe.core.navigation.Destination
import es.joshluq.kmsafe.core.navigation.NavigationResultStore
import es.joshluq.kmsafe.infrastructure.remote.billing.BillingManager
import es.joshluq.kmsafe.infrastructure.worker.SyncManager
import es.joshluq.kmsafe.ui.navigation.AppNavigation
import es.joshluq.kmsafe.ui.navigation.DeepLinkParser
import es.joshluq.kmsafe.ui.util.NetworkConnectivityObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var consentManager: ConsentManager

    @Inject
    lateinit var connectivityObserver: NetworkConnectivityObserver

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var resultStore: NavigationResultStore

    @Inject
    lateinit var analyticsTracker: es.joshluq.kmsafe.core.analytics.AnalyticsTracker

    @Inject
    lateinit var logger: LoggerKit

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        consentManager.gatherConsent(this) { canRequestAds ->
            if (canRequestAds) {
                consentManager.initializeAds(this)
            }
        }

        connectivityObserver.observe()
            .onEach { status ->
                logger.d("MainActivity", "Network status changed: $status")
                if (status == NetworkConnectivityObserver.Status.Available) {
                    // Small delay to ensure the data connection is stable
                    delay(1000.milliseconds)
                    logger.d("MainActivity", "Triggering background sync")
                    syncManager.scheduleSync()
                }
            }
            .launchIn(lifecycleScope)

        enableEdgeToEdge()

        val initialDestination = DeepLinkParser.parse(intent) ?: Destination.Launch
        val isQuickAdd = intent?.data?.getQueryParameter("action") == "quick_add"
        if (isQuickAdd) {
            resultStore.setResult("quick_add_odometer", true)
        }

        setContent {
            CanvasKitTheme {
                AppNavigation(
                    initialDestination = initialDestination,
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val destination = DeepLinkParser.parse(intent)
        if (destination != null) {
            resultStore.setResult("deep_link_destination", destination)
        }
        val isQuickAdd = intent.data?.getQueryParameter("action") == "quick_add"
        if (isQuickAdd) {
            resultStore.setResult("quick_add_odometer", true)
        }
    }
}
