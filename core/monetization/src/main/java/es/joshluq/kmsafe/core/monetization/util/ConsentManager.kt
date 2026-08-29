package es.joshluq.kmsafe.core.monetization.util

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.annotation.RequiresPermission
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.android.ump.UserMessagingPlatform.getConsentInformation
import com.google.android.ump.UserMessagingPlatform.loadAndShowConsentFormIfRequired
import es.joshluq.foundationkit.log.LoggerKit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to manage User Messaging Platform (UMP) consent flow.
 */
@Singleton
class ConsentManager @Inject constructor(
    private val logger: LoggerKit
) {
    private lateinit var consentInformation: ConsentInformation

    /**
     * Gathers consent from the user if required.
     *
     * @param activity The host activity.
     * @param onConsentGathered Callback when the flow completes.
     */
    fun gatherConsent(
        activity: Activity,
        onConsentGathered: (Boolean) -> Unit
    ) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        logger.e("ConsentManager", "Consent form error: ${formError.message}")
                    }
                    onConsentGathered(consentInformation.canRequestAds())
                }
            },
            { requestError ->
                logger.e("ConsentManager", "Consent info update error: ${requestError.message}")
                onConsentGathered(consentInformation.canRequestAds())
            }
        )
    }

    /**
     * Checks if ads can be requested based on the current consent state.
     */
    fun canRequestAds(): Boolean = ::consentInformation.isInitialized && consentInformation.canRequestAds()

    /**
     * Initializes the Mobile Ads SDK if consent is given.
     */
    @RequiresPermission(Manifest.permission.INTERNET)
    fun initializeAds(context: Context) {
        if (canRequestAds()) {
            MobileAds.initialize(context)
        }
    }

    /**
     * Checks if the privacy options form is required (GDPR requirement).
     */
    fun isPrivacyOptionsRequired(): Boolean =
        ::consentInformation.isInitialized &&
            consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Shows the privacy options form to allow the user to change their consent settings.
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentGathered: (Boolean) -> Unit
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                logger.e("ConsentManager", "Privacy options form error: ${formError.message}")
            }
            onConsentGathered(consentInformation.canRequestAds())
        }
    }
}
