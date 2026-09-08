package es.joshluq.kmsafe.core.navigation

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationResultStoreTest {

    private val resultStore = NavigationResultStore()

    @Test
    fun `given result set when observed then emits value and can be cleared`() = runTest {
        resultStore.setResult("test_key", "result_value")
        val result = resultStore.getResult<String>("test_key").first()
        assertEquals("result_value", result)

        resultStore.clearResult("test_key")
        val cleared = resultStore.getResult<String>("test_key").first()
        assertNull(cleared)
    }

    @Test
    fun `given uninitialized key when observed then emits null`() = runTest {
        val result = resultStore.getResult<String>("non_existent_key").first()
        assertNull(result)
    }
}
