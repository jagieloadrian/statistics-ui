package com.anjo.statisticsui.model

private class Conversion(
    val from: String,
    val to: String,
    val factor: Double,
    val offset: Double = 0.0,
)

private val CONVERSIONS =
    listOf(
        Conversion("°C", "°F", 1.8, 32.0),
        Conversion("cm", "in", 1 / 2.54),
        Conversion("mm", "in", 1 / 25.4),
        Conversion("m", "ft", 1 / 0.3048),
        Conversion("km", "mi", 1 / 1.609344),
        Conversion("g", "oz", 1 / 28.349523125),
        Conversion("kg", "lb", 1 / 0.45359237),
        Conversion("km/h", "mph", 1 / 1.609344),
        Conversion("m/s", "mph", 3600 / 1609.344),
        Conversion("ml", "fl oz", 1 / 29.5735295625),
        Conversion("l", "gal", 1 / 3.785411784),
        Conversion("hPa", "inHg", 1 / 33.8638866667),
        Conversion("kPa", "psi", 1 / 6.894757293168),
    )

class UnitDisplay(
    val unit: String?,
    private val factor: Double = 1.0,
    private val offset: Double = 0.0,
) {
    fun convert(value: Double) = value * factor + offset
}

fun unitDisplay(
    unit: String?,
    system: String,
): UnitDisplay {
    if (system == "imperial") CONVERSIONS.firstOrNull { it.from == unit }?.let { return UnitDisplay(it.to, it.factor, it.offset) }
    if (system ==
        "metric"
    ) {
        CONVERSIONS.firstOrNull { it.to == unit }?.let { return UnitDisplay(it.from, 1 / it.factor, -it.offset / it.factor) }
    }
    return UnitDisplay(unit)
}
