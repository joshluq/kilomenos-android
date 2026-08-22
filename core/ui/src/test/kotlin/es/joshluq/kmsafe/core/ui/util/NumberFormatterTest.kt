package es.joshluq.kmsafe.core.ui.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Unit tests for [NumberFormatter] using JUnit4.
 * 
 * Note: We use [Locale.setDefault] to ensure deterministic formatting across environments.
 */
class NumberFormatterTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `formatDistance should remove decimals for whole numbers`() {
        val result = NumberFormatter.formatDistance(10.0)
        assertEquals("10", result)
    }

    @Test
    fun `formatDistance should keep one decimal if non-zero`() {
        val result = NumberFormatter.formatDistance(10.5)
        assertEquals("10.5", result)
    }

    @Test
    fun `formatDistance should keep two decimals if non-zero`() {
        val result = NumberFormatter.formatDistance(10.45)
        assertEquals("10.45", result)
    }

    @Test
    fun `formatDistance should round to two decimals`() {
        val result = NumberFormatter.formatDistance(10.456)
        assertEquals("10.46", result)
    }

    @Test
    fun `formatCurrency should always show two decimals and Euro symbol`() {
        assertEquals("12.50 €", NumberFormatter.formatCurrency(12.5))
        assertEquals("12.00 €", NumberFormatter.formatCurrency(12.0))
        assertEquals("12.45 €", NumberFormatter.formatCurrency(12.453))
        assertEquals("12.46 €", NumberFormatter.formatCurrency(12.456))
    }

    @Test
    fun `formatRate should remove decimals for whole numbers`() {
        val result = NumberFormatter.formatRate(42.0)
        assertEquals("42", result)
    }

    @Test
    fun `formatRate should keep one decimal if non-zero`() {
        val result = NumberFormatter.formatRate(42.5)
        assertEquals("42.5", result)
    }

    @Test
    fun `formatRate should round to one decimal`() {
        val result = NumberFormatter.formatRate(42.56)
        assertEquals("42.6", result)
    }
}
