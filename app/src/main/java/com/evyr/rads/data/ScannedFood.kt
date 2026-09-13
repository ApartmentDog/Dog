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
    val source: String,
    val confidence: String? = null
)

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
