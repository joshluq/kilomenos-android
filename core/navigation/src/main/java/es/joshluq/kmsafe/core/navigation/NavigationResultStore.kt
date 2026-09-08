package es.joshluq.kmsafe.core.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe, in-memory navigation result bus for decoupled screen communication in Navigation 3.
 * Adheres to the Coordinator Pattern by allowing screens to emit results and observers to receive them.
 */
@Singleton
class NavigationResultStore @Inject constructor() {

    private val results = ConcurrentHashMap<String, MutableStateFlow<Any?>>()

    /**
     * Sets a navigation result for a given key.
     *
     * @param key Unique identifier for the result payload.
     * @param result Value to be stored and emitted.
     */
    fun <T : Any> setResult(key: String, result: T) {
        val flow = results.computeIfAbsent(key) { MutableStateFlow(null) }
        flow.value = result
    }

    /**
     * Observes a navigation result for a given key as a reactive [StateFlow].
     *
     * @param key Unique identifier for the result payload.
     * @return [StateFlow] emitting the latest result or null.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getResult(key: String): StateFlow<T?> {
        val flow = results.computeIfAbsent(key) { MutableStateFlow(null) }
        return flow.asStateFlow() as StateFlow<T?>
    }

    /**
     * Clears a result once consumed by the coordinator Route.
     *
     * @param key Unique identifier for the result payload.
     */
    fun clearResult(key: String) {
        results[key]?.value = null
    }
}

/**
 * CompositionLocal providing access to the [NavigationResultStore] within the Compose hierarchy.
 */
val LocalNavigationResultStore = staticCompositionLocalOf<NavigationResultStore> {
    error("No NavigationResultStore provided in CompositionLocalProvider")
}
