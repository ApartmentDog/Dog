package com.evyr.rads.data

import com.evyr.rads.data.local.FoodLogEntry

/**
 * Common allergens the user can switch on in Settings, plus any custom ones
 * they type in free-text. Kept separate from Condition: an allergy is a flat
 * "this ingredient is present" flag, always OVER_LIMIT, not a caution tier
 * that scales with amount -- the two don't share a shape.
 *
 * matchWords are what's searched for in a food's name and ingredient text,
 * word-boundary, case-insensitive. Deliberately broad (e.g. PEANUT also
 * matches "peanut butter", "peanuts") since a missed match is the worse
 * failure for an allergy.
 */
enum class Allergen(val key: String, val label: String, val matchWords: List<String>) {
    PEANUTS("peanuts", "Peanuts", listOf("peanut")),
    TREE_NUTS(
        "tree_nuts", "Tree Nuts",
        listOf(
            "almond", "cashew", "walnut", "pecan", "pistachio", "hazelnut",
            "macadamia", "brazil nut", "pine nut"
        )
    ),
    SHELLFISH(
        "shellfish", "Shellfish",
        listOf("shrimp", "prawn", "crab", "lobster", "scallop", "clam", "mussel", "oyster", "crawfish")
    ),
    FISH("fish", "Fish", listOf("salmon", "tuna", "cod", "tilapia", "halibut", "anchovy", "sardine", "fish sauce")),
    EGGS("eggs", "Eggs", listOf("egg")),
    DAIRY("dairy", "Milk / Dairy", listOf("milk", "cheese", "butter", "cream", "yogurt", "whey", "casein")),
    WHEAT_GLUTEN(
        "wheat_gluten", "Wheat / Gluten",
        listOf("wheat", "gluten", "barley", "rye", "malt", "bread", "pasta", "flour tortilla")
    ),
    SOY("soy", "Soy", listOf("soy", "tofu", "edamame", "tempeh")),
    SESAME("sesame", "Sesame", listOf("sesame", "tahini"));

    companion object {
        fun fromKey(k: String): Allergen? =
            values().firstOrNull { it.key == k.trim().lowercase() }

        fun parse(csv: String?): Set<Allergen> =
            csv.orEmpty().split(',').mapNotNull { fromKey(it) }.toSet()

        fun toCsv(set: Set<Allergen>): String =
            values().filter { it in set }.joinToString(",") { it.key }
    }
}

/**
 * A user-typed allergen not in the common list ("mangoes", "MSG"). Matched
 * the same way as a common allergen: the word itself, as typed, against a
 * food's name and ingredient text.
 */
data class CustomAllergen(val label: String) {
    companion object {
        fun parseCsv(csv: String?): List<CustomAllergen> =
            csv.orEmpty().split(',').map { it.trim() }.filter { it.isNotBlank() }.map { CustomAllergen(it) }

        fun toCsv(list: List<CustomAllergen>): String =
            list.joinToString(",") { it.label.trim() }
    }
}

data class AllergyFlag(val label: String)

object AllergyChecker {

    private fun rx(word: String) = Regex("""\b${Regex.escape(word)}\b""", RegexOption.IGNORE_CASE)

    private fun matches(text: String, words: List<String>) =
        words.any { rx(it).containsMatchIn(text) }

    /** Every allergen (common + custom) present in the given text. */
    fun check(
        text: String,
        allergens: Set<Allergen>,
        customAllergens: List<CustomAllergen>
    ): List<AllergyFlag> {
        if (text.isBlank()) return emptyList()
        val hits = mutableListOf<AllergyFlag>()
        allergens.forEach { a -> if (matches(text, a.matchWords)) hits += AllergyFlag(a.label) }
        customAllergens.forEach { c -> if (matches(text, listOf(c.label))) hits += AllergyFlag(c.label) }
        return hits
    }

    /** Name + ingredients when present (a candidate before logging). */
    fun check(food: ScannedFood, allergens: Set<Allergen>, customAllergens: List<CustomAllergen>): List<AllergyFlag> =
        check(listOfNotNull(food.name, food.ingredients).joinToString(" "), allergens, customAllergens)

    /** Name only -- a saved entry doesn't keep ingredient text. */
    fun check(entry: FoodLogEntry, allergens: Set<Allergen>, customAllergens: List<CustomAllergen>): List<AllergyFlag> =
        check(entry.name, allergens, customAllergens)
}
