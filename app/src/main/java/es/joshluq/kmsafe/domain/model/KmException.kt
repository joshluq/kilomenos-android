package es.joshluq.kmsafe.domain.model

/**
 * A custom exception that wraps a [KmError].
 * Facilitates the propagation of structured errors through the reactive streams.
 */
class KmException(val error: KmError) : Exception()
