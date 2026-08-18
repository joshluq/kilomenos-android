package es.joshluq.kmsafe.domain.model

/**
 * Domain-owned representation of the authentication session lifecycle.
 *
 * This sealed interface decouples the domain layer from any third-party session
 * management SDK (e.g., AuthKit), preserving the Dependency Inversion Principle
 * and the purity of the Bounded Context.
 *
 * Mapping from infrastructure types to this type is performed exclusively in the
 * infrastructure layer (see `AuthMapper.kt`).
 */
sealed interface AuthSessionState {

    /**
     * The session is fully active and tokens are valid.
     * Users and entitlements can be safely read.
     */
    data object Active : AuthSessionState

    /**
     * The session is active but the access token is nearing expiry.
     * A background refresh is in progress. The session is still usable.
     */
    data object ExpiringSoon : AuthSessionState

    /**
     * No session exists. The user is logged out.
     */
    data object Idle : AuthSessionState

    /**
     * The session system is performing initial bootstrapping.
     * No data should be read until this state transitions.
     */
    data object Initializing : AuthSessionState
}
