package com.evyr.rads.data

import kotlin.math.roundToInt

/**
 * Storage is always metric (kg, cm). Display and entry convert at the edges,
 * so switching units never rewrites stored data.
 */
object Units {
    const val LB_PER_KG = 2.20462262
    const val CM_PER_IN = 2.54

    fun kgToLb(kg: Double): Double = kg * LB_PER_KG
    fun lbToKg(lb: Double): Double = lb / LB_PER_KG

    fun cmToIn(cm: Double): Double = cm / CM_PER_IN
    fun inToCm(inches: Double): Double = inches * CM_PER_IN

    /** cm -> (feet, inches) for display. */
    fun cmToFeetInches(cm: Double): Pair<Int, Int> {
        val totalIn = (cm / CM_PER_IN).roundToInt()
        return (totalIn / 12) to (totalIn % 12)
    }

    fun feetInchesToCm(feet: Int, inches: Int): Double =
        ((feet * 12) + inches) * CM_PER_IN

    fun weightLabel(imperial: Boolean) = if (imperial) "lb" else "kg"
    fun heightLabel(imperial: Boolean) = if (imperial) "ft/in" else "cm"

    fun displayWeight(kg: Double, imperial: Boolean): String {
        if (kg <= 0) return ""
        val v = if (imperial) kgToLb(kg) else kg
        return if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
    }

    fun parseWeightToKg(input: String, imperial: Boolean): Double? {
        val v = input.toDoubleOrNull() ?: return null
        return if (imperial) lbToKg(v) else v
    }
}
