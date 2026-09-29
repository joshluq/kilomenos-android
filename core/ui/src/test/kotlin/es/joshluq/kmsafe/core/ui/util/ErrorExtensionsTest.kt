package es.joshluq.kmsafe.core.ui.util

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.kmsafe.core.ui.R
import es.joshluq.kmsafe.domain.model.KmError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorExtensionsTest {

    @Test
    fun `given Entitlements catalog errors when toText then maps to corresponding string resources`() {
        val premiumRequiredText = KmError.PremiumRequired.toText()
        assertTrue(premiumRequiredText is TextProvider.Resource)
        assertEquals(R.string.error_premium_only_feature, (premiumRequiredText as TextProvider.Resource).resId)

        val deviceTrialLimitText = KmError.DeviceTrialLimit.toText()
        assertTrue(deviceTrialLimitText is TextProvider.Resource)
        assertEquals(R.string.error_device_trial_limit, (deviceTrialLimitText as TextProvider.Resource).resId)

        val alreadyPremiumText = KmError.AlreadyPremium.toText()
        assertTrue(alreadyPremiumText is TextProvider.Resource)
        assertEquals(R.string.error_already_premium, (alreadyPremiumText as TextProvider.Resource).resId)

        val purchaseVerificationFailedText = KmError.PurchaseVerificationFailed.toText()
        assertTrue(purchaseVerificationFailedText is TextProvider.Resource)
        assertEquals(R.string.error_purchase_verification_failed, (purchaseVerificationFailedText as TextProvider.Resource).resId)
    }

    @Test
    fun `given Validation catalog errors when toText then maps with field arguments`() {
        val requiredField = KmError.ValidationRequiredField("fcm_token").toText()
        assertTrue(requiredField is TextProvider.Resource)
        assertEquals(R.string.error_validation_required_field, (requiredField as TextProvider.Resource).resId)
        assertEquals(listOf("fcm_token"), requiredField.args.toList())

        val invalidFormat = KmError.InvalidFormat("email").toText()
        assertTrue(invalidFormat is TextProvider.Resource)
        assertEquals(R.string.error_invalid_format, (invalidFormat as TextProvider.Resource).resId)
        assertEquals(listOf("email"), invalidFormat.args.toList())
    }

    @Test
    fun `given System and External catalog errors when toText then maps to corresponding string resources`() {
        val dbError = KmError.DatabaseError(500).toText()
        assertTrue(dbError is TextProvider.Resource)
        assertEquals(R.string.error_database, (dbError as TextProvider.Resource).resId)

        val fcmError = KmError.FcmDispatchFailed.toText()
        assertTrue(fcmError is TextProvider.Resource)
        assertEquals(R.string.error_fcm_dispatch_failed, (fcmError as TextProvider.Resource).resId)

        val serviceUnavailable = KmError.ServiceUnavailable(60).toText()
        assertTrue(serviceUnavailable is TextProvider.Resource)
        assertEquals(R.string.error_service_unavailable, (serviceUnavailable as TextProvider.Resource).resId)

        val conflict = KmError.Conflict.toText()
        assertTrue(conflict is TextProvider.Resource)
        assertEquals(R.string.error_conflict, (conflict as TextProvider.Resource).resId)

        val notFound = KmError.ResourceNotFound.toText()
        assertTrue(notFound is TextProvider.Resource)
        assertEquals(R.string.error_resource_not_found, (notFound as TextProvider.Resource).resId)
    }
}
