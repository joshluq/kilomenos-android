package es.joshluq.kmsafe.data.util

import com.fasterxml.jackson.databind.ObjectMapper
import es.joshluq.foundationkit.provider.SerializerProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [SerializerProvider] using Jackson ObjectMapper.
 */
@Singleton
class JacksonSerializerProvider @Inject constructor() : SerializerProvider {
    private val mapper = ObjectMapper()

    override fun <T : Any> serialize(value: T, type: Class<T>): String {
        return mapper.writeValueAsString(value)
    }

    override fun <T : Any> deserialize(value: String, type: Class<T>): T {
        return mapper.readValue(value, type)
    }
}
