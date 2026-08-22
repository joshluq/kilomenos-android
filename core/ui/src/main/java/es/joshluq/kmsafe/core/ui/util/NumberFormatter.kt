package es.joshluq.kmsafe.core.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utility for formatting numbers with high precision but clean output.
 */
object NumberFormatter {

    /**
     * Formats a distance (km) showing up to 2 decimals only if they are non-zero.
     * Example: 10.0 -> "10", 10.5 -> "10.5", 10.45 -> "10.45"
     */
    fun formatDistance(value: Double): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val df = DecimalFormat("#.##", symbols)
        return df.format(value)
    }

    /**
     * Formats a currency value with exactly 2 decimals and the Euro symbol.
     * Example: 12.5 -> "12.50 €"
     */
    fun formatCurrency(value: Double): String {
        return String.format(Locale.getDefault(), "%.2f €", value)
    }
    
    /**
     * Formats a rate value (e.g. km/day, km/month) showing up to 1 decimal only if non-zero.
     * Example: 42.0 -> "42", 42.5 -> "42.5"
     */
    fun formatRate(value: Double): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val df = DecimalFormat("#.#", symbols)
        return df.format(value)
    }

}
