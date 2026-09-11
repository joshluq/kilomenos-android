package es.joshluq.kmsafe.core.ui.icons

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class CanvasKitIconsTest {

    @Test
    fun `navigation icons are created with standard 24dp and 24f viewport`() {
        val navIcons = listOf(
            CanvasKitIcons.Navigation.HorizonRunway,
            CanvasKitIcons.Navigation.AuditLog,
            CanvasKitIcons.Navigation.RiskSentinel,
            CanvasKitIcons.Navigation.BivalentPump,
            CanvasKitIcons.Navigation.SmartPilot
        )

        navIcons.forEach { icon ->
            assertNotNull(icon)
            assertEquals(24.dp, icon.defaultWidth)
            assertEquals(24.dp, icon.defaultHeight)
            assertEquals(24f, icon.viewportWidth)
            assertEquals(24f, icon.viewportHeight)
        }
    }

    @Test
    fun `navigation icons are memoized singleton instances`() {
        val firstRunway = CanvasKitIcons.Navigation.HorizonRunway
        val secondRunway = CanvasKitIcons.Navigation.HorizonRunway
        assertSame(firstRunway, secondRunway)

        val firstPump = CanvasKitIcons.Navigation.BivalentPump
        val secondPump = CanvasKitIcons.Navigation.BivalentPump
        assertSame(firstPump, secondPump)
    }

    @Test
    fun `energy dual mode icons are created with identical bounds`() {
        val fuel = CanvasKitIcons.Energy.FuelDrop
        val electric = CanvasKitIcons.Energy.ElectricBolt

        assertNotNull(fuel)
        assertNotNull(electric)
        assertEquals(fuel.defaultWidth, electric.defaultWidth)
        assertEquals(fuel.defaultHeight, electric.defaultHeight)
        assertEquals(fuel.viewportWidth, electric.viewportWidth)
        assertEquals(fuel.viewportHeight, electric.viewportHeight)
    }

    @Test
    fun `mobility icons are initialized successfully`() {
        val odometer = CanvasKitIcons.Mobility.OdometerLive
        val bluetooth = CanvasKitIcons.Mobility.BluetoothBeacon

        assertNotNull(odometer)
        assertNotNull(bluetooth)
        assertEquals(24.dp, odometer.defaultWidth)
        assertEquals(24.dp, bluetooth.defaultWidth)
    }
}
