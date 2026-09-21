package com.evyr.rads.data

/** A food identified by barcode or vision, before it's committed to the log. */
data class ScannedFood(
    val name: String,
    val calories: Int,
    val fatGrams: Double,
    val proteinGrams: Double,
    val carbGrams: Double,
    val saturatedFatGrams: Double? = null,
    val servingNote: String? = null,
    val barcode: String? = null,
    /**
     * Set when the numbers are per-100g rather than per-serving, because the
     * source published no serving size. The portion step then asks for grams
     * instead of servings, so a 170 g biscuit isn't logged as 100 g.
     */
    val basisGrams: Double? = null,
    /** Weight/volume of one serving in g or ml, when the source publishes it. */
    val servingGrams: Double? = null,
    /** FDC id, so real portion options can be fetched when needed. */
    val sourceId: String? = null,
    val source: String,
    val confidence: String? = null
)

/** A real-world portion published for a food, e.g. "1 biscuit" at 170 g. */
data class FoodPortion(val label: String, val gramWeight: Double)

enum class Verdict { PASS, CAUTION, OVER_LIMIT }

data class VerdictResult(
    val verdict: Verdict,
    val headline: String,
    val reasons: List<String>
)

object VerdictRules {

    /**
     * Judged against the per-meal fat ceiling and what's already in this meal —
     * never against a daily fat budget.
     */
    fun evaluate(
        food: ScannedFood,
        fatAlreadyInMeal: Double,
        fatLimitPerMeal: Double
    ): VerdictResult {
        val reasons = mutableListOf<String>()
        val projected = fatAlreadyInMeal + food.fatGrams

        val verdict = when {
            projected >= fatLimitPerMeal -> Verdict.OVER_LIMIT
            projected >= fatLimitPerMeal * 0.7 -> Verdict.CAUTION
            else -> Verdict.PASS
        }

        reasons += "Meal fat would reach ${fmt(projected)}g of ${fmt(fatLimitPerMeal)}g."

        if (fatAlreadyInMeal > 0) {
            reasons += "${fmt(fatAlreadyInMeal)}g already logged this meal."
        }

        food.saturatedFatGrams?.let { sat ->
            if (sat >= food.fatGrams * 0.5 && sat >= 5.0) {
                reasons += "High share of saturated fat (${fmt(sat)}g)."
            }
        }

        if (food.calories >= 700) {
            reasons += "Large single item at ${food.calories} kcal."
        }

        food.servingNote?.let { reasons += it }

        if (food.confidence == "low") {
            reasons += "Vision estimate — numbers are approximate."
        }

        val headline = when (verdict) {
            Verdict.PASS -> "WITHIN MEAL LIMIT"
            Verdict.CAUTION -> "APPROACHING MEAL LIMIT"
            Verdict.OVER_LIMIT -> "EXCEEDS MEAL FAT LIMIT"
        }

        return VerdictResult(verdict, headline, reasons)
    }

    private fun fmt(v: Double): String =
        if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
}
