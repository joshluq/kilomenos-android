package es.joshluq.kmsafe.infrastructure.mapper

import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class FuelMapperTest {

    @Test
    fun `toRequest maps all fields correctly including consumption`() {
        val expense = FuelExpense(
            id = "uuid-123",
            vehicleId = "vehicle-456",
            stationId = "station-789",
            stationName = "Test Station",
            timestamp = 1672531200000L, // 2023-01-01T00:00:00Z
            fuelType = FuelType.GASOLINE_95,
            unitPrice = 1.5,
            volumeQuantity = 40.0,
            totalCost = 60.0,
            odometerAtExpense = 10000.0,
            isFullTank = true,
            kmSinceLastRefuel = 500.0,
            consumptionPer100km = 8.0,
            receiptImagePath = "user-1/receipt-123.jpg",
            syncStatus = SyncStatus.PENDING
        )

        val request = expense.toRequest()

        assertEquals("uuid-123", request.id)
        assertEquals("2023-01-01T00:00:00Z", request.timestamp)
        assertEquals(10000.0, request.odometerValue!!, 0.001)
        assertEquals(40.0, request.volumeQuantity, 0.001)
        assertEquals(1.5, request.pricePerUnit, 0.001)
        assertEquals(60.0, request.totalCost, 0.001)
        assertEquals(true, request.isFullTank)
        assertEquals("GASOLINE_95", request.fuelType)
        assertEquals("station-789", request.stationId)
        assertEquals("Test Station", request.stationName)
        assertEquals(500.0, request.kmSinceLastRefuel!!, 0.001)
        assertEquals(8.0, request.consumptionPer100km ?: 0.0, 0.001)
        assertEquals("user-1/receipt-123.jpg", request.receiptImagePath)
    }
}
