package es.joshluq.kmsafe.core.ui.util

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.kmsafe.core.ui.R
import es.joshluq.kmsafe.domain.model.FuelType

/**
 * Extension to convert FuelType to a localized TextProvider.
 */
fun FuelType.toTextProvider(): TextProvider {
    val resId = when (this) {
        FuelType.GASOLINE_95 -> R.string.fuel_type_gasoline_95
        FuelType.GASOLINE_98 -> R.string.fuel_type_gasoline_98
        FuelType.DIESEL -> R.string.fuel_type_diesel
        FuelType.DIESEL_PLUS -> R.string.fuel_type_diesel_plus
        FuelType.LPG -> R.string.fuel_type_lpg
        FuelType.CNG -> R.string.fuel_type_cng
        FuelType.ELECTRIC_KWH -> R.string.fuel_type_electric
        FuelType.HYBRID_PHEV -> R.string.fuel_type_hybrid
        FuelType.HYDROGEN -> R.string.fuel_type_hydrogen
        FuelType.BIODIESEL -> R.string.fuel_type_biodiesel
        FuelType.ETHANOL_E85 -> R.string.fuel_type_ethanol_e85
        FuelType.ADBLUE -> R.string.fuel_type_adblue
        FuelType.OTHER -> R.string.fuel_type_other
    }
    return TextProvider.Resource(resId)
}
