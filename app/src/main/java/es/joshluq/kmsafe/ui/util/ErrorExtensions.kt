package es.joshluq.kmsafe.ui.util

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.kmsafe.R
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
        is KmError.ServerError -> TextProvider.Resource(R.string.error_server, this.code)
        KmError.UnknownError -> TextProvider.Resource(R.string.error_unknown)
    }
}
