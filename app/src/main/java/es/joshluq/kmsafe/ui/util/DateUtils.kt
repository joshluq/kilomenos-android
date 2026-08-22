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

    /**
     * Converts UTC 00:00 millis to Local 00:00 millis.
     * Useful for components that format dates using the local timezone.
     */
    /**
     * Normalizes a timestamp to the start of the day (00:00:00.000) in UTC.
     */
    fun normalizeToUtc00(millis: Long): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Calculates the end date of a contract based on its start date and duration in months.
     */
    fun getContractEndDate(startDate: Long, durationMonths: Int): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = startDate
            add(Calendar.MONTH, durationMonths)
        }.timeInMillis
    }
}
