package es.joshluq.kmsafe.domain.model

/**
 * Supported fuel and energy types for expenses and stations.
 *
 * @property unitOfMeasure Unit of measurement ('L' for liquids/gas, 'kWh' for electricity, 'kg' for hydrogen/CNG).
 * @property category Higher-level energy category (Combustion, Electric, or Gas).
 */
enum class FuelType(
    val unitOfMeasure: String,
    val category: EnergyCategory
) {
    GASOLINE_95("L", EnergyCategory.COMBUSTION),
    GASOLINE_98("L", EnergyCategory.COMBUSTION),
    DIESEL("L", EnergyCategory.COMBUSTION),
    DIESEL_PLUS("L", EnergyCategory.COMBUSTION),
    LPG("L", EnergyCategory.COMBUSTION),
    CNG("kg", EnergyCategory.COMBUSTION),
    ELECTRIC_KWH("kWh", EnergyCategory.ELECTRIC),
    HYBRID_PHEV("L/kWh", EnergyCategory.HYBRID),
    HYDROGEN("kg", EnergyCategory.COMBUSTION),
    BIODIESEL("L", EnergyCategory.COMBUSTION),
    ETHANOL_E85("L", EnergyCategory.COMBUSTION),
    ADBLUE("L", EnergyCategory.COMBUSTION),
    OTHER("", EnergyCategory.COMBUSTION);

    companion object {
        fun fromName(name: String?): FuelType =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: GASOLINE_95
    }
}
