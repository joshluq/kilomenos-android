package es.joshluq.kmsafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.infrastructure.remote.billing.BillingManager
import es.joshluq.kmsafe.infrastructure.worker.SyncManager
import es.joshluq.kmsafe.ui.navigation.AppNavigation
import es.joshluq.kmsafe.ui.util.ConsentManager
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
    lateinit var logger: LoggerKit

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        consentManager.gatherConsent(this) { canRequestAds ->
            if (canRequestAds) {
                MobileAds.initialize(this)
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

        setContent {
            CanvasKitTheme {
                AppNavigation(
                    onLaunchBilling = { billingManager.launchBillingFlow(this) },
                    onShowPrivacyOptions = {
                        consentManager.gatherConsent(this) { canRequestAds ->
                            if (canRequestAds) {
                                MobileAds.initialize(this)
                            }
                        }
                    }
                )
            }
        }
    }
}
