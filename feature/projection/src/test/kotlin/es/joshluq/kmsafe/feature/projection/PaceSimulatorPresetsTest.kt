package es.joshluq.kmsafe.feature.projection

import es.joshluq.kmsafe.feature.projection.components.PacePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaceSimulatorPresetsTest {

    /**
     * Replicates the user's casuistry:
     * Contract with 55,000 km, started Sep 1, 1411.02 km surplus margin -> realDailyAverage is ~1.08 km/day.
     */
    @Test
    fun `given user casuistry with low daily pace when normal pace then only normal preset is selected`() {
        val realDailyAverage = 1.08f
        val simulatedDailyKm = 1.08f // Normal
        val paceMultiplier = 1.0f

        // Under old flawed calculation:
        val oldMinus20 = kotlin.math.abs(simulatedDailyKm - (realDailyAverage * 0.8f)) < 0.5f
        val oldMinus10 = kotlin.math.abs(simulatedDailyKm - (realDailyAverage * 0.9f)) < 0.5f
        val oldNormal = kotlin.math.abs(simulatedDailyKm - realDailyAverage) < 0.5f
        val oldPlus10 = kotlin.math.abs(simulatedDailyKm - (realDailyAverage * 1.1f)) < 0.5f
        val oldPlus20 = kotlin.math.abs(simulatedDailyKm - (realDailyAverage * 1.2f)) < 0.5f

        val oldSelectedCount = listOf(oldMinus20, oldMinus10, oldNormal, oldPlus10, oldPlus20).count { it }
        // Verify that the old logic indeed suffered from the bug (multiple presets selected!)
        assertTrue("Old logic should exhibit the defect by selecting 5 presets", oldSelectedCount > 1)

        // Under new robust PacePreset calculation:
        val newMinus20 = PacePreset.MINUS_20.isSelected(paceMultiplier)
        val newMinus10 = PacePreset.MINUS_10.isSelected(paceMultiplier)
        val newNormal = PacePreset.NORMAL.isSelected(paceMultiplier)
        val newPlus10 = PacePreset.PLUS_10.isSelected(paceMultiplier)
        val newPlus20 = PacePreset.PLUS_20.isSelected(paceMultiplier)

        assertFalse(newMinus20)
        assertFalse(newMinus10)
        assertTrue(newNormal)
        assertFalse(newPlus10)
        assertFalse(newPlus20)

        val newSelectedCount = PacePreset.entries.count { it.isSelected(paceMultiplier) }
        assertEquals("New logic must guarantee exactly 1 preset is selected", 1, newSelectedCount)
    }

    @Test
    fun `given each preset multiplier selected then exactly one preset is active`() {
        val multipliers = listOf(
            0.8f to PacePreset.MINUS_20,
            0.9f to PacePreset.MINUS_10,
            1.0f to PacePreset.NORMAL,
            1.1f to PacePreset.PLUS_10,
            1.2f to PacePreset.PLUS_20
        )

        multipliers.forEach { (multiplier, expectedPreset) ->
            val selectedCount = PacePreset.entries.count { it.isSelected(multiplier) }
            assertEquals("Expected exactly 1 preset selected for multiplier $multiplier", 1, selectedCount)
            assertTrue("Expected $expectedPreset to be selected for multiplier $multiplier", expectedPreset.isSelected(multiplier))
        }
    }

    @Test
    fun `given continuous range of multipliers then at most one preset is ever selected`() {
        var multiplier = 0.0f
        while (multiplier <= 2.5f) {
            val selectedCount = PacePreset.entries.count { it.isSelected(multiplier) }
            assertTrue(
                "Invariant violated: $selectedCount presets selected for multiplier $multiplier (must be <= 1)",
                selectedCount <= 1
            )
            multiplier += 0.005f
        }
    }
}
