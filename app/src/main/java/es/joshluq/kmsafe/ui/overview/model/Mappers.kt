package es.joshluq.kmsafe.ui.overview.model

import es.joshluq.kmsafe.domain.model.MonthlyOdometerAggregation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Maps a pure Domain Value Object [MonthlyOdometerAggregation] to a UI-ready [MonthlyUsageUiModel].
 */
fun MonthlyOdometerAggregation.toUiModel(): MonthlyUsageUiModel {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
    }

    // Example formatting "Ene", "Feb", etc.
    val formatter = SimpleDateFormat("MMM", Locale.getDefault())
    val monthName = formatter.format(calendar.time).replaceFirstChar { it.uppercase() }

    val limitState = if (totalKms > budgetedKms) {
        MonthlyUsageUiModel.LimitState.OVER_LIMIT
    } else {
        MonthlyUsageUiModel.LimitState.SAFE
    }

    return MonthlyUsageUiModel(
        monthName = monthName,
        kmsText = totalKms.toString(),
        budgetedKmsText = budgetedKms.toString(),
        barPercentage = totalKms.toFloat(), // We pass the raw value to let the UI normalize it against the global max
        budgetPercentage = budgetedKms.toFloat(),
        limitState = limitState
    )
}
