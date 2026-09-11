package es.joshluq.kmsafe.core.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DestinationSerializationTest {

    private val testSaverScope = SaverScope { true }

    @Test
    fun `given backstack with destinations when saved and restored then retains all destinations in order`() {
        val originalList = mutableStateListOf<Destination>(
            Destination.Launch,
            Destination.Dashboard,
            Destination.Overview,
            Destination.VehicleDetail(vehicleId = "veh_123"),
            Destination.Expenses(stationId = "station_abc", autoOpenAdd = true, priceReportMode = false),
            Destination.WelcomeDiscovery(isGuideMode = true),
            Destination.RecordDetail(recordId = "rec_999"),
            Destination.Preferences
        )

        val saved = with(DestinationListSaver) { testSaverScope.save(originalList) }
        assertNotNull(saved)

        val restored = DestinationListSaver.restore(saved!!)
        assertNotNull(restored)
        assertEquals(originalList.size, restored!!.size)
        for (i in originalList.indices) {
            assertEquals(originalList[i], restored[i])
        }
    }
}
