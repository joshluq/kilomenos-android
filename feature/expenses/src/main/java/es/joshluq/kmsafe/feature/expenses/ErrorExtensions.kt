package es.joshluq.kmsafe.feature.expenses

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.kmsafe.domain.model.KmError

/**
 * Maps a domain [KmError] to a localized [TextProvider] for the Expenses feature.
 * Uses string resources defined within this feature module.
 */
fun KmError.toText(): TextProvider {
    return when (this) {
        KmError.InvalidFuelExpenseValues -> TextProvider.Resource(R.string.error_invalid_fuel_values)
        KmError.FuelExpensesPremiumOnly -> TextProvider.Resource(R.string.error_premium_only_feature)
        is KmError.ReceiptScanRateLimitBurst -> TextProvider.Resource(R.string.expenses_error_rate_limit_burst, this.retryAfterSeconds)
        KmError.ReceiptScanRateLimitDaily -> TextProvider.Resource(R.string.expenses_error_rate_limit_daily)
        is KmError.ReceiptScanRateLimitInvalidDocs -> {
            val minutes = (this.retryAfterSeconds + 59) / 60
            TextProvider.Resource(R.string.expenses_error_rate_limit_invalid_docs, minutes)
        }
        KmError.InvalidReceiptImage -> TextProvider.Resource(R.string.expenses_error_not_fuel_receipt)
        KmError.UnsupportedImageFormat -> TextProvider.Resource(R.string.expenses_error_unsupported_media)
        KmError.UnauthorizedFileAccess -> TextProvider.Resource(R.string.expenses_error_unauthorized_access)
        KmError.ReceiptScanServerError -> TextProvider.Resource(R.string.expenses_error_ocr_server_down)
        KmError.ExpenseNotFound -> TextProvider.Resource(R.string.error_expense_not_found)
        KmError.NetworkError -> TextProvider.Resource(R.string.error_network)
        KmError.UnknownError -> TextProvider.Resource(R.string.error_unknown)
        is KmError.ServerError -> TextProvider.Resource(R.string.error_network) // Fallback for generic server errors
        else -> TextProvider.Resource(R.string.error_unknown)
    }
}
