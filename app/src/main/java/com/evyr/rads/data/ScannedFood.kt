package com.evyr.rads.data

/** A food identified by barcode or vision, before it's committed to the log. */
data class ScannedFood(
    val name: String,
    val calories: Int,
    val fatGrams: Double,
    val proteinGrams: Double,
    val carbGrams: Double,
    val saturatedFatGrams: Double? = null,
    val sugarGrams: Double? = null,
    val fiberGrams: Double? = null,
    val sodiumMg: Double? = null,
    /** Ingredient text when the source publishes it — used for trigger detection. */
    val ingredients: String? = null,
    /** Trigger keys an AI estimate reported directly (see Trigger). */
    val tags: Set<String> = emptySet(),
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
