package es.joshluq.kmsafe.ui.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Utility functions for date and time manipulations.
 */
object DateUtils {

    /**
     * Merges the date component of [selectedDateMillis] (usually from a DatePicker at 00:00 UTC)
     * with the current time (hours, minutes, seconds) of the system.
     *
     * @param selectedDateMillis The timestamp representing the start of the day in UTC.
     * @return A timestamp in milliseconds with the chosen date and current time.
     */
    fun mergeDateWithCurrentTime(selectedDateMillis: Long): Long {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = selectedDateMillis
        }
        val year = utcCalendar.get(Calendar.YEAR)
        val month = utcCalendar.get(Calendar.MONTH)
        val day = utcCalendar.get(Calendar.DAY_OF_MONTH)

        val resultCalendar = Calendar.getInstance()

        resultCalendar.set(Calendar.YEAR, year)
        resultCalendar.set(Calendar.MONTH, month)
        resultCalendar.set(Calendar.DAY_OF_MONTH, day)

        return resultCalendar.timeInMillis
    }
}
