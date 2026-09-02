package es.joshluq.kmsafe.core.ui.util

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.kmsafe.core.ui.R
import es.joshluq.kmsafe.domain.model.KmError

/**
 * Maps a domain [KmError] to a localized [TextProvider] for the UI.
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
        is KmError.ServerError -> TextProvider.Resource(R.string.error_server, this.code)
        KmError.UnknownError -> TextProvider.Resource(R.string.error_unknown)
    }
}
