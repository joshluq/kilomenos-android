package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.BluetoothRepository
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateBluetoothConnectionStateUseCaseTest {

    private val bluetoothRepository: BluetoothRepository = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: UpdateBluetoothConnectionStateUseCase

    @Before
    fun setUp() {
        useCase = UpdateBluetoothConnectionStateUseCaseImpl(bluetoothRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given input when invoked then delegates to bluetooth repository`() = runTest {
        val mac = "AA:BB:CC:DD:EE:FF"
        val result = useCase(UpdateBluetoothConnectionStateUseCase.Input(mac, isConnected = true))

        assertTrue(result.isSuccess)
        assertEquals(UpdateBluetoothConnectionStateUseCase.Output.Success, result.getOrNull())
        verify(exactly = 1) { bluetoothRepository.updateDeviceConnectionState(mac, true) }
    }
}
