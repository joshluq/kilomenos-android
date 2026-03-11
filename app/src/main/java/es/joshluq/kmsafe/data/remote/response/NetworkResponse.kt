package es.joshluq.kmsafe.data.remote.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Base class for all API responses.
 * Contains common fields like success, message, and error.
 */
abstract class NetworkResponse {
    @JsonProperty("success")
    val success: Boolean = false

    @JsonProperty("message")
    val message: String? = null

    @JsonProperty("error")
    val error: String? = null
}
