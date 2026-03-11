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
        val currentCalendar = Calendar.getInstance()
        val hour = currentCalendar.get(Calendar.HOUR_OF_DAY)
        val minute = currentCalendar.get(Calendar.MINUTE)
        val second = currentCalendar.get(Calendar.SECOND)
        val millisecond = currentCalendar.get(Calendar.MILLISECOND)

        val resultCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = selectedDateMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, millisecond)
        }

        // Convert back to system default timezone for consistency in storage if needed,
        // but since our system uses Long timestamps, we just return the millis.
        return resultCalendar.timeInMillis
    }
}
