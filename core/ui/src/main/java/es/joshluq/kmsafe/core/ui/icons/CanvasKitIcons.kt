package es.joshluq.kmsafe.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Single Source of Truth for the KiloMenos / CanvasKit "Kinetic Mobility" icon system.
 *
 * Implements high-precision, 2dp-stroke programmatic vectors with zero runtime XML overhead,
 * adhering to the 4-Layer UI architecture, geometric symmetry, and Design System guidelines.
 */
object CanvasKitIcons {

    /**
     * Top-level navigation destinations (Dashboard BottomBar tabs).
     */
    object Navigation {

        private var _home: ImageVector? = null
        private var _auditLog: ImageVector? = null
        private var _riskSentinel: ImageVector? = null
        private var _bivalentPump: ImageVector? = null
        private var _smartPilot: ImageVector? = null

        /**
         * Tab 1: Overview - Modern Architectural Home / Inicio.
         * Instantly recognizable, clean 2dp stroke with arched portal.
         */
        val Home: ImageVector
            get() {
                if (_home != null) return _home!!
                _home = ImageVector.Builder(
                    name = "Navigation.Home",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f
                ).apply {
                    // Roof Overhang Gable
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(3f, 10.5f)
                        lineTo(12f, 3.5f)
                        lineTo(21f, 10.5f)
                    }
                    // House Body Walls & Base with subtle foundation curve
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(5.5f, 9.5f)
                        lineTo(5.5f, 19.5f)
                        curveTo(5.5f, 20.5f, 6.5f, 21f, 7.5f, 21f)
                        lineTo(16.5f, 21f)
                        curveTo(17.5f, 21f, 18.5f, 20.5f, 18.5f, 19.5f)
                        lineTo(18.5f, 9.5f)
                    }
                    // Arched Modern Entryway Door
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(9.5f, 21f)
                        lineTo(9.5f, 14.5f)
                        curveTo(9.5f, 13.2f, 10.6f, 12.5f, 12f, 12.5f)
                        curveTo(13.4f, 12.5f, 14.5f, 13.2f, 14.5f, 14.5f)
                        lineTo(14.5f, 21f)
                    }
                }.build()
                return _home!!
            }

        /**
         * Backward-compatible alias for Tab 1 (Overview).
         */
        val HorizonRunway: ImageVector
            get() = Home

        /**
         * Tab 2: History - Certified Audit Log & Route Ledger.
         */
        val AuditLog: ImageVector
            get() {
                if (_auditLog != null) return _auditLog!!
                _auditLog = ImageVector.Builder(
                    name = "Navigation.AuditLog",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f
                ).apply {
                    // Certified Ledger Card Outline with rounded corners
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(7f, 3.5f)
                        lineTo(17f, 3.5f)
                        curveTo(19f, 3.5f, 20f, 4.5f, 20f, 6.5f)
                        lineTo(20f, 18.5f)
                        curveTo(20f, 20.5f, 19f, 21.5f, 17f, 21.5f)
                        lineTo(7f, 21.5f)
                        curveTo(5f, 21.5f, 4f, 20.5f, 4f, 18.5f)
                        lineTo(4f, 6.5f)
                        curveTo(4f, 4.5f, 5f, 3.5f, 7f, 3.5f)
                        close()
                    }
                    // Verified Checklist Checkmark on Entry 1
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(7.5f, 8.5f)
                        lineTo(9f, 10f)
                        lineTo(11.5f, 7.5f)
                        // Entry 1 row text bar
                        moveTo(13.5f, 8.5f)
                        lineTo(16.5f, 8.5f)
                    }
                    // Entry 2 record row
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(7.5f, 13f)
                        lineTo(16.5f, 13f)
                    }
                    // Entry 3 record row
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(7.5f, 17.5f)
                        lineTo(13.5f, 17.5f)
                    }
                }.build()
                return _auditLog!!
            }

        /**
         * Tab 3: Projection - Minimalist Financial Forecast & Trend Arrow.
         * Simple, high-contrast, zero-clutter 2dp stroke.
         */
        val RiskSentinel: ImageVector
            get() {
                if (_riskSentinel != null) return _riskSentinel!!
                _riskSentinel = ImageVector.Builder(
                    name = "Navigation.RiskSentinel",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f
                ).apply {
                    // Minimalist Growth Trend Path (clean 3-segment upward trajectory)
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(3.5f, 17f)
                        lineTo(9.5f, 11f)
                        lineTo(14f, 15.5f)
                        lineTo(20.5f, 7f)
                    }
                    // Precision Forecast Arrow Head
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(15.5f, 7f)
                        lineTo(20.5f, 7f)
                        lineTo(20.5f, 12f)
                    }
                }.build()
                return _riskSentinel!!
            }

        /**
         * Tab 4: Expenses - Bivalent Smart Pump (Fuel + EV Dual Mode).
         */
        val BivalentPump: ImageVector
            get() {
                if (_bivalentPump != null) return _bivalentPump!!
                _bivalentPump = ImageVector.Builder(
                    name = "Navigation.BivalentPump",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f
                ).apply {
                    // Pump body with rounded top contour
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(4f, 20.5f)
                        lineTo(4f, 6.5f)
                        curveTo(4f, 4.5f, 5.5f, 3.5f, 7.5f, 3.5f)
                        lineTo(11.5f, 3.5f)
                        curveTo(13.5f, 3.5f, 15f, 4.5f, 15f, 6.5f)
                        lineTo(15f, 20.5f)
                    }
                    // Base Plate
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(2.5f, 20.5f)
                        lineTo(16.5f, 20.5f)
                    }
                    // Station meter display
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(7f, 7.5f)
                        lineTo(12f, 7.5f)
                    }
                    // Energy Bolt symbol inside pump
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 1.8f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(9.5f, 11.5f)
                        lineTo(8f, 14.5f)
                        lineTo(11f, 14.5f)
                        lineTo(9.5f, 17.5f)
                    }
                    // Ergonomic Hose & Nozzle
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 1.8f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(15f, 8.5f)
                        curveTo(17.5f, 8.5f, 19.5f, 9.5f, 19.5f, 12f)
                        lineTo(19.5f, 16.5f)
                        lineTo(18f, 15f)
                    }
                }.build()
                return _bivalentPump!!
            }

        /**
         * Tab 5: Profile - Smart Pilot & Driver Identity.
         */
        val SmartPilot: ImageVector
            get() {
                if (_smartPilot != null) return _smartPilot!!
                _smartPilot = ImageVector.Builder(
                    name = "Navigation.SmartPilot",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f
                ).apply {
                    // Pilot Head Circle
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(12f, 4f)
                        curveTo(9.5f, 4f, 7.5f, 6f, 7.5f, 8.5f)
                        curveTo(7.5f, 11f, 9.5f, 13f, 12f, 13f)
                        curveTo(14.5f, 13f, 16.5f, 11f, 16.5f, 8.5f)
                        curveTo(16.5f, 6f, 14.5f, 4f, 12f, 4f)
                        close()
                    }
                    // Pilot Visor Slit
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(10f, 8.5f)
                        lineTo(14f, 8.5f)
                    }
                    // Executive Shoulders / Torso Contour
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(4f, 20.5f)
                        curveTo(4f, 16.5f, 7.5f, 15f, 12f, 15f)
                        curveTo(16.5f, 15f, 20f, 16.5f, 20f, 20.5f)
                    }
                }.build()
                return _smartPilot!!
            }
    }

    /**
     * Energy Domain Icons (Fossil Fuel vs Electrification Dual Mode).
     */
    object Energy {

        private var _fuelDrop: ImageVector? = null
        private var _electricBolt: ImageVector? = null

        /**
         * Refined Fossil Fuel Drop (ICE Mode).
         */
        val FuelDrop: ImageVector
            get() {
                if (_fuelDrop != null) return _fuelDrop!!
                _fuelDrop = ImageVector.Builder(
                    name = "Energy.FuelDrop",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f
                ).apply {
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(12f, 3.5f)
                        curveTo(12f, 3.5f, 6f, 10.5f, 6f, 15f)
                        curveTo(6f, 18.5f, 8.7f, 21f, 12f, 21f)
                        curveTo(15.3f, 21f, 18f, 18.5f, 18f, 15f)
                        curveTo(18f, 10.5f, 12f, 3.5f, 12f, 3.5f)
                        close()
                    }
                    // Subtle light sheen highlight
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 1.8f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(14.5f, 13f)
                        curveTo(14.5f, 11f, 13.5f, 9.5f, 12f, 8f)
                    }
                }.build()
                return _fuelDrop!!
            }

        /**
         * Precision Electric Bolt (EV / PHEV Mode).
         */
        val ElectricBolt: ImageVector
            get() {
                if (_electricBolt != null) return _electricBolt!!
                _electricBolt = ImageVector.Builder(
                    name = "Energy.ElectricBolt",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f
                ).apply {
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(13f, 2.5f)
                        lineTo(6.5f, 12f)
                        lineTo(11.5f, 12f)
                        lineTo(10.5f, 21.5f)
                        lineTo(17.5f, 11.5f)
                        lineTo(12.5f, 11.5f)
                        close()
                    }
                }.build()
                return _electricBolt!!
            }
    }

    /**
     * Mobility & Telemetry Domain Icons.
     */
    object Mobility {

        private var _odometerLive: ImageVector? = null
        private var _bluetoothBeacon: ImageVector? = null

        val OdometerLive: ImageVector
            get() {
                if (_odometerLive != null) return _odometerLive!!
                _odometerLive = ImageVector.Builder(
                    name = "Mobility.OdometerLive",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f
                ).apply {
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(4.5f, 17.5f)
                        curveTo(3f, 10.5f, 7.5f, 4.5f, 12f, 4.5f)
                        curveTo(16.5f, 4.5f, 21f, 10.5f, 19.5f, 17.5f)
                        moveTo(12f, 14f)
                        lineTo(15.5f, 9.5f)
                        moveTo(8f, 19f)
                        lineTo(16f, 19f)
                    }
                }.build()
                return _odometerLive!!
            }

        val BluetoothBeacon: ImageVector
            get() {
                if (_bluetoothBeacon != null) return _bluetoothBeacon!!
                _bluetoothBeacon = ImageVector.Builder(
                    name = "Mobility.BluetoothBeacon",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f
                ).apply {
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    ) {
                        moveTo(7f, 7f)
                        lineTo(17f, 17f)
                        lineTo(12f, 21.5f)
                        lineTo(12f, 2.5f)
                        lineTo(17f, 7f)
                        lineTo(7f, 17f)
                    }
                }.build()
                return _bluetoothBeacon!!
            }
    }
}
