package com.anjo.statisticsui

import com.anjo.statisticsui.presentation.dashboard.unitDisplay
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnitsTest {
    private fun assertClose(expected: Double, actual: Double, tolerance: Double = 1e-9) =
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected, got $actual")

    /** metric value [m] in [metric] equals imperial value [i] in [imperial], both ways, and round-trips. */
    private fun check(metric: String, m: Double, imperial: String, i: Double, tolerance: Double = 1e-6) {
        val toImperial = unitDisplay(metric, "imperial")
        val toMetric = unitDisplay(imperial, "metric")
        assertEquals(imperial, toImperial.unit)
        assertEquals(metric, toMetric.unit)
        assertClose(i, toImperial.convert(m), tolerance)
        assertClose(m, toMetric.convert(i), tolerance)
        assertClose(m, toMetric.convert(toImperial.convert(m)))
    }

    @Test
    fun everyFamilyConvertsBothWays() {
        check("°C", 100.0, "°F", 212.0)
        check("°C", -40.0, "°F", -40.0)
        check("km", 1.609344, "mi", 1.0)
        check("kg", 0.45359237, "lb", 1.0)
        check("km/h", 1.609344, "mph", 1.0)
        check("l", 3.785411784, "gal", 1.0)
        check("hPa", 33.8638866667, "inHg", 1.0)
    }

    @Test
    fun unknownOrSameSystemUnitsPassThrough() {
        listOf("%" to "imperial", "ppm" to "metric", "°C" to "metric", "°F" to "imperial").forEach { (unit, system) ->
            val d = unitDisplay(unit, system)
            assertEquals(unit, d.unit)
            assertEquals(21.5, d.convert(21.5))
        }
        assertEquals(null, unitDisplay(null, "imperial").unit)
    }
}
