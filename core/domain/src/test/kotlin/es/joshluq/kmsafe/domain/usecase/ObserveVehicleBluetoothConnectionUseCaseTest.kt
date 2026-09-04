package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.BluetoothRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObserveVehicleBluetoothConnectionUseCaseTest {

    private val bluetoothRepository: BluetoothRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: ObserveVehicleBluetoothConnectionUseCase

    @Before
    fun setUp() {
        useCase = ObserveVehicleBluetoothConnectionUseCaseImpl(bluetoothRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given null or blank mac address when invoke then emits Success with isConnected false`() = runTest {
        val nullEmissions = useCase(ObserveVehicleBluetoothConnectionUseCase.Input(null)).toList()
        assertEquals(1, nullEmissions.size)
        val nullOutput = nullEmissions.first() as ObserveVehicleBluetoothConnectionUseCase.Output.Success
        assertFalse(nullOutput.isConnected)

        val blankEmissions = useCase(ObserveVehicleBluetoothConnectionUseCase.Input("   ")).toList()
        assertEquals(1, blankEmissions.size)
        val blankOutput = blankEmissions.first() as ObserveVehicleBluetoothConnectionUseCase.Output.Success
        assertFalse(blankOutput.isConnected)
    }

    @Test
    fun `given valid mac address when repository emits states then maps to Success outputs`() = runTest {
        val mac = "00:11:22:33:44:55"
        every { bluetoothRepository.observeDeviceConnection(mac) } returns flowOf(false, true, false)

        val emissions = useCase(ObserveVehicleBluetoothConnectionUseCase.Input(mac)).toList()

        assertEquals(3, emissions.size)
        assertFalse((emissions[0] as ObserveVehicleBluetoothConnectionUseCase.Output.Success).isConnected)
        assertTrue((emissions[1] as ObserveVehicleBluetoothConnectionUseCase.Output.Success).isConnected)
        assertFalse((emissions[2] as ObserveVehicleBluetoothConnectionUseCase.Output.Success).isConnected)
    }
}
