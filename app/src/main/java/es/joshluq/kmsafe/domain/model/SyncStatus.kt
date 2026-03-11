package es.joshluq.kmsafe.domain.model

/**
 * Represents the synchronization status of a local entity with the remote server.
 */
enum class SyncStatus {
    /** Entity is only local and needs to be uploaded. */
    PENDING,
    /** Entity has been successfully synchronized with the server. */
    SYNCED,
    /** An error occurred during synchronization. */
    FAILED
}
