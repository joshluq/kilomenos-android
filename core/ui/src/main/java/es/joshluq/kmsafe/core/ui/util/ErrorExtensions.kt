package es.joshluq.kmsafe.core.ui.util

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.kmsafe.core.ui.R
import es.joshluq.kmsafe.domain.model.KmError

/**
 * Maps a domain [KmError] to a localized [TextProvider] for the UI based on KILOMENOS-18.
 */
fun KmError.toText(): TextProvider {
    return when (this) {
        KmError.InvalidCredentials -> TextProvider.Resource(R.string.error_invalid_credentials)
        KmError.UserAlreadyRegistered -> TextProvider.Resource(R.string.error_user_already_registered)
        KmError.WeakPassword -> TextProvider.Resource(R.string.error_weak_password)
        KmError.InvalidEmail -> TextProvider.Resource(R.string.error_invalid_email)
        KmError.NetworkError -> TextProvider.Resource(R.string.error_network)
        KmError.InvalidFuelExpenseValues -> TextProvider.Resource(R.string.error_invalid_fuel_values)
        KmError.FuelExpensesPremiumOnly -> TextProvider.Resource(R.string.error_premium_only_feature)
        KmError.TrialAlreadyUsed -> TextProvider.Resource(R.string.error_trial_already_used)
        KmError.ExpenseNotFound -> TextProvider.Resource(R.string.error_expense_not_found)
        KmError.Unauthenticated -> TextProvider.Resource(R.string.error_unauthenticated)
        KmError.InvalidVehicleName -> TextProvider.Resource(R.string.error_invalid_vehicle_name)
        KmError.InvalidContractMetrics -> TextProvider.Resource(R.string.error_invalid_metrics)
        KmError.InvalidReceiptImage -> TextProvider.Resource(R.string.error_invalid_receipt_image)
        KmError.ReceiptScanQuotaExceeded -> TextProvider.Resource(R.string.error_receipt_scan_quota_exceeded)
        is KmError.ReceiptScanRateLimitBurst -> TextProvider.Resource(R.string.error_receipt_scan_quota_exceeded)
        KmError.ReceiptScanRateLimitDaily -> TextProvider.Resource(R.string.error_receipt_scan_quota_exceeded)
        is KmError.ReceiptScanRateLimitInvalidDocs -> TextProvider.Resource(R.string.error_invalid_receipt_image)
        KmError.UnsupportedImageFormat -> TextProvider.Resource(R.string.error_invalid_receipt_image)
        KmError.UnauthorizedFileAccess -> TextProvider.Resource(R.string.error_network)
        KmError.ReceiptScanServerError -> TextProvider.Resource(R.string.error_unknown)
        KmError.MultiVehicleLimitReached -> TextProvider.Resource(R.string.error_multi_vehicle_limit)

        // Official Backend Error Catalog (KILOMENOS-18)
        KmError.PremiumRequired -> TextProvider.Resource(R.string.error_premium_only_feature)
        KmError.DeviceTrialLimit -> TextProvider.Resource(R.string.error_device_trial_limit)
        KmError.AlreadyPremium -> TextProvider.Resource(R.string.error_already_premium)
        KmError.PurchaseVerificationFailed -> TextProvider.Resource(R.string.error_purchase_verification_failed)

        KmError.AuthUnauthorized -> TextProvider.Resource(R.string.error_unauthenticated)
        KmError.AuthTokenExpired -> TextProvider.Resource(R.string.error_unauthenticated)
        KmError.ApiKeyInvalid -> TextProvider.Resource(R.string.error_network)

        KmError.InvalidJsonBody -> TextProvider.Resource(R.string.error_invalid_json_body)
        is KmError.ValidationRequiredField -> TextProvider.Resource(R.string.error_validation_required_field, this.field)
        is KmError.InvalidFormat -> TextProvider.Resource(R.string.error_invalid_format, this.field)

        KmError.NotificationNotFound -> TextProvider.Resource(R.string.error_notification_not_found)
        KmError.ResourceNotFound -> TextProvider.Resource(R.string.error_resource_not_found)
        KmError.Conflict -> TextProvider.Resource(R.string.error_conflict)

        is KmError.DatabaseError -> TextProvider.Resource(R.string.error_database)
        KmError.FcmDispatchFailed -> TextProvider.Resource(R.string.error_fcm_dispatch_failed)
        is KmError.ServiceUnavailable -> TextProvider.Resource(R.string.error_service_unavailable)

        is KmError.ServerError -> TextProvider.Resource(R.string.error_server, this.code)
        KmError.UnknownError -> TextProvider.Resource(R.string.error_unknown)
    }
}
