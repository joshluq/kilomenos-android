package es.joshluq.kmsafe.ui.navigation

import android.content.Intent
import android.net.Uri
import es.joshluq.kmsafe.core.navigation.DeepLinkConfig
import es.joshluq.kmsafe.core.navigation.Destination

/**
 * Parses incoming intents to extract valid Navigation 3 [Destination] instances.
 */
object DeepLinkParser {

    /**
     * Parses incoming [Intent] and resolves the corresponding typed [Destination].
     *
     * @param intent Incoming Android intent to parse.
     * @return Resolved [Destination] or null if the intent does not match known URI patterns.
     */
    fun parse(intent: Intent?): Destination? {
        val data: Uri = intent?.data ?: return null

        val isHttps = data.scheme == DeepLinkConfig.SCHEME && data.host == DeepLinkConfig.HOST
        val isCustomScheme = data.scheme == "kmsafe" && data.host == "app"
        if (!isHttps && !isCustomScheme) {
            return null
        }

        val pathSegments = data.pathSegments ?: return null

        return when {
            pathSegments.isEmpty() || pathSegments[0] == "dashboard" || pathSegments[0] == "overview" -> Destination.Dashboard
            pathSegments[0] == "expenses" -> {
                val stationId = data.getQueryParameter("stationId")
                val autoOpen = data.getBooleanQueryParameter("autoOpen", false)
                val priceReportMode = data.getBooleanQueryParameter("priceReportMode", false)
                Destination.Expenses(
                    stationId = stationId,
                    autoOpenAdd = autoOpen,
                    priceReportMode = priceReportMode
                )
            }
            pathSegments[0] == "vehicle" && pathSegments.size > 1 -> {
                Destination.VehicleDetail(vehicleId = pathSegments[1])
            }
            pathSegments[0] == "record" && pathSegments.size > 1 -> {
                Destination.RecordDetail(recordId = pathSegments[1])
            }
            pathSegments[0] == "station" && pathSegments.size > 1 -> {
                Destination.StationDetail(stationId = pathSegments[1])
            }
            pathSegments[0] == "premium" -> Destination.PremiumPaywall
            pathSegments[0] == "preferences" -> Destination.Preferences
            else -> null
        }
    }
}
