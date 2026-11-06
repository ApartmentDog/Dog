package com.evyr.rads.data.local

/**
 * What the user told us they want help with during onboarding. This decides
 * which modules the home screen shows. Several can be on at once.
 */
enum class Focus(val key: String, val title: String, val blurb: String) {
    CONDITION(
        "condition", "Eating safely with a health condition",
        "Heartburn, gallbladder, IBS, diabetes, blood pressure and more"
    ),
    ALLERGY(
        "allergy", "Avoiding allergens",
        "Get a warning before you log something you react to"
    ),
    WEIGHT(
        "weight", "Losing or gaining weight",
        "A calorie goal and daily progress"
    ),
    MUSCLE(
        "muscle", "Building muscle or performance",
        "Protein-forward targets and macro tracking"
    );

    companion object {
        fun parse(csv: String?): Set<Focus> =
            csv.orEmpty().split(',').mapNotNull { k -> values().firstOrNull { it.key == k.trim() } }.toSet()

        fun toCsv(set: Set<Focus>): String =
            values().filter { it in set }.joinToString(",") { it.key }
    }
}

/** How much the app shows on each food: a plain verdict, or the numbers too. */
enum class DetailLevel(val key: String) {
    SIMPLE("simple"),
    DETAILED("detailed");

    companion object {
        fun fromKey(k: String?): DetailLevel =
            values().firstOrNull { it.key == k } ?: DETAILED
    }
}

/**
 * The home (Log) screen, worked out from the profile. Profiles that predate
 * onboarding choices have no focus saved, so they keep the original look:
 * calorie ring and macros, plus the check button if they use conditions or
 * allergies.
 */
data class HomeLayout(
    /** Big "Can I eat this?" button at the top. */
    val showCheckHero: Boolean,
    /** Calorie ring and macro bars. */
    val showCalorieRing: Boolean,
    /** Share of calories used for the protein / carb bar targets. */
    val proteinShare: Double,
    val carbShare: Double,
    /** Plain verdict per food instead of numbers and condition tags. */
    val simple: Boolean
)
