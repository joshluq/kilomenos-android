package es.joshluq.kmsafe.core.monetization.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * A Composable that displays an AdMob banner ad.
 * Includes layout jitter prevention (minHeight) and lifecycle release cleanup.
 *
 * @param adUnitId The AdMob ad unit ID.
 * @param modifier Root layout modifier.
 */
@androidx.annotation.RequiresPermission(android.Manifest.permission.INTERNET)
@Composable
fun AdMobBanner(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { adView ->
            adView.destroy()
        }
    )
}
