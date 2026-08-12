package es.joshluq.kmsafe.domain.repository

/**
 * Interface to handle media operations without platform dependencies.
 */
interface MediaRepository {
    /**
     * Retrieves the byte array from a given file path or URI string.
     * @param uriPath The string representation of the media location.
     * @return ByteArray or null if retrieval fails.
     */
    suspend fun getFileBytes(uriPath: String): ByteArray?
}
